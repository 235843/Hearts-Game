package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;


    Client(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        }
        catch (IOException e)
        {
            close();
        }
    }


    /**
     * "This function creates a new thread that listens for messages from the server and prints them to the console."
     *
     */
    public void listen(){
        new Thread(() -> {
            String mess;
            while (socket.isConnected()) {
                try {
                    mess = bufferedReader.readLine();
                    if(mess==null)
                        System.exit(1);
                    System.out.println(mess);
                }
                catch (IOException e){
                    close();
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * Close the socket, buffered reader, and buffered writer.
     */
    public void close()
    {
        try {
            if(this.bufferedReader!=null)
                this.bufferedReader.close();
            if(this.bufferedWriter!=null)
                this.bufferedWriter.close();
            if(this.socket!=null)
                this.socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * It creates a socket, creates a client object, starts listening for
     * incoming messages, and then waits for user input
     */
    public static void main(String[] args) {
        Socket socket = null;
        try {
            socket = new Socket("localhost", 8000);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Client client = new Client(socket);
        client.listen();

        System.out.println("Witaj! Wpisz 'R' jeśli chcesz się zarejestrować lub 'L' jeśli chcesz się zalogować ");
        while (socket.isConnected()) {
            Scanner scanner = new Scanner(System.in);
            String mess = scanner.nextLine();
            try {
                client.bufferedWriter.write(mess);
                client.bufferedWriter.newLine();
                client.bufferedWriter.flush();
            } catch (IOException e) {
                client.close();
                e.printStackTrace();
            }
        }
    }
}
