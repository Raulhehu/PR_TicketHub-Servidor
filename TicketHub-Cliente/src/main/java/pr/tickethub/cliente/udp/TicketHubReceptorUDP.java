package pr.tickethub.cliente.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

// esta clase se encarga de escuchar las notificaciones que manda el servidor por udp, sirve para alertas rapidas
public class TicketHubReceptorUDP {

    // defino el puerto donde voy a estar escuchando las notificaciones
    private static final int PUERTO = 9091;

    public static void main(String[] args) {
        System.out.println("Receptor UDP TicketHub escuchando en puerto " + PUERTO);
        // preparo un arreglo de bytes para recibir los datos que lleguen
        byte[] buffer = new byte[1024];

        // abro el socket udp en el puerto especificado, uso try para asegurar que se cierre si pasa algo raro
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            // ciclo infinito para quedarme escuchando siempre y no cerrarme despues del primer mensaje
            while (true) {
                // preparo el paquete vacio donde se guardara la info que llegue
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                // aqui el programa se detiene y espera hasta que llegue un paquete, es una llamada bloqueante
                socket.receive(packet);
                
                // convierto los bytes recibidos a texto legible usando utf-8, solo tomo la longitud real de lo que llego
                String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                System.out.println("NOTIF: " + msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}