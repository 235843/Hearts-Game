package server;

import java.io.BufferedWriter;
import java.util.ArrayList;
import game.*;

public class Player {
    public int MAX_CARDS_IN_HAND=13;
    private final String username;
    private int points;
    private ArrayList<Card> hand;
    private final BufferedWriter bufferedWriter;
    private Card playedCard;
    public boolean wasFirst;
    public boolean isInLobby;

    Player(String name, BufferedWriter bw)
    {
        this.username = name;
        this.points = 0;
        this.bufferedWriter = bw;
        this.hand = new ArrayList<>(13);
        isInLobby =false;
        wasFirst=false;
    }

    // A getters and setters for the player class.
    public String getUsername(){
        return username;
    }

    public Card getPlayedCard(){
        return playedCard;
    }

    public int getPoints(){
        return points;
    }

    public void setPlayedCard(Card card){
        playedCard=card;
    }

    public void setPoints(int p){
        points=p;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public BufferedWriter getBufferedWriter(){
        return bufferedWriter;
    }

    public void getHand(ArrayList<Card> h){
        hand=h;
    }

    /**
     * The playCard function removes the card from the player's hand and
     * sets the playedCard variable to the card that was played.
     *
     * @param card The card to be played
     */
    public void playCard(Card card) {
        playedCard=card;
        hand.remove(card);
    }
}
