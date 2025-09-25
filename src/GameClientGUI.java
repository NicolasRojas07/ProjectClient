import javax.swing.*;
import java.awt.*;

public class GameClientGUI extends JFrame {
    private final GameRMI game;
    private final int playerId;
    private final String playerName;

    private JButton[][] myBoardButtons;
    private JButton[][] enemyBoardButtons;
    private JTextArea logArea;

    private int[] shipSizes = {2, 3, 4};
    private int currentShip = 0;
    private boolean placingShips = true;
    private JButton readyButton;

    // Panel superior para botón de listo
    private JPanel topPanel;

    // Variables para log eficiente
    private StringBuilder logBuffer = new StringBuilder();
    private int logUpdateCounter = 0;
    private static final int LOG_UPDATE_INTERVAL = 3; // actualizar log cada 3 mensajes
    private String lastTurnMsg = ""; // evitar repetición de mensajes de turno

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
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        // Evitar resize y mantener altura fija
        logArea.setPreferredSize(new Dimension(800, 150));
        logArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Mensajes"));

        add(boardsPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        log("✅ Bienvenido " + playerName + ". Coloca tus barcos en el tablero.");

        // Polling para turnos
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
                updateBoards();
                currentShip++;
                if (currentShip >= shipSizes.length) {
                    placingShips = false;
                    showReadyButton();
                }
            } else {
                log("❌ No se pudo colocar el barco.");
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
                readyButton.setEnabled(false);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private void updateBoards() {
        try {
            char[][] myBoard = game.getBoard(playerId);
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    myBoardButtons[i][j].setText(String.valueOf(myBoard[i][j]));
                    switch (myBoard[i][j]) {
                        case 'B': myBoardButtons[i][j].setBackground(Color.GREEN); break;
                        case 'X': myBoardButtons[i][j].setBackground(Color.RED); break;
                        case 'O': myBoardButtons[i][j].setBackground(Color.GRAY); break;
                        default:  myBoardButtons[i][j].setBackground(Color.CYAN); break;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refreshTurn() {
        try {
            String msg = game.getCurrentTurn();
            if (!msg.equals(lastTurnMsg)) { // Solo si cambia
                log(msg);
                lastTurnMsg = msg;
            }

            if (msg.contains(playerName)) {
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

    // Log eficiente y siempre mostrando últimos mensajes
    private void log(String msg) {
        logBuffer.append(msg).append("\n");
        logUpdateCounter++;

        if (logUpdateCounter >= LOG_UPDATE_INTERVAL) {
            logArea.append(logBuffer.toString());
            logBuffer.setLength(0);
            logUpdateCounter = 0;

            // Auto-scroll al final
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
}
