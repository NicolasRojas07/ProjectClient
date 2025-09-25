import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;

public class GameMenuGUI extends JFrame {
    private static GameRMI game;
    private static int playerId;
    private static String playerName;
    private static final String SERVER_IP = "192.168.20.163";

    private JTextArea logArea;

    public GameMenuGUI() {
        setTitle("Menú del Juego - Batalla Naval");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Área de log
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel panel = new JPanel();
        JButton btnPlayers = new JButton("👥 Ver Jugadores");
        JButton btnBoard = new JButton("🛠️ Ver Tablero");
        JButton btnEnemyBoard = new JButton("🎯 Ver Tablero Enemigo");
        JButton btnReady = new JButton("✅ Estoy Listo");
        JButton btnTurn = new JButton("🔄 Ver Turno Actual");

        panel.add(btnPlayers);
        panel.add(btnBoard);
        panel.add(btnEnemyBoard);
        panel.add(btnReady);
        panel.add(btnTurn);
        add(panel, BorderLayout.SOUTH);

        // Acciones botones
        btnPlayers.addActionListener(e -> {
            try {
                log("Jugadores conectados: " + game.listPlayers());
            } catch (Exception ex) {
                log("Error listando jugadores: " + ex.getMessage());
            }
        });

        btnBoard.addActionListener(e -> {
            try {
                char[][] board = game.getBoard(playerId);
                log("Tu tablero:");
                printBoard(board);
            } catch (Exception ex) {
                log("Error mostrando tablero: " + ex.getMessage());
            }
        });

        btnEnemyBoard.addActionListener(e -> {
            try {
                char[][] board = game.getEnemyBoard(playerId);
                log("Tablero enemigo:");
                printBoard(board);
            } catch (Exception ex) {
                log("Error mostrando tablero enemigo: " + ex.getMessage());
            }
        });

        btnReady.addActionListener(e -> {
            try {
                game.setPlayerReady(playerId);
                log("✔️ Marcado como listo. Esperando a los demás...");
            } catch (Exception ex) {
                log("Error al marcar listo: " + ex.getMessage());
            }
        });

        btnTurn.addActionListener(e -> {
            try {
                log("Turno actual: " + game.getCurrentTurn());
            } catch (Exception ex) {
                log("Error mostrando turno: " + ex.getMessage());
            }
        });

        // Timer para refrescar estado y detectar ganador
        Timer refresher = new Timer(1000, e -> {
            try {
                if (game.isGameOver()) {
                    log("🎉 El ganador es: " + game.getWinner());
                    ((Timer) e.getSource()).stop(); // 👈 Detiene el bucle
                }
            } catch (Exception ex) {
                log("Error verificando estado del juego: " + ex.getMessage());
            }
        });
        refresher.start();
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength()); // 👈 siempre baja al final
    }

    private void printBoard(char[][] board) {
        for (char[] row : board) {
            StringBuilder sb = new StringBuilder();
            for (char c : row) {
                sb.append(c).append(" ");
            }
            log(sb.toString());
        }
    }

    public static void main(String[] args) {
        try {
            game = (GameRMI) Naming.lookup("rmi://" + SERVER_IP + "/Game");
            playerName = JOptionPane.showInputDialog("Ingresa tu nombre:");
            playerId = game.registerPlayer(playerName);
            JOptionPane.showMessageDialog(null, "Registrado como jugador #" + playerId);

            SwingUtilities.invokeLater(() -> {
                new GameMenuGUI().setVisible(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error conectando al servidor: " + e.getMessage());
        }
    }
}
