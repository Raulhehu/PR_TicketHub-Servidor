

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class TicketHubReceptorUDP {

    private static final int PUERTO = 9091;

    public static void main(String[] args) {
        System.out.println("Receptor UDP TicketHub escuchando en puerto " + PUERTO);
        byte[] buffer = new byte[1024];

        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                System.out.println("NOTIF: " + msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
