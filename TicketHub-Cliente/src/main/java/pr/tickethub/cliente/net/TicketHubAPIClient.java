package pr.tickethub.cliente.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

// esta clase es mi cliente para conectarme a la API del servidor y consumir los servicios
public class TicketHubAPIClient {

    // defino la url base donde esta corriendo mi servidor local
    private static final String BASE_URL = "http://localhost:8081";

    // metodo para pedir la lista de tickets al backend
    public static List<Map<String,Object>> listarTickets() throws Exception {
        URL url = new URL(BASE_URL + "/tickets");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        
        // configuro la conexion para hacer una peticion tipo GET y esperar un JSON
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        // verifico si el servidor respondio con exito (codigo 200) si no lanzo error
        int status = con.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("Error HTTP: " + status);
        }

        // aqui leo todo el flujo de datos que me manda el servidor y lo reconstruyo en un string
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = in.readLine()) != null) {
                sb.append(linea);
            }
            
            // convierto la respuesta de texto plano a un objeto JSON para poder leerlo
            JSONObject obj = new JSONObject(sb.toString());
            // extraigo el arreglo especifico que contiene los tickets
            JSONArray arr = obj.getJSONArray("tickets");

            List<Map<String,Object>> lista = new ArrayList<>();
            
            // recorro el arreglo JSON, ticket por ticket para pasarlos a una lista de mapas de java
            // hago esto para facilitar el manejo de datos en la interfaz grafica mas adelante
            for (int i = 0; i < arr.length(); i++) {
                JSONObject t = arr.getJSONObject(i);
                Map<String,Object> m = new LinkedHashMap<>();
                // extraigo cada campo del JSON y lo guardo en mi mapa
                m.put("id", t.getInt("id"));
                m.put("titulo", t.getString("titulo"));
                m.put("estado", t.getString("estado"));
                m.put("prioridad", t.getString("prioridad"));
                m.put("creado_en", t.getString("creado_en"));
                lista.add(m);
            }
            return lista;
        }
    }
}