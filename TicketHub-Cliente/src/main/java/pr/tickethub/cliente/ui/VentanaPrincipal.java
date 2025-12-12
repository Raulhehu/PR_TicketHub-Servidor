package pr.tickethub.cliente.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.json.JSONObject;
import pr.tickethub.cliente.net.TicketHubAPIClient;

// esta es la clase principal donde armo toda la interfaz grafica usando swing
public class VentanaPrincipal extends JFrame {

    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;
    private JButton btnEnProceso, btnCerrado, btnNuevo, btnRefrescar;

    // defino mi paleta de colores aqui para usarlos facil en todos los componentes y mantener el estilo
    private static final Color COLOR_HEADER = new Color(31, 33, 33);     // gris oscuro
    private static final Color COLOR_ACCENT = new Color(33, 128, 141);    // teal
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
        btnEnProceso.addActionListener(e -> cambiarEstadoSeleccionado("EN_PROCESO"));

        btnCerrado = crearBoton("Marcar Cerrado", new Color(244, 67, 54));
        btnCerrado.addActionListener(e -> cambiarEstadoSeleccionado("CERRADO"));

        JButton btnSalir = crearBoton("Salir", new Color(158, 158, 158));
        btnSalir.addActionListener(e -> System.exit(0));

        panelBotones.add(btnNuevo);
        panelBotones.add(btnRefrescar);
        panelBotones.add(btnEnProceso);
        panelBotones.add(btnCerrado);
        panelBotones.add(btnSalir);

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

        String[] columnas = { "ID", "Titulo", "Estado", "Prioridad", "Creado en" };
        // sobrescribo el modelo para evitar que el usuario edite las celdas directamente
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
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

        panel.add(scroll, BorderLayout.CENTER);
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
                    List<Map<String,Object>> tickets = get();
                    modelo.setRowCount(0); // limpio la tabla
                    for (Map<String,Object> t : tickets) {
                        modelo.addRow(new Object[] {
                            t.get("id"),
                            t.get("titulo"),
                            t.get("estado"),
                            t.get("prioridad"),
                            t.get("creado_en")
                        });
                    }
                    lblTotal.setText(tickets.size() + " tickets");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Error al cargar tickets: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // valido que haya algo seleccionado antes de intentar cambiar el estado
    private void cambiarEstadoSeleccionado(String nuevoEstado) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                    "Por favor selecciona un ticket de la tabla",
                    "Seleccion requerida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idTicket = (int) modelo.getValueAt(fila, 0);
        enviarCambioEstadoTCP(idTicket, nuevoEstado);
    }

    // uso otro SwingWorker para conectar por TCP socket sin bloquear la ventana
    private void enviarCambioEstadoTCP(int idTicket, String estado) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // me conecto al puerto 9090 del servidor para enviar el comando
                try (Socket socket = new Socket("localhost", 9090);
                     PrintWriter out = new PrintWriter(
                             new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                     BufferedReader in = new BufferedReader(
                             new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                    // leo el mensaje de bienvenida del servidor
                    in.readLine();

                    // construyo el comando manualmente con formato parecido a JSON
                    String comando = "SET_ESTADO {\"id\":" + idTicket + ",\"estado\":\"" + estado + "\"}";
                    out.println(comando);

                    // verifico si el servidor me respondio OK
                    String respuesta = in.readLine();
                    if (respuesta != null && respuesta.startsWith("OK")) {
                        JOptionPane.showMessageDialog(VentanaPrincipal.this,
                                "Ticket " + idTicket + " cambio a " + estado,
                                "Exito",
                                JOptionPane.INFORMATION_MESSAGE);
                        // si salio bien, recargo la tabla para ver el cambio reflejado
                        cargarTickets();
                    } else {
                        JOptionPane.showMessageDialog(VentanaPrincipal.this,
                                "Error: " + respuesta,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                // manejo cualquier error de conexion que haya ocurrido en el doInBackground
                try {
                    get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Error al conectar con servidor TCP: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
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
private void crearTicketViaREST(String titulo, String descripcion) {
    new SwingWorker<Integer, Void>() {
        @Override
        protected Integer doInBackground() throws Exception {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8081/tickets");
                java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
                
                // configuro para hacer un POST y aviso que mando JSON
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                // construyo el objeto JSON usando la libreria para no batallar con las comillas
                JSONObject json = new JSONObject();
                json.put("titulo", titulo);
                json.put("descripcion", descripcion);
                json.put("id_cliente", JSONObject.NULL);
                json.put("id_categoria", JSONObject.NULL);

                String jsonStr = json.toString();
                byte[] bytes = jsonStr.getBytes("UTF-8");

                // habilito la salida para poder escribir el cuerpo del mensaje
                con.setDoOutput(true);
                try (java.io.OutputStream os = con.getOutputStream()) {
                    os.write(bytes);
                }

                // si el codigo es 201 Created es que todo salio bien
                int status = con.getResponseCode();
                if (status == 201) {
                    try (BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                        String respuesta = in.readLine();
                        JSONObject obj = new JSONObject(respuesta);
                        return obj.getInt("id");
                    }
                } else {
                    throw new RuntimeException("Error HTTP: " + status);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        @Override
        protected void done() {
            try {
                // verifico si me devolvio un ID valido para confirmar al usuario
                int idNuevo = get();
                if (idNuevo > 0) {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Ticket creado con ID: " + idNuevo,
                            "Exito",
                            JOptionPane.INFORMATION_MESSAGE);
                    cargarTickets(); // actualizo la lista automaticamente
                } else {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this,
                            "Error al crear el ticket",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(VentanaPrincipal.this,
                        "Error: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
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