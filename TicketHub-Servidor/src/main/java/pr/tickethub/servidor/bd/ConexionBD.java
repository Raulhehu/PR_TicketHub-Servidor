package pr.tickethub.servidor.bd;

import java.sql.*;

// esta clase la uso exclusivamente para manejar la conexion con postgresql
public class ConexionBD {
    
    // aqui defino las credenciales y la url de mi base de datos local
    private static final String URL = "jdbc:postgresql://localhost:5432/tickethub";
    private static final String USUARIO = "postgres";
    private static final String CONTRASENA = "1234";

    // este metodo es el que llamo desde otras clases cada vez que necesito abrir una conexion nueva
    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
    }

    // funcion auxiliar rapida para validar que la base de datos este arriba y respondiendo
    public static void probar() {
        // uso try con recursos para que la conexion se cierre sola al terminar la prueba
        try (Connection c = obtener(); Statement st = c.createStatement()) {
            // ejecuto una consulta minima para ver si no hay errores de red o autenticacion
            st.execute("select 1");
            System.out.println("OK PostgreSQL TicketHub");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}