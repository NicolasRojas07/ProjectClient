import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameRMI extends Remote {
    int registerPlayer(String name) throws RemoteException;
    boolean placeShip(int playerId, int x, int y, int size, String orientation) throws RemoteException;
    String shoot(int playerId, int x, int y) throws RemoteException;
    char[][] getBoard(int playerId) throws RemoteException;
    String getCurrentTurn() throws RemoteException;

    // Gestión de jugadores
    List<String> listPlayers() throws RemoteException;
    boolean removePlayer(int playerId) throws RemoteException;

    // Listo para jugar
    void setPlayerReady(int playerId) throws RemoteException;
    boolean allPlayersReady() throws RemoteException;

    // Tablero del enemigo (solo aciertos y fallos)
    char[][] getEnemyBoard(int playerId) throws RemoteException;

    // 👇 Nuevo para validar ganador
    int checkWinner() throws RemoteException;
}
