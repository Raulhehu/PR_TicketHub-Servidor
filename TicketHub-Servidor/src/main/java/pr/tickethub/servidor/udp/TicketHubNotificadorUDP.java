package pr.tickethub.servidor.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

// esta clase auxiliar me sirve para enviar mensajes rapidos por udp, util para notificaciones que no requieren conexion fija
public class TicketHubNotificadorUDP {

    private final String host;
    private final int puerto;

    // constructor para definir desde el inicio a quien le voy a mandar los mensajes
    public TicketHubNotificadorUDP(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
    }

    // este metodo hace todo el trabajo sucio: convierte el texto a paquete y lo dispara
    public void enviar(String mensaje) {
        // uso try con recursos para crear el socket y que se cierre solo al terminar, asi no dejo puertos abiertos
        try (DatagramSocket socket = new DatagramSocket()) {
            // paso el string a bytes usando utf-8 para no tener problemas con acentos o caracteres raros
            byte[] datos = mensaje.getBytes("UTF-8");
            InetAddress addr = InetAddress.getByName(host);
            
            // armo el datagrama con los datos, su longitud, la ip y el puerto destino
            DatagramPacket packet =
                new DatagramPacket(datos, datos.length, addr, puerto);
            
            // envio el paquete y listo, como es udp no me espero a ver si llego o no
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}