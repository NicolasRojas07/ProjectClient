import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;

public class GameClientGUI extends JFrame {
    private final GameRMI game;
    private final int playerId;
    private final String playerName;
    private final JTextArea logArea;
    private final JButton[][] myBoardButtons;
    private final JButton[][] enemyBoardButtons;

    public GameClientGUI(GameRMI game, int playerId, String playerName) {
        this.game = game;
        this.playerId = playerId;
        this.playerName = playerName;

        setTitle("Batalla Naval - Jugador: " + playerName);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardsPanel = new JPanel(new GridLayout(1, 2));

        myBoardButtons = new JButton[10][10];
        enemyBoardButtons = new JButton[10][10];

        // 📌 Tablero propio
        JPanel myBoardPanel = new JPanel(new GridLayout(10, 10));
        myBoardPanel.setBorder(BorderFactory.createTitledBorder("Tu tablero"));
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                myBoardButtons[i][j] = new JButton("~");
                myBoardButtons[i][j].setEnabled(false);
                myBoardPanel.add(myBoardButtons[i][j]);
            }
        }

        // 📌 Tablero enemigo
        JPanel enemyBoardPanel = new JPanel(new GridLayout(10, 10));
        enemyBoardPanel.setBorder(BorderFactory.createTitledBorder("Tablero enemigo"));
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                final int x = i, y = j;
                enemyBoardButtons[i][j] = new JButton("~");
                int finalI = i;
                int finalJ = j;
                enemyBoardButtons[i][j].addActionListener(e -> shoot(finalI, finalJ));
                enemyBoardPanel.add(enemyBoardButtons[i][j]);
            }
        }

        boardsPanel.add(myBoardPanel);
        boardsPanel.add(enemyBoardPanel);

        // 📌 Log con Scroll
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(boardsPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        refreshBoards();
    }

    private void shoot(int x, int y) {
        try {
            String result = game.shoot(playerId, x, y);
            log("Disparo en (" + x + "," + y + "): " + result);

            // refrescar tableros
            refreshBoards();

            // 👉 Revisar si alguien ganó
            int winnerId = game.checkWinner();
            if (winnerId != -1) {
                if (winnerId == playerId) {
                    JOptionPane.showMessageDialog(this, "🎉 ¡Has ganado la partida!");
                } else {
                    JOptionPane.showMessageDialog(this, "😢 Has perdido. Ganó tu oponente.");
                }
                dispose();
                SwingUtilities.invokeLater(() -> new GameMenuGUI().setVisible(true));
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void refreshBoards() {
        try {
            char[][] myBoard = game.getBoard(playerId);
            char[][] enemyBoard = game.getEnemyBoard(playerId);

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    myBoardButtons[i][j].setText(String.valueOf(myBoard[i][j]));
                    enemyBoardButtons[i][j].setText(String.valueOf(enemyBoard[i][j]));
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength()); // 📌 autoscroll
    }
}
