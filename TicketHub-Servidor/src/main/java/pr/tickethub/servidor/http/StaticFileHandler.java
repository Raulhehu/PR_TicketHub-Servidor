package pr.tickethub.servidor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// esta clase se encarga de servir los archivos del frontend como html css y js
public class StaticFileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        
        // si piden la raiz del servidor o nada, les doy el index.html por defecto
        if (path.equals("/") || path.isEmpty()) {
            path = "index.html";
        } else {
            path = path.substring(1); // le quito la barra inicial para tener solo el nombre
        }

        // esto es importante, defino varias rutas posibles donde podria estar la carpeta web
        // hago esto porque dependiendo si corro desde netbeans o la terminal la ruta relativa cambia
        String[] posiblesPaths = {
            "web/" + path,
            "../web/" + path,
            "../../web/" + path,
            "TicketHub-Servidor/web/" + path
        };

        // pruebo ruta por ruta hasta encontrar el archivo real
        for (String filePath : posiblesPaths) {
            try {
                Path file = Paths.get(filePath);
                // si el archivo existe en esta ruta, lo proceso y lo envio
                if (Files.exists(file)) {
                    byte[] bytes = Files.readAllBytes(file);
                    
                    // asigno el tipo mime correcto segun la extension para que el navegador entienda que recibe
                    String contentType = "text/html; charset=utf-8";
                    if (path.endsWith(".css")) contentType = "text/css; charset=utf-8";
                    if (path.endsWith(".js")) contentType = "application/javascript; charset=utf-8";
                    if (path.endsWith(".json")) contentType = "application/json; charset=utf-8";

                    ex.getResponseHeaders().add("Content-Type", contentType);
                    ex.sendResponseHeaders(200, bytes.length);
                    ex.getResponseBody().write(bytes);
                    ex.close();
                    
                    System.out.println("!! Encontrado y servido: " + filePath);
                    // si ya lo encontre y envie, termino aqui para no seguir buscando
                    return;
                }
            } catch (Exception e) {
                // si falla leer en esta ruta, no hago nada y dejo que el ciclo pruebe la siguiente
            }
        }

        // si termine el ciclo y no encontre el archivo en ninguna parte, devuelvo 404
        System.out.println("X Archivo NO encontrado: " + path);
        sendError(ex, 404, "404 - No encontrado: " + path);
    }

    // metodo auxiliar simple para mandar errores al cliente
    private void sendError(HttpExchange ex, int status, String msg) throws IOException {
        byte[] bytes = msg.getBytes("UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }
}