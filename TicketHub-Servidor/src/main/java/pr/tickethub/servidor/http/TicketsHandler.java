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

    // metodo para listar los tickets, es sencillo: saca del dao y manda json
    private void handleGet(HttpExchange ex) throws Exception {
        List<Map<String,Object>> lista = dao.listar();
        JSONArray arr = new JSONArray(lista);
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("tickets", arr);
        sendJson(ex, 200, res.toString());
    }

    // metodo para crear un ticket nuevo
    private void handlePost(HttpExchange ex) throws Exception {
        // leo todo el json que me manda el cliente
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject in = new JSONObject(body);

        String titulo = in.optString("titulo", "");
        String descripcion = in.optString("descripcion", "");

        // esta parte es delicada: verifico si mandan id_cliente y si no es nulo
        // json tiene su propio tipo de null (JSONObject.NULL) que es diferente al null de java
        // asi que valido ambos casos para evitar errores al convertir a entero
        Object cliObj = in.opt("id_cliente");
        Integer idCliente = null;
        if (cliObj != null && !JSONObject.NULL.equals(cliObj)) {
            idCliente = ((Number) cliObj).intValue();
        }

        // hago lo mismo para la categoria, extrayendo el valor solo si es valido
        Object catObj = in.opt("id_categoria");
        Integer idCategoria = null;
        if (catObj != null && !JSONObject.NULL.equals(catObj)) {
            idCategoria = ((Number) catObj).intValue();
        }

        // validacion minima, sin titulo no creo nada
        if (titulo.isBlank()) {
            send(ex, 400, "{\"ok\":false,\"msg\":\"titulo requerido\"}");
            return;
        }
        /*upgrade*/
        // guardo en base de datos y retorno el id generado
        int id = dao.crear(titulo, descripcion, idCliente, idCategoria);
        
        // Parche: Actualizar a EN_ESPERA justo despues de crear
        // (O idealmente modificar el DAO para aceptar estado, 
        // pero esto funciona sin tocar el DAO de momento)
        actualizarCampo(id, "estado", "EN_ESPERA");
        
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("id", id);
        sendJson(ex, 201, res.toString());
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
