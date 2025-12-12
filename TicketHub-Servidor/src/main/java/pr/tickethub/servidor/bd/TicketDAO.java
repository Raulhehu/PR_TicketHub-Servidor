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
            
            // esto es importante, verifico si el id del cliente viene nulo
            // si es null tengo que usar setNull especificamente, sino java muere al intentar meter null en un entero
            if (idCliente == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, idCliente);
            }
            
            // hago lo mismo para la categoria, por si el ticket no tiene categoria asignada aun
            if (idCategoria == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, idCategoria);
            }

            // ejecuto como query porque espero que me regrese el id
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // metodo para sacar toda la lista de tickets para mostrarla en la interfaz
    public List<Map<String,Object>> listar() throws SQLException {
        // los ordeno descendente para ver primero los mas recientes
        String sql = "select t.id, t.titulo, t.estado, t.prioridad, t.creado_en " +
                     "from tickets t order by t.id desc";
        
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
                // convierto la fecha a string directo para no batallar con formatos luego
                m.put("creado_en", rs.getTimestamp("creado_en").toString());
                out.add(m);
            }
            return out;
        }
    }
}