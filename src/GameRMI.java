import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameRMI extends Remote {
    int registerPlayer(String name) throws RemoteException;
    boolean placeShip(int playerId, int x, int y, int size, String orientation) throws RemoteException;
    String shoot(int playerId, int x, int y) throws RemoteException;
    char[][] getBoard(int playerId) throws RemoteException;
    int checkWinner() throws RemoteException;

    List<String> listPlayers() throws RemoteException;
    boolean removePlayer(int playerId) throws RemoteException;
}
