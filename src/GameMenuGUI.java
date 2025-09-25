import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;

public class GameMenuGUI extends JFrame {
    private static GameRMI game;
    private static int playerId;
    private static String playerName;
    private static final String SERVER_IP = "192.168.20.163";

    public GameMenuGUI() {
        setTitle("Menú Batalla Naval");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10));

        JButton registerBtn = new JButton("Registrar jugador");
        JButton listBtn = new JButton("Listar jugadores");
        JButton deleteBtn = new JButton("Eliminar jugador");
        JButton startBtn = new JButton("Comenzar partida");

        add(registerBtn); add(listBtn); add(deleteBtn); add(startBtn);

        try {
            game = (GameRMI) Naming.lookup("rmi://" + SERVER_IP + "/GameBattleship");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ No se pudo conectar al servidor");
            System.exit(1);
        }

        registerBtn.addActionListener(e -> registerPlayer());
        listBtn.addActionListener(e -> listPlayers());
        deleteBtn.addActionListener(e -> deletePlayer());
        startBtn.addActionListener(e -> startGame());
    }

    private void registerPlayer() {
        try {
            playerName = JOptionPane.showInputDialog(this, "Ingrese su nombre:");
            if (playerName == null || playerName.trim().isEmpty()) return;
            playerId = game.registerPlayer(playerName);
            JOptionPane.showMessageDialog(this,
                    "✅ Jugador registrado: " + playerName + " (ID: " + playerId + ")");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void listPlayers() {
        try {
            java.util.List<String> players = game.listPlayers();
            JOptionPane.showMessageDialog(this,
                    players.isEmpty() ? "⚠️ No hay jugadores registrados"
                            : "Jugadores:\n" + String.join("\n", players));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deletePlayer() {
        try {
            String input = JOptionPane.showInputDialog(this, "Ingrese ID a eliminar:");
            if (input == null) return;
            int id = Integer.parseInt(input);
            boolean removed = game.removePlayer(id);
            JOptionPane.showMessageDialog(this,
                    removed ? "Jugador eliminado." : "No existe jugador con ese ID.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void startGame() {
        if (playerId == 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Primero registre un jugador.");
            return;
        }
        dispose();
        GameClientGUI gui = new GameClientGUI(game, playerId, playerName);
        gui.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameMenuGUI().setVisible(true));
    }
}
