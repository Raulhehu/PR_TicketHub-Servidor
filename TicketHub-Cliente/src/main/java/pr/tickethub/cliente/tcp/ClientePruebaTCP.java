package pr.tickethub.cliente.tcp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// clase sencilla para probar la conexion tcp de forma manual desde la consola
public class ClientePruebaTCP {

    public static void main(String[] args) {
        // defino la direccion y el puerto donde escucha mi servidor
        String host = "localhost";
        int puerto = 9090;

        // abro el socket y configuro los streams de entrada y salida con UTF-8 para evitar problemas de caracteres raros, tambien uso try con recursos para que se cierren solos
        try (Socket socket = new Socket(host, puerto);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
             Scanner sc = new Scanner(System.in)) {

            // leo y muestro el mensaje de bienvenida que manda el servidor al conectarme
            System.out.println(in.readLine());

            // ciclo infinito para poder enviar multiples comandos sin cerrar el programa
            while (true) {
                System.out.print("> ");
                // leo el comando que escribo en la consola y se lo mando al servidor
                String comando = sc.nextLine();
                out.println(comando);

                // si escribo QUIT rompo el ciclo para cerrar la conexion limpiamente
                if ("QUIT".equalsIgnoreCase(comando)) {
                    break;
                }

                String linea;
                // leo la respuesta del servidor linea por linea
                while ((linea = in.readLine()) != null) {
                    System.out.println(linea);
                    // esta parte es importante, verifico si la linea es un marcador de fin de mensaje 
                    //como END, OK o ERR para dejar de leer, si no el cliente se quedaria esperando eternamente.
                    if ("END".equals(linea) || linea.startsWith("OK") || linea.startsWith("ERR") || "BYE".equals(linea)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}