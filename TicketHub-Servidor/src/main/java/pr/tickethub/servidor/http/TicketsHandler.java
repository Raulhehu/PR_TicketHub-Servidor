package pr.tickethub.servidor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.json.*;
import pr.tickethub.servidor.bd.TicketDAO;
import pr.tickethub.servidor.bd.ConexionBD;
import pr.tickethub.servidor.bd.Seguridad;

// esta clase es el cerebro de mi api para los tickets, decide que hacer segun el metodo http que llegue
public class TicketsHandler implements HttpHandler {

    private final TicketDAO dao = new TicketDAO();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            
            // imprimo en consola para debugear que peticiones van llegando
            System.out.println(method + " " + path);

            // importantisimo: configuro cors para que el navegador acepte peticiones desde otro puerto o dominio
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            // si es options solo respondo ok y cierro, es el pre-flight check que hace el navegador antes de enviar datos
            if ("OPTIONS".equalsIgnoreCase(method)) {
                ex.sendResponseHeaders(200, 0);
                ex.close();
                return;
            }

            // aqui dirijo el trafico segun el metodo que me manden
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(ex);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(ex);
            } else if ("PUT".equalsIgnoreCase(method)) {
                handlePut(ex, path);
            } else {
                send(ex, 405, "{\"ok\":false,\"msg\":\"metodo no permitido\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // si algo falla feo, devuelvo error 500 y el mensaje de la excepcion para saber que paso
            send(ex, 500, "{\"ok\":false,\"msg\":\"error interno: " + e.getMessage() + "\"}");
        }
    }

// metodo para listar, aqui decido si devuelvo tickets o la lista de tecnicos
    private void handleGet(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath(); // saco la ruta
        JSONObject res = new JSONObject();
        res.put("ok", true);

        // si la url dice 'tecnicos', entonces voy al dao a pedir la lista de empleados
        if (path.contains("/tecnicos")) {
            List<Map<String,Object>> lista = dao.listarTecnicos();
            res.put("tecnicos", new JSONArray(lista));
        } else {
            // si no, asumo que quieren los tickets de siempre
            List<Map<String,Object>> lista = dao.listar();
            res.put("tickets", new JSONArray(lista));
        }
        
        sendJson(ex, 200, res.toString());
    }

  // este es el metodo principal para cuando me llega un POST, aqui decido si es para ticket o tecnico
    private void handlePost(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath();

        // primero reviso si la ruta trae 'tecnicos', si es asi, lo mando a la funcion de crear tecnico
        if (path.contains("/tecnicos")) {
            handlePostTecnico(ex);
            return; // corto aqui para que no siga ejecutando la logica de tickets
        }

        //upgrade AQUI EMPIEZA LA LOGICA PARA CREAR UN TICKET
        
        // leo lo que me manda el cliente en el body y lo paso a string
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject in = new JSONObject(body);

        String titulo = in.optString("titulo", "");
        String descripcion = in.optString("descripcion", "");

        // aqui tengo cuidado con los nulos de json vs java para el id_cliente
        Object cliObj = in.opt("id_cliente");
        Integer idCliente = null;
        if (cliObj != null && !JSONObject.NULL.equals(cliObj)) {
            idCliente = ((Number) cliObj).intValue();
        }

        // hago lo mismo para la categoria, solo lo convierto si trae algo valido
        Object catObj = in.opt("id_categoria");
        Integer idCategoria = null;
        if (catObj != null && !JSONObject.NULL.equals(catObj)) {
            idCategoria = ((Number) catObj).intValue();
        }

        // si no me mandan titulo, les regreso error porque es obligatorio
        if (titulo.isBlank()) {
            send(ex, 400, "{\"ok\":false,\"msg\":\"titulo requerido\"}");
            return;
        }

        // mando guardar a la base de datos y recupero el id que se genero
        int id = dao.crear(titulo, descripcion, idCliente, idCategoria);
        
        //Nuevo upgrade para poder hacer las notificaiones UDP
        try{
            pr.tickethub.servidor.udp.TicketHubNotificadorUDP udp =
                    new pr.tickethub.servidor.udp.TicketHubNotificadorUDP("localhost", 9091);
            udp.enviar("NUEVO_TICKET: " +id);
            System.out.println("Notificacion UDP enviada.");
        } catch (Exception e) {
            System.out.println("No se pudo enviar UDP (no es critico): " + e.getMessage());
        }
        
        // parche rapido: le pongo estado EN_ESPERA manual apenas se crea
        actualizarCampo(id, "estado", "EN_ESPERA");
        
        // armo la respuesta json con el id nuevo y digo que todo salio bien
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("id", id);
        sendJson(ex, 201, res.toString());
    }

    // UPGRADE aQUI CONTROLO LA CREACION DE TECNICOS
    private void handlePostTecnico(HttpExchange ex) throws Exception {
        // igual que arriba, leo el json que me llega
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject in = new JSONObject(body);

        String nombre = in.optString("nombre", "");
        String especialidad = in.optString("especialidad", "");
        String usuario = in.optString("usuario", "");
        String contrasenaPlana = in.optString("contrasena", "");

        // valido que no falten los datos importantes para el login
        if (nombre.isBlank() || usuario.isBlank() || contrasenaPlana.isBlank()) {
            send(ex, 400, "{\"ok\":false,\"msg\":\"Faltan datos obligatorios (nombre, usuario, pass)\"}");
            return;
        }
        
        // upgrade: antes de guardar, encripto la contraseña para que en la BD se vea como garabatos
        String passEncriptada = Seguridad.encriptar(contrasenaPlana);
        
        // aqui llamo a mi dao especial que usa transacciones para guardar tecnico y usuario juntos
        // upgrade: ahora mando la encriptada al DAO en vez de la original
        boolean exito = dao.crearTecnicoCompleto(nombre, especialidad, usuario, passEncriptada);
        

        if (exito) {
            sendJson(ex, 201, "{\"ok\":true, \"msg\":\"Tecnico creado exitosamente\"}");
        } else {
            send(ex, 500, "{\"ok\":false,\"msg\":\"Error al crear tecnico\"}");
        }
    }
    
    // metodo para actualizar tickets, ya sea estado o tecnico
private void handlePut(HttpExchange ex, String path) throws Exception {
        // necesito partir la url para sacar el id, ej: /tickets/2/estado
        String[] partes = path.split("/");
        if (partes.length < 3) {
            send(ex, 400, "{\"ok\":false}");
            return;
        }

        // el id del ticket deberia estar en la posicion 2 del array
        int idTicket = Integer.parseInt(partes[2]);

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject in = new JSONObject(body);

        // reviso la url para saber que tipo de actualizacion quieren hacer
        if (path.contains("/estado")) {
            // logica para cambiar el estado
            String nuevoEstado = in.optString("estado", "");
            if (nuevoEstado.isBlank()) {
                send(ex, 400, "{\"ok\":false,\"msg\":\"estado requerido\"}");
                return;
            }

            String sql = "update tickets set estado = ? where id = ?";
            try (Connection c = ConexionBD.obtener();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nuevoEstado);
                ps.setInt(2, idTicket);
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    sendJson(ex, 200, "{\"ok\":true}");
                } else {
                    send(ex, 404, "{\"ok\":false}");
                }
            }

        } else if (path.contains("/asignar")) {
            // logica para asignar un tecnico
            int idTecnico = in.getInt("id_tecnico");

            String sql = "update tickets set id_tecnico = ? where id = ?";
            try (Connection c = ConexionBD.obtener();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, idTecnico);
                ps.setInt(2, idTicket);
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    sendJson(ex, 200, "{\"ok\":true}");
                } else {
                    send(ex, 404, "{\"ok\":false}");
                }
            }

        } else if (path.contains("/prioridad")) { 
            //upgrade: Logica para cambiar la prioridad
            String nuevaPrioridad = in.optString("prioridad", "");
            
            if (nuevaPrioridad.isBlank()) {
                send(ex, 400, "{\"ok\":false,\"msg\":\"prioridad requerida\"}");
                return;
            }

            String sql = "update tickets set prioridad = ? where id = ?";
            try (Connection c = ConexionBD.obtener();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nuevaPrioridad);
                ps.setInt(2, idTicket);
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    sendJson(ex, 200, "{\"ok\":true}");
                } else {
                    send(ex, 404, "{\"ok\":false}");
                }
            }

        } else {
            send(ex, 400, "{\"ok\":false,\"msg\":\"accion desconocida\"}");
        }
    }
    
// Metodo helper para no repetir tanto codigo SQL
    private void actualizarCampo(int id, String columna, String valor) throws Exception {
        /*upgrade: validar columna para evitar inyeccion SQL, aqui solo permito estado o prioridad*/ 
        if (!columna.equals("estado") && !columna.equals("prioridad")) return;

        String sql = "update tickets set " + columna + " = ? where id = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /*upgrade*/
    private void actualizarCampoInt(int id, String columna, int valor) throws Exception {
        if (!columna.equals("id_tecnico")) return;

        String sql = "update tickets set " + columna + " = ? where id = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, valor);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // helpers para facilitar el envio de respuestas
    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        send(ex, status, json);
    }

    private void send(HttpExchange ex, int status, String data) throws IOException {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }
}
