package game;

public class Card implements Comparable<Card> {
    public Suit suit;
    Rank rank;

    Card( Suit s, Rank r){
        this.suit =s;
        this.rank =r;
    }

    /**
     * If the suits are different, then the suit with the higher value is the greater card.
     * Otherwise, the card with the higher value is the greater card
     *
     * @param other the other card to compare to this
     * @return The suit of the card.
     */
    @Override
    public int compareTo(Card other) {
        int suitCompare = suit.compareTo(other.suit);
        if (suitCompare != 0) {
            return suitCompare;
        } else {
            return rank.compareTo(other.rank);
        }
    }
}


