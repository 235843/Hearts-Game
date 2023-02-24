package game;

import java.io.IOException;
import java.util.*;
import server.*;

public class Game {
    public final int MAX_ROUND=10;
    public final int lobbyOwnerId=0;
    public int nextPlayer;
    public final String lobbiesName;
    public final ArrayList<Player> players;
    public ArrayList<Card> deck;
    public final ArrayList<Card> cardsOnTable;
    public Suit firstSuit;
    public boolean canHeartsGo;
    public int round;
    public boolean newRound;

    public Game(ArrayList<Player> p, String name){
        nextPlayer=0;
        lobbiesName=name;
        players=p;
        round=0;
        cardsOnTable = new ArrayList<>();
        createDeck();
        newRound=true;
    }

    /**
     * Create a new deck of cards by creating a new ArrayList of cards,  then adding each card to the deck.
     */
    public void createDeck() {
        deck=new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
    }

    /**
     * The function shuffleDeck() shuffles the deck of cards
     */
    public void shuffleDeck(){
        Collections.shuffle(deck);
    }

    /**
     * This function deals cards to each player in the game
     */
    public void dealCards(){
        int i=0;
            for (Player player: players) {
                player.getHand().clear();
                while (player.getHand().size()<player.MAX_CARDS_IN_HAND) {
                    ArrayList<Card> playersHand = player.getHand();
                    playersHand.add(deck.get(i));
                    player.getHand(playersHand);
                    i++;
                }
                Collections.sort(player.getHand());
                player.isInLobby =true;
            }
    }

    /**
     * It sends a message to all the players in the game
     *
     * @param mess The message to be sent to the players
     */
    public void sendInfoToPlayers(String mess) throws IOException {
        for (Player player: players) {
            player.getBufferedWriter().write(mess);
            player.getBufferedWriter().newLine();
            player.getBufferedWriter().flush();
        }
    }

    /**
     * This function clears the played cards of all players.
     */
    public void clearPlayedCards(){
        for(Player player: players)
            player.setPlayedCard(null);
    }

    /**
     * If the card is null, return a dash, otherwise return the card's rank and suit
     *
     * @param card the card to be converted to string
     * @return String
     */
    public String fromCardToString(Card card){
        if(card==null)
            return "-";
        String stringCard = switch (card.rank.value) {
            case 11 -> "Jopek";
            case 12 -> "Dama";
            case 13 -> "Król";
            case 14 -> "As";
            default -> Integer.toString(card.rank.value);
        };
        return stringCard+" "+card.suit.label;
    }

    /**
     * It takes a string and returns a card
     *
     * @param player the player who is making the move
     * @param move String, which is the move that the player wants to make.
     * @return Card
     */
    public Card fromStringToCard(Player player, String move){
        String[] info = move.split(" ");
        if(info.length!=2)
            return null;
        for (Card card: player.getHand()) {
            info[0] = switch (info[0]){
                case "Jopek" -> Integer.toString(Rank.JACK.value);
                case "Dama" -> Integer.toString(Rank.QUEEN.value);
                case "Król" -> Integer.toString(Rank.KING.value);
                case "As" -> Integer.toString(Rank.ACE.value);
                default -> info[0];
            };
            if(info[0].equals(Integer.toString(card.rank.value)) && info[1].equals(card.suit.label)){
                return card;
            }
        }
        return null;
    }

    /**
     * It sends a message to a player
     *
     * @param player The player you want to send the message to.
     * @param mess The message to be sent to the player
     */
    public void sendToPlayer(Player player, String mess)throws IOException{
        player.getBufferedWriter().write(mess);
        player.getBufferedWriter().newLine();
        player.getBufferedWriter().flush();
    }

    /**
     * It sends information about the current state of the game to all players
     */
    public void showBoard()throws IOException{
        sendInfoToPlayers("-Pokój " + lobbiesName);
        for (Player player: players) {
            sendInfoToPlayers("Gracz "+player.getUsername()+" rzucił: "+fromCardToString(player.getPlayedCard()));

        }
        for (Player player: players) {
            if(player.equals(players.get((nextPlayer)%4))){
                sendToPlayer(player, player.getUsername() + " Twoja tura");
            }
            else{
                sendToPlayer(player, player.getUsername() +" Tura gracza " + players.get((nextPlayer)%4).getUsername());
            }
            String cardsInHand = "Twoje karty: |";
            for (Card card: player.getHand())
                cardsInHand += fromCardToString(card) + "| ";
            sendToPlayer(player, cardsInHand+"\nTwoje punkty: "+player.getPoints());

        }
    }

    /**
     * It handles action that server sent. Restart game.
     */
    public void serverAction()throws IOException{
        newRound=false;
        canHeartsGo = false; firstSuit = Suit.CLUBS;
        cardsOnTable.clear();
        clearPlayedCards();
        shuffleDeck(); dealCards();
        firstRound();
        givePlayersRandomPoints(); round++;
        sendInfoToPlayers("Serwer zarządził ponowne rozdanie");
        showBoard();
    }

    /**
     * The first round is played by the player who has the two of clubs
     */
    public void firstRound(){
        for (Player player : players) {
            for (Card card : player.getHand()) {
                if (card.suit.equals(Suit.CLUBS) && card.rank.equals(Rank.TWO)) {
                    nextPlayer = players.indexOf(player);
                    player.wasFirst = true;
                    return;
                }
            }
        }
    }

    /**
     * > This function checks if the player has any cards of the same suit as the first card played
     *
     * @param player The player whose cards are being checked.
     * @return A boolean value.
     */
    public boolean checkCardsColor(Player player){
        for(Card card: player.getHand()){
            if(card.suit.equals(firstSuit))
                return false;
        }
        return true;
    }

    /**
     * It checks if the card can be played and sends information to player if card is not valid
     *
     * @param player the player who is playing the card
     * @param move String, which is the card that the player wants to play
     * @return Boolean
     */
    public boolean canCardBePlayed(Player player, String move)throws IOException{
        Card card = fromStringToCard(player, move);
        if(card==null){
            sendToPlayer(player, "Niepoprawna karta");
            return false;
        }
        if(cardsOnTable.isEmpty()){
            if(card.suit.equals(Suit.HEARTS) && !canHeartsGo){
                sendToPlayer(player,"Nie możesz zagrać tego koloru");
                return false;
            }
            firstSuit=card.suit;
        }else if(!card.suit.equals(firstSuit) && !checkCardsColor(player)){
            sendToPlayer(player,"Przeciwnik zagrał innym kolorem");
            return false;
        }
        if(card.suit.equals(Suit.HEARTS))
            canHeartsGo=true;

        return true;
    }

    /**
     * > The player who played the highest card of the first suit played is the loser
     *
     * @return The player who has the highest card of the first suit played.
     */
    public Player makeLoser(){
        Player loser=players.get((nextPlayer+1)%4);
        for (Player player: players){
            if(player.getPlayedCard().suit.equals(firstSuit) && player.getPlayedCard().rank.value>loser.getPlayedCard().rank.value)
                loser=player;
        }
        return loser;
    }

    /**
     * If the card is a heart, add one to the score. If the card is a spade queen, add 13 to the score.
     *
     * @return The score of the cards on the table.
     */
    public int countPoints(){
        int score=0;
        for(Card card : cardsOnTable){
            if(card.suit.equals(Suit.HEARTS))
                score++;
            else if(card.suit.equals(Suit.SPADES) && card.rank.equals(Rank.QUEEN))
                score+=13;
        }
        return score;
    }

    /**
     * Give each player a random number of points.
     */
    public void givePlayersRandomPoints(){
        for (Player player: players){
            player.setPoints(player.getPoints()+ new Random().nextInt(10));
        }
    }

    /**
     * The function is called when a player lost a round. It adds the points of the player who lost the round to his score.
     * Clears the cards on the table, sets the played card of each player to null, sets the wasFirst
     * variable of each player to false, sets the nextPlayer variable to the index of the player who won the round, and
     * sets the wasFirst variable of the player who won the round to true
     *
     * @param player the player who lost the round
     */
    public void result(Player player) {
        player.setPoints(player.getPoints() + countPoints());
        cardsOnTable.clear();
        for (Player player1 : players){
            player1.setPlayedCard(null);
            player1.wasFirst=false;
        }
        nextPlayer = players.indexOf(player);
        player.wasFirst = true;
    }
}
