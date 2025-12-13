package pr.tickethub.servidor;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import pr.tickethub.servidor.http.TicketsHandler;
import pr.tickethub.servidor.http.StaticFileHandler;
import pr.tickethub.servidor.http.LoginHandler;

// esta es la clase principal que arranca todo mi servidor http aunque
// (comentar mas adenlante lo que hare con el http) 
public class MainServidor {

    public static void main(String[] args) throws Exception {
        // defino el puerto donde va a escuchar mi servidor
        int puerto = 8081;
        
        // creo la instancia del servidor y le digo que escuche en todas las interfaces de red (0.0.0.0)
        // esto es util por si quiero conectarme desde otra compu en la misma red y no solo localhost 
        // o para que alguien mas pueda conectarse tambien, siempre y cuando estemos en la misma red
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", puerto), 0);

        // aqui mapeo las rutas url a mis clases manejadoras
        // es decir, le digo al servidor que codigo ejecutar cuando alguien entra a /login o /tickets
        server.createContext("/login", new LoginHandler());
        server.createContext("/tickets", new TicketsHandler());
        
        /*upgrade: para el tema de los tecnicos*/
        server.createContext("/tecnicos", new TicketsHandler());
        // este ultimo maneja los archivos estaticos como html o css en la raiz
        server.createContext("/", new StaticFileHandler());

        // configuro un pool de hilos para que el servidor pueda atender hasta 20 peticiones simultaneas
        // asi si un cliente tarda mucho no bloquea a los demas
        server.setExecutor(Executors.newFixedThreadPool(20));
        server.start();
        
        // imprimo informacion util en la consola para saber que todo arranco bien y mostrar las credenciales de prueba
        System.out.println("========================================");
        System.out.println("TicketHub servidor iniciado");
        System.out.println("URL: http://localhost:8081");
        System.out.println("Usuarios de prueba ya");
        System.out.println("registrados previamente"); 
        System.out.println("juan, maria, carlos (pass: 1234)");
        System.out.println("========================================");
    }
}
