package server;

import java.io.IOException;
import java.util.ArrayList;
import game.*;

public class Lobby {
    private ArrayList<Player> players;
    public final int lobbyOwnerId=0;
    public final int MAX_PLAYERS_COUNT=4;
    private final String lobbyName;
    public boolean isInGame;
    public boolean serverAction;
    public Game game;

    Lobby(String name) {
        lobbyName = name;
        players = new ArrayList<>();
        isInGame=false;
        serverAction=false;
    }

    /**
     * This function adds a player to the list of players.
     *
     * @param player The player to add to the game.
     */
    public void addPlayer(Player player){
        players.add(player);
    }

    /**
     * This function returns a players ArrayList in Lobby.
     *
     * @return a players ArrayLists.
     */
    public ArrayList<Player> getPlayers(){
        return players;
    }

    /**
     * It returns the name of the lobby
     *
     * @return The lobby name.
     */
    public String getLobbyName(){
        return lobbyName;
    }

    /**
     * It starts the game and sends info to players.
     */
    public void startGame()throws IOException {
        isInGame=true;
        Server.showLobbies();
        game = new Game(players, lobbyName);
        game.sendInfoToPlayers("Jeśli chcesz opuścić grę wpisz 'QUIT'\n" +
                "Jeśli chcesz napisać wiadomość do innych graczy wpisz 'Send *twoja wiadomość*");
    }

    /**
     * It removes player from lobby.
     * If the player who left the game is the owner of the lobby,
     * then the game is stopped and all players are sent back to the main menu
     *
     * @param player the player who left the game
     */
    public void playerQuit(Player player)throws IOException{
        if(player.equals(game.players.get(game.lobbyOwnerId))){
            game.sendInfoToPlayers("Właściciel pokoju wyszedł z gry, zostałeś przeniesiony do menu głównego, wciśnij ENTER, aby kontynuować");
            for(Player player1: game.players)
                player1.isInLobby=false;
            game.players.clear();
            game=null;
            getPlayers().clear();
            Server.lobbies.remove(this);
            Server.showLobbies();
            return;
        }
        players.remove(player);
        game=null;
        Server.showLobbies();
    }

}
