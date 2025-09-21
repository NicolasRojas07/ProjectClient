import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameClientGUI extends JFrame {
    private final GameRMI game;
    private final int playerId;
    private final String playerName;

    private JButton[][] myBoardButtons;
    private JButton[][] enemyBoardButtons;
    private JTextArea logArea;

    private int[] shipSizes = {2, 3, 4}; // barcos a colocar
    private int currentShip = 0;          // índice del barco actual
    private boolean placingShips = true;  // modo colocación de barcos

    public GameClientGUI(GameRMI game, int playerId, String playerName) {
        this.game = game;
        this.playerId = playerId;
        this.playerName = playerName;

        setTitle("Batalla Naval - " + playerName);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de tableros
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

                // 🔹 Usar clicks para colocar barcos
                myBoardButtons[i][j].addActionListener(e -> {
                    if (placingShips) {
                        placeShipAt(x, y);
                    }
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
                enemyBoardButtons[i][j].setEnabled(false); // desactivado hasta terminar colocación

                enemyBoardButtons[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            String result = game.shoot(playerId, x, y);
                            log("Disparo en (" + x + "," + y + "): " + result);
                            updateBoards();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                enemyBoardPanel.add(enemyBoardButtons[i][j]);
            }
        }

        boardsPanel.add(myBoardPanel);
        boardsPanel.add(enemyBoardPanel);

        // Área de mensajes
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Mensajes"));

        add(boardsPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        log("✅ Bienvenido " + playerName + ". Coloca tus barcos en el tablero.");
        log("👉 Haz clic en las casillas para colocar tus barcos.");
    }

    // 🔹 Colocar barcos en el tablero propio
    private void placeShipAt(int x, int y) {
        try {
            int size = shipSizes[currentShip];
            String orientation = JOptionPane.showInputDialog(
                    this,
                    "Barco de tamaño " + size + " - Orientación (H/V):"
            );

            if (orientation == null) return; // cancelar
            orientation = orientation.toUpperCase();

            boolean placed = game.placeShip(playerId, x, y, size, orientation);
            if (placed) {
                log("✅ Barco de tamaño " + size + " colocado en (" + x + "," + y + ")");
                updateBoards();
                currentShip++;

                if (currentShip >= shipSizes.length) {
                    placingShips = false;
                    enableEnemyBoard(true);
                    log("🚀 Todos los barcos colocados. ¡Comienza la batalla!");
                }
            } else {
                log("❌ No se pudo colocar el barco en (" + x + "," + y + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Activar/desactivar tablero enemigo
    private void enableEnemyBoard(boolean enable) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                enemyBoardButtons[i][j].setEnabled(enable);
            }
        }
    }

    // 🔹 Actualizar tableros desde el servidor
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Mensajes en el área de texto
    private void log(String msg) {
        logArea.append(msg + "\n");
    }
}
