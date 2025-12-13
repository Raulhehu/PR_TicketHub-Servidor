package pr.tickethub.servidor.bd;

import java.sql.*;
import java.util.*;

// esta clase es la que se encarga de hablar directo con la base de datos para todo lo de los tickets
public class TicketDAO {

    // metodo para guardar un ticket nuevo en la bd
    public int crear(String titulo, String descripcion, Integer idCliente, Integer idCategoria) throws SQLException {
        // uso returning id, que es una funcion de postgres para que me devuelva el id generado en la misma consulta
        String sql = "insert into tickets(titulo, descripcion, id_cliente, id_categoria) " +
                     "values(?,?,?,?) returning id";
        
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, titulo);
            ps.setString(2, descripcion);
            /*upgrade: para el Tomar ticket*/
            ps.setObject(3, idCliente);
            ps.setObject(4, idCategoria);
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    // metodo para sacar toda la lista de tickets para mostrarla en la interfaz
    public List<Map<String,Object>> listar() throws SQLException {
        /*upgrade*/
        List<Map<String, Object>> lista = new ArrayList<>();
        // los ordeno descendente para ver primero los mas recientes
        String sql = "SELECT id, titulo, estado, prioridad, id_tecnico, creado_en FROM tickets ORDER BY id DESC";
        
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Map<String,Object>> out = new ArrayList<>();
            
            // recorro cada fila que me trajo la consulta
            while (rs.next()) {
                // uso un mapa para guardar los datos de la fila porque es mas facil pasarlo a json despues
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("titulo", rs.getString("titulo"));
                m.put("estado", rs.getString("estado"));
                m.put("prioridad", rs.getString("prioridad"));
                /*upgrade:*/
                m.put("id_tecnico", rs.getObject("id_tecnico"));
                // convierto la fecha a string directo para no batallar con formatos luego
                m.put("creado_en", rs.getTimestamp("creado_en").toString());
                out.add(m);
            }
            return out;
        }
    }
    
    //Metodo para obtener la lista de tecnicos (para el dropdown)
    public List<Map<String, Object>> listarTecnicos() throws Exception {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_tecnico, nombre FROM tecnicos WHERE activo = true";
        try (Connection c = ConexionBD.obtener();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id_tecnico"));
                m.put("nombre", rs.getString("nombre"));
                lista.add(m);
            }
        }
        return lista;
    }

    /*upgrade importante: se agrego este metodo para poder registrar nuevos empleados 
        puesto que esta aplicacion es solo y unicamente para los tecnicos
        no elabore un sign up y unicamente el login para los que estan trabajando*/
    //metodo para CREAR un tecnico nuevo y su usuario de login al mismo tiempo
    public boolean crearTecnicoCompleto(String nombre, String especialidad, String usuario, String pass) throws Exception {
        Connection c = null;
        try {
            c = ConexionBD.obtener();
            c.setAutoCommit(false); // Iniciamos transaccion manual

            // Insertamos en la tabla tecnicos
            String sqlTec = "INSERT INTO tecnicos (nombre, especialidad) VALUES (?, ?)";
            int idTecnico = -1;
            try (PreparedStatement ps = c.prepareStatement(sqlTec, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.setString(2, especialidad);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) idTecnico = rs.getInt(1);
                }
            }

            //Insertar en tabla usuarios_login
            String sqlUser = "INSERT INTO usuarios_login (id_tecnico, usuario, contrasena) VALUES (?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sqlUser)) {
                ps.setInt(1, idTecnico);
                ps.setString(2, usuario);
                ps.setString(3, pass);
                ps.executeUpdate();
            }

            c.commit(); // Todo salio bien, guardamos cambios
            return true;
        } catch (Exception e) {
            if (c != null) c.rollback(); // Si algo falla, deshacemos todo
            e.printStackTrace();
            return false;
        } finally {
            if (c != null) { c.setAutoCommit(true); c.close(); }
        }
    }
    
}
