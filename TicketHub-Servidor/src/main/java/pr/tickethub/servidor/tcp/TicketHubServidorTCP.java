package pr.tickethub.servidor.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import pr.tickethub.servidor.bd.ConexionBD;
import pr.tickethub.servidor.bd.TicketDAO;
import pr.tickethub.servidor.udp.TicketHubNotificadorUDP;

// esta clase maneja el servidor tcp concurrente, aqui recibo las conexiones persistentes
public class TicketHubServidorTCP {

    private static final int PUERTO = 9090;

    private ServerSocket serverSocket;
    // uso un pool de hilos para no crear uno nuevo por cada cliente y saturar la memoria
    private ExecutorService pool;
    private TicketDAO ticketDAO = new TicketDAO();

    // instancio mi notificador udp apuntando al puerto local 9091
    private TicketHubNotificadorUDP notificador =
            new TicketHubNotificadorUDP("localhost", 9091);

    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(PUERTO);
        // limito a 20 conexiones simultaneas
        pool = Executors.newFixedThreadPool(20);
        System.out.println("TicketHub TCP escuchando en puerto " + PUERTO);

        // ciclo infinito para aceptar clientes todo el tiempo
        while (true) {
            Socket cliente = serverSocket.accept();
            // cada vez que llega alguien, le paso el trabajo al pool de hilos con el dao y notificador
            pool.submit(new ManejadorCliente(cliente, ticketDAO, notificador));
        }
    }

    // clase interna que define la logica de atencion para cada cliente conectado
    private static class ManejadorCliente implements Runnable {

        private final Socket socket;
        private final TicketDAO dao;
        private final TicketHubNotificadorUDP notificador;

        ManejadorCliente(Socket socket, TicketDAO dao, TicketHubNotificadorUDP notificador) {
            this.socket = socket;
            this.dao = dao;
            this.notificador = notificador;
        }

        @Override
        public void run() {
            String remoto = socket.getRemoteSocketAddress().toString();
            System.out.println("Cliente TCP conectado: " + remoto);
            
            // configuro streams con utf-8 para caracteres especiales
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
                 PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

                out.println("OK TicketHub TCP listo");

                String linea;
                // leo comando por comando hasta que el cliente se desconecte o mande quit
                while ((linea = in.readLine()) != null) {
                    linea = linea.trim();
                    // protocolo simple de texto: detecto palabras clave
                    if (linea.equalsIgnoreCase("QUIT")) {
                        out.println("BYE");
                        break;
                    } else if (linea.equalsIgnoreCase("LIST")) {
                        manejarList(out);
                    } else if (linea.startsWith("SET_ESTADO")) {
                        manejarSetEstado(linea, out);
                    } else {
                        out.println("ERR comando no reconocido");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // aseguro cerrar el socket pase lo que pase para no dejar conexiones colgadas
                try { socket.close(); } catch (IOException ignored) {}
                System.out.println("Cliente TCP desconectado: " + remoto);
            }
        }

        private void manejarList(PrintWriter out) {
            try {
                // obtengo la lista y la envio linea por linea
                List<Map<String,Object>> tickets = dao.listar();
                out.println("TICKETS " + tickets.size());
                for (Map<String,Object> t : tickets) {
                    out.println("#" + t.get("id") + " [" + t.get("estado") + "] " + t.get("titulo"));
                }
                // marcador de fin para que el cliente sepa cuando dejar de leer
                out.println("END");
            } catch (Exception e) {
                e.printStackTrace();
                out.println("ERR al listar tickets");
            }
        }

        private void manejarSetEstado(String linea, PrintWriter out) {
            try {
                // el comando viene como: SET_ESTADO {json} asi que busco donde empieza el json
                int idx = linea.indexOf('{');
                if (idx < 0) {
                    out.println("ERR formato invalido");
                    return;
                }
                // extraigo y parseo el json para sacar los datos
                String jsonStr = linea.substring(idx);
                JSONObject obj = new JSONObject(jsonStr);
                int id = obj.getInt("id");
                String estado = obj.getString("estado");

                actualizarEstado(id, estado);

                // importante: aqui disparo la notificacion udp para avisar a otros sistemas en tiempo real
                notificador.enviar("Ticket " + id + " cambio a " + estado);

                out.println("OK estado actualizado");
            } catch (Exception e) {
                e.printStackTrace();
                out.println("ERR al actualizar estado");
            }
        }

        private void actualizarEstado(int id, String estado) throws Exception {
            String sql = "update tickets set estado = ? where id = ?";
            try (var c = ConexionBD.obtener();
                 var ps = c.prepareStatement(sql)) {
                ps.setString(1, estado);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new TicketHubServidorTCP().iniciar();
    }
}