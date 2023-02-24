package game;

public enum Suit {
    CLUBS("TREFL"),
    DIAMONDS("KARO"),
    SPADES("PIK"),
    HEARTS("KIER");

    public final String label;

    Suit(String label){
        this.label = label;
    }
}
