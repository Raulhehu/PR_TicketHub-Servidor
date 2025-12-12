package pr.tickethub.servidor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import org.json.JSONObject;
import pr.tickethub.servidor.bd.ConexionBD;

// esta clase maneja todo lo relacionado con el inicio de sesion de los tecnicos via http
public class LoginHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            
            // reviso el metodo, si es post intento loguear, si es options es para configurar cors
            if ("POST".equalsIgnoreCase(method)) {
                handleLogin(ex);
            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                // esta parte es vital para que el navegador no bloquee las peticiones si el cliente corre en otro puerto (como en desarrollo)
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(200, 0);
                ex.close();
            } else {
                send(ex, 405, "{\"ok\":false,\"msg\":\"metodo no permitido\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"ok\":false,\"msg\":\"error interno\"}");
        }
    }

    // aqui proceso la logica de autenticacion
    private void handleLogin(HttpExchange ex) throws Exception {
        // leo todo el cuerpo de la peticion y lo parseo como json
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject in = new JSONObject(body);

        String usuario = in.optString("usuario", "");
        String contrasena = in.optString("contrasena", "");

        // validacion basica para no ir a la bd por gusto si faltan datos
        if (usuario.isBlank() || contrasena.isBlank()) {
            send(ex, 400, "{\"ok\":false,\"msg\":\"usuario y contrasena requeridos\"}");
            return;
        }

        // busco al usuario en la bd, hago un join con la tabla tecnicos para 
        // obtener su nombre real y especialidad de una vez
        String sql = "select ul.id_usuario, ul.id_tecnico, t.nombre, t.especialidad " +
                     "from usuarios_login ul " +
                     "join tecnicos t on ul.id_tecnico = t.id_tecnico " +
                     "where ul.usuario = ? and ul.contrasena = ? and ul.activo = true";

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            // paso los parametros de forma segura para evitar inyeccion sql
            ps.setString(1, usuario);
            ps.setString(2, contrasena);

            ResultSet rs = ps.executeQuery();
            // si rs.next devuelve true es que si existe el usuario y la clave es correcta
            if (rs.next()) {
                JSONObject res = new JSONObject();
                res.put("ok", true);
                res.put("id_usuario", rs.getInt("id_usuario"));
                res.put("id_tecnico", rs.getInt("id_tecnico"));
                res.put("nombre", rs.getString("nombre"));
                res.put("especialidad", rs.getString("especialidad"));
                sendJson(ex, 200, res.toString());
            } else {
                // si no coincide nada devuelvo error de autorizacion 401
                send(ex, 401, "{\"ok\":false,\"msg\":\"usuario o contrasena incorrectos\"}");
            }
        }
    }

    // metodo auxiliar para mandar respuestas json con las cabeceras correctas
    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        send(ex, status, json);
    }

    // metodo generico para escribir la respuesta en el flujo de salida
    private void send(HttpExchange ex, int status, String data) throws IOException {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }
}