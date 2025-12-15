package pr.tickethub.cliente.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.json.JSONObject;
import pr.tickethub.cliente.net.TicketHubAPIClient;

// esta es la clase principal donde armo toda la interfaz grafica usando swing
public class VentanaPrincipal extends JFrame {

    // defino la url base de mi api para usarla en todo el codigo y no repetirla
    private static final String BASE_URL = "http://localhost:8081";
    
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;
    private JButton btnEnProceso, btnCerrado, btnNuevo, btnRefrescar;

    // defino mi paleta de colores aqui para usarlos facil en todos los componentes y mantener el estilo
    private static final Color COLOR_HEADER = new Color(31, 33, 33);     // gris oscuro
    private static final Color COLOR_ACCENT = new Color(32, 128, 144);    // teal
    private static final Color COLOR_BG = new Color(252, 252, 249);       // crema
    private static final Color COLOR_TEXT = new Color(19, 52, 59);        // azul oscuro
    private static final Color COLOR_BUTTON = new Color(33, 128, 141);    // teal

    public VentanaPrincipal() {
        // configuro las propiedades basicas de la ventana como titulo, tamaño y cierre
        setTitle("TicketHub - Sistema de Soporte Tecnico");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(COLOR_BG);

        // mando a llamar mis metodos para construir cada parte de la interfaz por separado
        JPanel panelHeader = crearHeader();
        JPanel panelLateral = crearPanelLateral();
        JPanel panelCentral = crearPanelCentral();

        // agrego todo al layout principal
        add(panelHeader, BorderLayout.NORTH);
        add(panelLateral, BorderLayout.WEST);
        add(panelCentral, BorderLayout.CENTER);
    
        iniciarEscuchaUDP();

    }

    // este metodo crea la barra superior con el titulo y el contador
    private JPanel crearHeader() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_HEADER);
        panel.setPreferredSize(new Dimension(0, 80));
        panel.setLayout(new BorderLayout(20, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titulo = new JLabel("TicketHub");
        titulo.setFont(new Font("Arial", Font.BOLD, 32));
        titulo.setForeground(COLOR_ACCENT);

        JLabel subtitulo = new JLabel("Sistema de Gestion de Tickets de Soporte Tecnico");
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitulo.setForeground(new Color(200, 200, 200));

        JPanel panelTexto = new JPanel(new BorderLayout());
        panelTexto.setBackground(COLOR_HEADER);
        panelTexto.add(titulo, BorderLayout.NORTH);
        panelTexto.add(subtitulo, BorderLayout.SOUTH);

        panel.add(panelTexto, BorderLayout.WEST);

        lblTotal = new JLabel("Cargando...");
        lblTotal.setFont(new Font("Arial", Font.PLAIN, 14));
        lblTotal.setForeground(COLOR_ACCENT);
        panel.add(lblTotal, BorderLayout.EAST);

        return panel;
    }

    // aqui armo el menu lateral con todos los botones de accion
    private JPanel crearPanelLateral() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 245, 245));
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayout(5, 1, 0, 8));
        panelBotones.setBackground(new Color(245, 245, 245));

        // configuro las acciones de cada boton usando expresiones lambda para ahorrar codigo
        btnNuevo = crearBoton("+ Nuevo Ticket", new Color(76, 175, 80));
        btnNuevo.addActionListener(e -> mostrarDialogoNuevoTicket());

        btnRefrescar = crearBoton("Refrescar", COLOR_BUTTON);
        btnRefrescar.addActionListener(e -> cargarTickets());

        btnEnProceso = crearBoton("En Proceso", new Color(255, 193, 7));
        btnEnProceso.addActionListener(e -> cambiarEstadoREST ("EN_PROCESO"));

        btnCerrado = crearBoton("Cerrar Ticket", new Color(244, 67, 54));
        btnCerrado.addActionListener(e -> cambiarEstadoREST("CERRADO"));

        JButton btnSalir = crearBoton("Salir", new Color(158, 158, 158));
        btnSalir.addActionListener(e -> System.exit(0));

        panelBotones.add(btnNuevo);
        panelBotones.add(btnRefrescar);
        panelBotones.add(btnEnProceso);
        panelBotones.add(btnCerrado);
        panelBotones.add(btnSalir);
        panel.add(new JSeparator()); // Separador visual

        panel.add(panelBotones, BorderLayout.NORTH);
        return panel;
    }

    // metodo auxiliar para dar estilo uniforme a los botones
    private JButton crearBoton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // configuro la tabla central donde se muestran los datos
    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnas = { "ID", "Titulo", "Estado", "Prioridad", "Tecnico" };
        // sobrescribo el modelo para evitar que el usuario edite las celdas directamente
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setFont(new Font("Arial", Font.PLAIN, 11));
        tabla.setRowHeight(25);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setGridColor(new Color(200, 200, 200));

        // Colores de encabezado
        tabla.getTableHeader().setBackground(COLOR_HEADER);
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBackground(COLOR_BG);

        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    // este metodo es clave, uso SwingWorker para cargar los datos en segundo plano y no congelar la interfaz
    private void cargarTickets() {
        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override
            protected List<Map<String,Object>> doInBackground() throws Exception {
                // llamo a mi cliente api que hice antes para obtener la lista
                return TicketHubAPIClient.listarTickets();
            }

            @Override
            protected void done() {
                try {
                    // cuando termina el hilo secundario, actualizo la interfaz grafica en el hilo principal
                    List<Map<String,Object>> data = get();
                    modelo.setRowCount(0); // limpio la tabla
                    for (Map<String,Object> t : data) {
                        // upgrate: manejo seguro de nulos para el tecnico
                        Object idTec = t.get("id_tecnico");
                        String tecnicoStr = (idTec == null || idTec.toString().equals("0")) ? "--libre--" : "Tec. #" + idTec;
                        modelo.addRow(new Object[] {
                            t.get("id"),
                            t.get("titulo"),
                            t.get("estado"),
                            t.get("prioridad"),
                            tecnicoStr
                        });
                    }
                    lblTotal.setText(data.size() + " tickets");
                } catch (Exception e) {
                    lblTotal.setText("Error de conexión");
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Error al cargar tickets: " + e.getMessage());
                }
            }
        }.execute();
    }

    // valido que haya algo seleccionado antes de intentar cambiar el estado
    private void cambiarEstadoREST(String nuevoEstado) {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor selecciona un ticket primero");
            return;
        }

        int idTicket = (int) modelo.getValueAt(fila, 0);
        
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                URL url = new URL(BASE_URL + "/tickets/" + idTicket + "/estado"); // Endpoint correcto
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("PUT");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("estado", nuevoEstado);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }

                return con.getResponseCode() == 200;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        cargarTickets(); // Refrescar tabla si tuvo éxito
                        JOptionPane.showMessageDialog(VentanaPrincipal.this, "Estado actualizado a " + nuevoEstado);
                    } else {
                        JOptionPane.showMessageDialog(VentanaPrincipal.this, "Error al actualizar estado.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    // muestro un dialogo modal sencillo para capturar los datos del nuevo ticket
    private void mostrarDialogoNuevoTicket() {
        JDialog dialogo = new JDialog(this, "Nuevo Ticket", true);
        dialogo.setSize(400, 250);
        dialogo.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_BG);

        JLabel lblTitulo = new JLabel("Titulo:");
        JTextField txtTitulo = new JTextField(20);

        JLabel lblDescripcion = new JLabel("Descripcion:");
        JTextArea txtDescripcion = new JTextArea(3, 20);
        txtDescripcion.setLineWrap(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);

        JButton btnCrear = new JButton("Crear");
        btnCrear.setBackground(COLOR_ACCENT);
        btnCrear.setForeground(Color.WHITE);
        btnCrear.addActionListener(e -> {
            String titulo = txtTitulo.getText().trim();
            String descripcion = txtDescripcion.getText().trim();

            if (titulo.isEmpty()) {
                JOptionPane.showMessageDialog(dialogo,
                        "El titulo es requerido",
                        "Validacion",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            crearTicketViaREST(titulo, descripcion);
            dialogo.dispose();
        });

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialogo.dispose());

        panel.add(lblTitulo);
        panel.add(txtTitulo);
        panel.add(lblDescripcion);
        panel.add(scrollDesc);
        panel.add(btnCrear);
        panel.add(btnCancelar);

        dialogo.add(panel);
        dialogo.setVisible(true);
    }

// aqui implemento la creacion via REST manualmente con HttpURLConnection 
//upgrade lo tratamos de hacer muy parecido al de la web
private void crearTicketViaREST(String titulo, String descripcion) {
    new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                URL url = new URL(BASE_URL + "/tickets");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                // configuro para hacer un POST y aviso que mando JSON
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setDoOutput(true);

                // construyo el objeto JSON usando la libreria para no batallar con las comillas
                JSONObject json = new JSONObject();
                json.put("titulo", titulo);
                json.put("descripcion", descripcion);
                json.put("id_cliente", JSONObject.NULL);
                json.put("id_categoria", JSONObject.NULL);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }

                return con.getResponseCode() == 201;
            }

        @Override
        protected void done() {
            try {
                if (get()) {
                    //upgrade: ya no verifico si el id 
                    cargarTickets(); // actualizo la lista automaticamente
                    JOptionPane.showMessageDialog(VentanaPrincipal.this, "Ticket creado correctamente.");
                } else {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Error al crear el ticket",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(VentanaPrincipal.this,
                        "Error: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}

// Este método inicia el "oído" del cliente en un hilo separado
private void iniciarEscuchaUDP() {
    new Thread(() -> {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket(9091)) {
            byte[] buffer = new byte[1024];
            System.out.println("Cliente escuchando UDP en 9091...");
            
            while (true) {
                java.net.DatagramPacket packet = new java.net.DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                
                // Si recibimos el aviso del servidor...
                if (msg.startsWith("NUEVO_TICKET")) {
                    // Actualizamos la interfaz (SwingUtilities es vital aquí para no romper la UI)
                    SwingUtilities.invokeLater(() -> {
                        // primero mostramos una alerta bonita (Toast notification)
                        JOptionPane.showMessageDialog(VentanaPrincipal.this, 
                            "¡Atención! Se ha creado un nuevo ticket.", 
                            "Notificación en Tiempo Real", 
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // despues refrescamos la tabla automaticamente
                        cargarTickets();
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start(); // .start() es importante para que corra en paralelo
}


    public static void main(String[] args) {
        // inicio la aplicacion en el hilo de eventos de Swing para que sea seguro
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal v = new VentanaPrincipal();
            v.setVisible(true);
            v.cargarTickets();
        });
    }
}
