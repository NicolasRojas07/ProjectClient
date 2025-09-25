import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameClientGUI extends JFrame {
    private final GameRMI game;
    private final int playerId;
    private final String playerName;

    private JButton[][] myBoardButtons;
    private JButton[][] enemyBoardButtons;
    private JTextArea logArea;
    private JScrollPane scrollPane;

    private int[] shipSizes = {2, 3, 4};
    private int currentShip = 0;
    private boolean placingShips = true;
    private JButton readyButton;

    // Panel superior para botón de listo
    private JPanel topPanel;

    // Variables para log eficiente
    private StringBuilder logBuffer = new StringBuilder();
    private int logUpdateCounter = 0;
    private static final int LOG_UPDATE_INTERVAL = 3; // actualizar log cada 3 mensajes si no hay flush forzado
    private String lastTurnMsg = ""; // evitar repetición de mensajes de turno

    // Timer para vaciar buffer periódicamente (evita perder mensajes pequeños)
    private Timer flushTimer;

    public GameClientGUI(GameRMI game, int playerId, String playerName) {
        this.game = game;
        this.playerId = playerId;
        this.playerName = playerName;

        setTitle("Batalla Naval - " + playerName);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10,10));

        // Panel superior para botón de listo
        topPanel = new JPanel(new FlowLayout());
        add(topPanel, BorderLayout.NORTH);

        JPanel boardsPanel = new JPanel(new GridLayout(1, 2, 20, 20));

        // Tablero propio
        JPanel myBoardPanel = new JPanel(new GridLayout(10, 10));
        myBoardPanel.setBorder(BorderFactory.createTitledBorder("Tu tablero"));
        myBoardButtons = new JButton[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int x = i, y = j;
                myBoardButtons[i][j] = new JButton("~");
                myBoardButtons[i][j].setBackground(Color.CYAN);
                myBoardButtons[i][j].addActionListener(e -> {
                    if (placingShips) placeShipAt(x, y);
                });
                myBoardPanel.add(myBoardButtons[i][j]);
            }
        }

        // Tablero enemigo
        JPanel enemyBoardPanel = new JPanel(new GridLayout(10, 10));
        enemyBoardPanel.setBorder(BorderFactory.createTitledBorder("Tablero enemigo"));
        enemyBoardButtons = new JButton[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int x = i, y = j;
                enemyBoardButtons[i][j] = new JButton("~");
                enemyBoardButtons[i][j].setBackground(Color.CYAN);
                enemyBoardButtons[i][j].setEnabled(false);
                enemyBoardButtons[i][j].addActionListener(e -> {
                    try {
                        String result = game.shoot(playerId, x, y);
                        log(result);
                        // Forzamos flush porque el resultado de un disparo es importante ver al instante
                        flushLog();
                        updateBoards();
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
                enemyBoardPanel.add(enemyBoardButtons[i][j]);
            }
        }

        boardsPanel.add(myBoardPanel);
        boardsPanel.add(enemyBoardPanel);

        // Área de mensajes (log)
        logArea = new JTextArea();
        logArea.setEditable(false);
        // Mantenemos wrap vertical para evitar scrollbar horizontal
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        // JScrollPane con scrollbar vertical siempre visible
        scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(800, 150));
        // Mejora de experiencia: scroll más fluido
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        // Aseguramos preferencia de ancho de la barra (puede no sobreescribir la configuración del SO)
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Mensajes"));

        add(boardsPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        // Mensaje inicial y start del timer que vacía buffer periódicamente
        log("✅ Bienvenido " + playerName + ". Coloca tus barcos en el tablero.");
        flushTimer = new Timer(800, ev -> flushLog()); // cada 800 ms vaciamos el buffer si hay algo
        flushTimer.setRepeats(true);
        flushTimer.start();

        // Listener para detener timer cuando se cierra la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (flushTimer != null && flushTimer.isRunning()) flushTimer.stop();
            }
        });

        // Polling para turnos (llama a refreshTurn en EDT)
        new Thread(() -> {
            while (true) {
                try {
                    SwingUtilities.invokeLater(this::refreshTurn);
                    Thread.sleep(2000);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }).start();
    }

    private void placeShipAt(int x, int y) {
        try {
            int size = shipSizes[currentShip];
            String orientation = JOptionPane.showInputDialog(
                    this, "Barco de tamaño " + size + " - Orientación (H/V):");
            if (orientation == null) return;
            orientation = orientation.toUpperCase();

            boolean placed = game.placeShip(playerId, x, y, size, orientation);
            if (placed) {
                log("✅ Barco de tamaño " + size + " colocado en (" + x + "," + y + ")");
                // flush porque la colocación es un evento importante
                flushLog();
                updateBoards();
                currentShip++;
                if (currentShip >= shipSizes.length) {
                    placingShips = false;
                    showReadyButton();
                }
            } else {
                log("❌ No se pudo colocar el barco.");
                flushLog();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showReadyButton() {
        readyButton = new JButton("✅ Listo para jugar");
        topPanel.add(readyButton);
        topPanel.revalidate();
        topPanel.repaint();

        readyButton.addActionListener(e -> {
            try {
                game.setPlayerReady(playerId);
                log("⏳ Esperando a los demás jugadores...");
                // flush porque es importante notificar
                flushLog();
                readyButton.setEnabled(false);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private void updateBoards() {
        try {
            // Refrescar mi tablero
            char[][] myBoard = game.getBoard(playerId);
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    myBoardButtons[i][j].setText(String.valueOf(myBoard[i][j]));
                    switch (myBoard[i][j]) {
                        case 'B': myBoardButtons[i][j].setBackground(Color.GREEN); break; // barco
                        case 'X': myBoardButtons[i][j].setBackground(Color.RED); break;   // impacto
                        case 'O': myBoardButtons[i][j].setBackground(Color.GRAY); break;  // agua fallada
                        default:  myBoardButtons[i][j].setBackground(Color.CYAN); break;  // vacío
                    }
                }
            }

            // Refrescar tablero enemigo (lo que yo sé de él). Si falla, lo ignoramos.
            try {
                char[][] enemyBoard = game.getEnemyBoard(playerId);
                if (enemyBoard != null) {
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j < 10; j++) {
                            enemyBoardButtons[i][j].setText(String.valueOf(enemyBoard[i][j]));
                            switch (enemyBoard[i][j]) {
                                case 'X': enemyBoardButtons[i][j].setBackground(Color.RED); break;   // acierto
                                case 'O': enemyBoardButtons[i][j].setBackground(Color.GRAY); break;  // fallo
                                default:  enemyBoardButtons[i][j].setBackground(Color.CYAN); break;  // desconocido
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Si la interfaz remota no provee getEnemyBoard o falla, no rompemos la UI.
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refreshTurn() {
        try {
            String msg = game.getCurrentTurn();
            if (msg != null && !msg.equals(lastTurnMsg)) { // Solo si cambia
                log(msg);
                lastTurnMsg = msg;
                // mostramos inmediatamente cambios de turno
                flushLog();
            }

            if (msg != null && msg.contains(playerName)) {
                enableEnemyBoard(true);
            } else {
                enableEnemyBoard(false);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void enableEnemyBoard(boolean enable) {
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                enemyBoardButtons[i][j].setEnabled(enable);
    }

    // Log eficiente: acumula en buffer y lo vacía según intervalo o por timer/flush forzado.
    private void log(String msg) {
        if (msg == null) return;
        logBuffer.append(msg).append("\n");
        logUpdateCounter++;

        if (logUpdateCounter >= LOG_UPDATE_INTERVAL) {
            flushLog();
        }
    }

    // Vacía el buffer al JTextArea y hace autoscroll al final (si hay contenido)
    private void flushLog() {
        // Copiamos y limpiamos buffer rápidamente (defensa de concurrencia)
        String toAppend;
        synchronized (logBuffer) {
            if (logBuffer.length() == 0) return;
            toAppend = logBuffer.toString();
            logBuffer.setLength(0);
            logUpdateCounter = 0;
        }

        // Garantizamos ejecución en EDT y autoscroll después del append
        SwingUtilities.invokeLater(() -> {
            logArea.append(toAppend);
            // Forzamos caret al final para que se vea siempre el último mensaje
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
