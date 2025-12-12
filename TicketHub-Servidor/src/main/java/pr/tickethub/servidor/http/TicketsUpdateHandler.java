package pr.tickethub.servidor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import org.json.JSONObject;
import pr.tickethub.servidor.bd.ConexionBD;

// esta clase maneja las actualizaciones especificas de los tickets, por ahora solo el cambio de estado
public class TicketsUpdateHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            
            // verifico si la url contiene /estado para saber que logica aplicar
            if (path.contains("/estado")) {
                handleEstado(ex, path);
            } else {
                send(ex, 404, "{\"ok\":false}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"ok\":false}");
        }
    }

    private void handleEstado(HttpExchange ex, String path) throws Exception {
        // aqui hago un truco rapido, parto la url por diagonales para sacar el id del ticket que viene en la posicion 2
        String[] partes = path.split("/");
        int id = Integer.parseInt(partes[2]);

        // leo el cuerpo de la peticion y extraigo el nuevo estado del json
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject in = new JSONObject(body);
        String estado = in.getString("estado");

        String sql = "update tickets set estado = ? where id = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, estado);
            ps.setInt(2, id);
            
            // ejecuto la actualizacion y checo si afecto alguna fila para confirmar el exito
            int updated = ps.executeUpdate();

            if (updated > 0) {
                send(ex, 200, "{\"ok\":true}");
            } else {
                send(ex, 404, "{\"ok\":false,\"msg\":\"ticket no encontrado\"}");
            }
        }
    }

    // metodo helper sencillo para escribir la respuesta y cerrar la conexion
    private void send(HttpExchange ex, int status, String data) throws IOException {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }
}