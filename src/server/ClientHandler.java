package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import game.*;

public class ClientHandler implements Runnable{
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    public Player player;
    public Lobby lobby;

    ClientHandler(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Server.clientHandlers.add(this);
            lobby=null;
        }
        catch (IOException e){
            close();
        }

    }

    /**
     * It closes the socket, buffered reader and buffered writer
     */
    void close() {
        try {
            if(this.bufferedReader!=null)
                this.bufferedReader.close();
            if(this.bufferedWriter!=null)
                this.bufferedWriter.close();
            if(this.socket!=null)
                this.socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * It writes the message to the buffered writer, adds a new line, and flushes the buffer
     *
     * @param mess The message to be sent to the server.
     */
    public void send(String mess) throws IOException {
        bufferedWriter.write(mess);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    /**
     * It sends a message to all the players in the lobby
     *
     * @param mess The message to be sent to the players
     */
    public void sendInfoToPlayers(String mess) throws IOException {
        for (Player player: lobby.getPlayers()) {
            player.getBufferedWriter().write(mess);
            player.getBufferedWriter().newLine();
            player.getBufferedWriter().flush();
        }
    }

    /**
     * It reads the user's input, checks if the input is in the correct format,
     * checks if the username is already taken,
     * checks if the passwords match, and if everything is correct,
     * it writes the username and password to the file
     */
    public void register() throws IOException {
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter("users.txt", true));
        BufferedReader fileReader = new BufferedReader(new FileReader("users.txt"));
        send("Podaj nazw?? u??ytkownika, has??o oraz powt??rzenie has??a");
        while (socket.isConnected()) {
            String user = bufferedReader.readLine();
            String[] data = user.split(" ");
            if(data.length!=3) {
                send("B????dny format\nSpr??buj ponownie :");
                continue;
            }
            String line = fileReader.readLine();
            while (line != null) {
                if (line.contains(data[0])) {
                    send("Nazwa u??ytkownika jest zaj??ta\nSpr??buj ponownie");
                    continue;
                }
                line = fileReader.readLine();
            }
            if(!data[1].equals(data[2])) {
                send("Podane has??a s?? r????ne\nSpr??buj ponownie");
                continue;
            }
            username = data[0];
            fileWriter.write(data[0]+" "+data[1]); fileWriter.newLine();
            fileWriter.close();
            send("Zosta??e?? zarejestrowany");
            return;
        }
    }

    /**
     * It reads the user's input and checks if it matches any of the lines in the users.txt file.
     * If it does, it sets the username variable to the user's name and sends a message to the user.
     * If it doesn't, it sends a message to the user and waits for another input
     */
    public void login() {
        try {
            send("Podaj nazw?? u??ytkownika has??o");
            while (socket.isConnected()) {
                BufferedReader fileReader = new BufferedReader(new FileReader("users.txt"));
                String user = bufferedReader.readLine();
                String line = fileReader.readLine();
                while (line != null) {
                    if (line.equals(user)) {
                        username = user.split(" ")[0];
                        fileReader.close();
                        send("Zosta??e?? zalogowany");
                        return;
                    }
                    line = fileReader.readLine();
                }
                send("B????dna nazwa u??ytkownika lub has??o\nSpr??buj ponownie");
                fileReader.close();
            }
        }catch (IOException e) {
            close();
            e.printStackTrace();
        }
    }

    /**
     * It creates a new lobby
     */
    public void createLobby()throws IOException {
        send("Podaj nazw?? pokoju");
        String name;
        while (true) {
            name = bufferedReader.readLine();
            boolean isNameUsed=false;
            for (Lobby lobby:Server.lobbies){
                if(lobby.getLobbyName().equals(name)){
                    isNameUsed=true;
                    break;
                }
            }
            if(isNameUsed){
                send("Ta nazwa jest ju?? zaj??ta");
                continue;
            }
            break;
        }
        lobby = new Lobby(name);
        lobby.addPlayer(player);
        player.isInLobby =true;
        Server.lobbies.add(lobby);
        Server.showLobbies();
        showCurrentLobby("Utworzono pok??j");
        send("Wpisz 'start' aby rozpocz????");
    }

    /**
     * It allows the user to join a lobby or create a new one
     */
    public void join()throws IOException {
        String command;
        send("Wpisz 'R', ??eby od??wie??y??");
        while (!socket.isClosed()){
            showRooms();
            command = bufferedReader.readLine();
            if(command.equalsIgnoreCase("C")){
                createLobby();
                break;
            }
            else if(command.equalsIgnoreCase("R"))
                continue;
            String[] comms = command.split(" ");

            if(comms.length<2 || !comms[0].equalsIgnoreCase("Join")) {
                send("Niepoprawna komenda, spr??buj ponownie");
                continue;
            }
            lobby = findLobby(command.substring(command.indexOf(" ")+1));
            if(lobby==null) {
                send("Nie ma takiego pokoju, spr??buj ponownie");
                continue;
            }
            if(lobby.getPlayers().size()>3)
            {
                send("Podany pok??j jest pe??ny, spr??buj ponownie");
                continue;
            }
            lobby.addPlayer(player);
            player.isInLobby =true;
            showCurrentLobby("Gracz "+username+" do????czy?? do pokoju");
            Server.showLobbies();
            break;
        }

    }

    /**
     * Find a lobby by name.
     *
     * @param name The name of the lobby you want to find.
     * @return The lobby with the name that is given as a parameter.
     */
    public Lobby findLobby(String name) {
        for (Lobby lobby: Server.lobbies) {
            if(lobby.getLobbyName().equals(name))
                return lobby;
        }
        return null;
    }

    /**
     * Find the lobby that contains the player.
     *
     * @return The lobby that the player is in.
     */
    public Lobby findLobbyByPlayer() {
        for (Lobby lobby: Server.lobbies) {
            for (Player player1: lobby.getPlayers()) {
                if(player1.equals(player))
                    return lobby;
            }
        }
        return null;
    }

    /**
     * It sends a message to the client, informing him about the available lobbies
     */
    public void showRooms()throws IOException {
        if(Server.lobbies.isEmpty())
            send("Brak wolnych pokoi, wpisz 'C', ??eby utworzy?? nowy");
        else
            send("Wpisz 'Join *nazwa pokoju*', ??eby wej???? do pokoju");
        for (Lobby lobby: Server.lobbies)
            send(lobby.getLobbyName() + " "+ lobby.getPlayers().size() +"/4");
    }

    /**
     * It returns an ArrayList of ClientHandlers that are in the lobby
     *
     * @return An ArrayList of ClientHandlers.
     */
    public ArrayList<ClientHandler> getPlayersInLobby(){
        ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
        for (Player player: lobby.getPlayers()) {
            for (ClientHandler clientHandler: Server.clientHandlers) {
                if(player.getUsername().equals(clientHandler.username))
                {
                    clientHandler.player.isInLobby =true;
                    clientHandlers.add(clientHandler);
                    break;
                }
            }
        }
        return clientHandlers;
    }

    /**
     * It sends a message to all players in the lobby
     *
     * @param mess message to be sent to all players in the lobby
     */
    public void showCurrentLobby(String mess) throws IOException{
        if(lobby==null)
            return;
        for (ClientHandler clientHandler:getPlayersInLobby()) {
            if(mess!=null)
                clientHandler.send(mess);
            clientHandler.send("Obecny pok??j: "+lobby.getLobbyName() +" "+ lobby.getPlayers().size()+"/4");
            clientHandler.send("Gracze:");
            for (ClientHandler clientHandler1:getPlayersInLobby())
                clientHandler.send(clientHandler1.username);
        }
    }

    /**
     * It sends scoreboard to all players in the lobby
     */
    public void showScoreboard()throws IOException{
        sendInfoToPlayers("-Wyniki");
        for(Player player: lobby.getPlayers()){
            sendInfoToPlayers(player.getUsername()+": "+player.getPoints());
        }
    }

    /**
     * It removes a player from the lobby and shows the current lobby
     *
     * @param player1 the player that is leaving the lobby
     */
    public void removePlayer(Player player1) throws IOException {
        lobby.getPlayers().remove(player1);
        showCurrentLobby("Gracz "+username+" opu??ci?? pok??j\n" +
                "Wci??nij 'ENTER', ??eby kontynuowa??");
    }

    /**
     * It's a menu that allows the user to create a new lobby or join an existing one
     */
    public void menu() throws IOException{
        String choice;
        while (!socket.isClosed()) {
            send("""
                            Je??li chcesz utworzy?? nowy pok??j wpisz 'C'
                            Je??li chcesz do????czy?? do istniej??cego pokoju wpisz 'J'
                            Je??li chcesz wyj???? wpisz 'Q'""");
            choice = bufferedReader.readLine();
            if (choice.equalsIgnoreCase("J")){
                join();break;
            }
            else if (choice.equalsIgnoreCase("C")){
                createLobby();break;
            }
            else if(choice.equalsIgnoreCase("Q")){
                close();
                return;
            }
            else {
                send("Podano nie prawid??ow?? komend??, spr??buj ponownie");
            }
        }
    }

    /**
     * It handles the client's connection to the server
     */
    @Override
    public void run() {
        String choice;
            try {
                while (!socket.isClosed()) {
                    choice = bufferedReader.readLine();
                    if (choice.equalsIgnoreCase("L"))
                        login();
                    else if (choice.equalsIgnoreCase("R"))
                        register();
                    else {
                        send("Podano nieprawid??ow?? komend??, spr??buj ponownie");
                        continue;
                    }
                    player = new Player(username, bufferedWriter);
                    break;
                }
                while (!socket.isClosed()) {
                    if(player.isInLobby){
                        Lobby lobby = findLobbyByPlayer();
                        send("Je??li chcesz wyj???? z lobby wpisz'QUIT'\n" +
                                "Je??li chcesz wys??a?? wiadomo???? do graczy wpisz'SEND *twoja wiadomo????*'");
                        send("Oczekiwanie na w??a??ciciela pokoju");
                        while (!socket.isClosed()) {
                            if(lobby.isInGame){
                                game();
                                if(lobby.getPlayers().contains(player))
                                    send("Je??li chcesz wyj???? z lobby wpisz'QUIT'\n" +
                                        "Je??li chcesz wys??a?? wiadomo???? do graczy wpisz'SEND *twoja wiadomo????*'");
                                else
                                    break;
                            }
                            String command = bufferedReader.readLine();
                            if(lobby.getPlayers().isEmpty())
                                break;
                            if (command.equalsIgnoreCase("QUIT")) {
                                if(player.equals(lobby.getPlayers().get(lobby.lobbyOwnerId))){
                                    for(Player player: lobby.getPlayers())
                                        player.isInLobby=false;
                                    lobby.getPlayers().clear();
                                    Server.lobbies.remove(lobby);
                                    Server.showLobbies();
                                    break;
                                }
                                removePlayer(player);
                                player.isInLobby = false;
                                Server.showLobbies();
                                break;
                            }
                            if(player.equals(lobby.getPlayers().get(lobby.lobbyOwnerId)) && lobby.getPlayers().size()==lobby.MAX_PLAYERS_COUNT && command.equalsIgnoreCase("start")){
                                lobby.startGame();
                                continue;
                            }
                            String[] info = command.split(" ");
                            if(info.length>1)
                            {
                                if(info[0].equalsIgnoreCase("send")){
                                    sendInfoToPlayers(username+": "+command.substring(command.indexOf(" ")+1));
                                    continue;
                                }
                            }
                            send("Niepoprawna komenda");
                        }
                    }
                    menu();
                }
            } catch (IOException e) {
                close();
                e.printStackTrace();
            }
    }

    /**
     * It's a game loop that handles game progression
     */
    public void game()throws IOException{
        Lobby lobby = findLobbyByPlayer();
        if(lobby.game==null){
            return;
        }
        lobby.game.round=0;
        player.setPoints(0);
        while (lobby.game.round<lobby.game.MAX_ROUND){
            if(lobby.game.newRound){
                lobby.game.newRound=false;
                lobby.game. canHeartsGo = false; lobby.game.firstSuit = Suit.CLUBS;
                lobby.game.cardsOnTable.clear(); lobby.game.clearPlayedCards();
                lobby.game.shuffleDeck(); lobby.game.dealCards();
                lobby.game.firstRound(); lobby.game.showBoard();
            }
            while (true){
                String move = bufferedReader.readLine();
                if (lobby.game==null)
                    return;
                if(move.equalsIgnoreCase("QUIT")){
                    lobby.playerQuit(player);
                    showCurrentLobby("Gracz "+username+" opu??ci?? pok??j\n" +
                            "Wci??nij 'ENTER', ??eby kontynuowa??");
                    return;
                }
                String[] info = move.split(" ");
                if(info.length<2){
                    send("Niepoprawna komenda");
                    continue;
                }
                if(info[0].equalsIgnoreCase("send")){
                    sendInfoToPlayers(username+": "+move.substring(move.indexOf(" ")+1));
                    continue;
                }
                if(!player.equals(lobby.game.players.get(lobby.game.nextPlayer))){
                    send("To nie jest twoja kolej na zagranie");
                    continue;
                }
                if (!lobby.game.canCardBePlayed(player, move))
                    continue;
                Card card = lobby.game.fromStringToCard(player, move);
                player.playCard(card);
                if (lobby.game.cardsOnTable.isEmpty())
                    lobby.game.firstSuit = card.suit;
                lobby.game.cardsOnTable.add(card);
                if (lobby.game.cardsOnTable.size() == lobby.game.players.size()) {
                    lobby.game.showBoard();lobby.game.result(lobby.game.makeLoser());
                    lobby.game.showBoard();
                    if(player.getHand().isEmpty())
                        break;
                    continue;
                }
                lobby.game.nextPlayer = (lobby.game.nextPlayer +1) % lobby.getPlayers().size();
                lobby.game.showBoard();
            }
            showScoreboard();
            lobby.game.newRound=true;
            lobby.game.round++;
        }
        sendInfoToPlayers("Gra si?? zako??czy??a");
        showScoreboard();
        lobby.isInGame=false;
        Server.showLobbies();
    }
}
