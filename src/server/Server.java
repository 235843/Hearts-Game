package server;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

public class Server {
    private final ServerSocket serverSocket;
    private static int serverPort;
    private static String serverHost;
    public static ArrayList<ClientHandler> clientHandlers;
    public static ArrayList<Lobby> lobbies;

    Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        clientHandlers = new ArrayList<>();
        lobbies = new ArrayList<>();
    }

    /**
     * It reads the configuration.xml file and sets the serverHost and
     * serverPort variables to the values found in the file
     */
    public static void readConfiguration(){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File("./configuration/configuration.xml"));

            NodeList hostConfiguration = document.getElementsByTagName("host");
            serverHost = hostConfiguration.item(0).getTextContent();

            NodeList portConfiguration = document.getElementsByTagName("port");
            serverPort = Integer.parseInt(portConfiguration.item(0).getTextContent());

        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }

    }

    // A method that shows all the lobbies and players in them that are currently in the server.
    public static void showLobbies(){
        if(lobbies.isEmpty()){
            System.out.println("Nie istnieją żadne pokoje");
            return;
        }
        System.out.println("Pokoje:");
        for (Lobby lobby: lobbies){
            System.out.print("Pokój "+ lobby.getLobbyName() );
            if(lobby.isInGame)
                System.out.println(" w trakcie gry");
            else
                System.out.println(" w oczekiwaniu");
            System.out.print("Gracze: ");
            for (Player player: lobby.getPlayers()){
                System.out.print(player.getUsername()+" | ");
            }
            System.out.println("");
        }
    }

    /**
     * "While the server socket is open, accept new connections and create a new thread for each one."
     *
     * The first thing we do is create a while loop that will run as long as the server socket is open. We then call the
     * accept() method on the server socket. This method will block until a new connection is made. Once a new connection
     * is made, we create a new ClientHandler object and pass it the socket that was created when the connection was made.
     * We then create a new thread and pass it the ClientHandler object. We then start the thread
     */
    void startServer()
    {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * It starts a new thread that listens for commands from the server
     */
    public void serverAction(){
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Wpisz 'stop *nazwa pokoju*', aby przerwać rozgrywkę");
            Server.showLobbies();
            while (!serverSocket.isClosed()){
                String command = scanner.nextLine();
                String[] info = command.split(" ");
                if(info.length!=2){
                    System.out.println("To nie jest poprawna komenda");
                    continue;
                }
                if(info[0].equalsIgnoreCase("stop")){
                    boolean hasStopped = false;
                    for(Lobby lobby: lobbies){
                        if(lobby.getLobbyName().equals(info[1]) && lobby.isInGame){
                            hasStopped=true;
                            try {
                                lobby.game.serverAction();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Przerwano grę");
                            break;
                        }
                    }
                    if(!hasStopped){
                        System.out.println("Nie można przerwać gry w tym pokoju");
                    }
                }
            }
        }).start();
    }

    /**
     * The main function reads the configuration file, creates a server socket, creates a server object, and starts the
     * server
     */
    public static void main(String[] args)
    {
        Server.readConfiguration();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Server.serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Server server = new Server(serverSocket);
        server.serverAction();
        server.startServer();

    }
}
