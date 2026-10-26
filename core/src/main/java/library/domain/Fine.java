package library.domain;

/**
 * The charge for a late return. A value object: it is defined by the number of days
 * of delay, and it knows what that costs. A return on time has no fine, so a fine
 * for zero days does not exist.
 */
public record Fine(long daysLate) {

    public static final Money PER_DAY = Money.of("0.50");

    public Fine {
        if (daysLate < 1) {
            throw new IllegalArgumentException("A fine needs at least one day of delay, not " + daysLate + ".");
        }
    }

    public Money amount() {
        return PER_DAY.times(daysLate);
    }
}
