package library.domain;

/**
 * One physical item of a book. An aggregate root with nothing inside it but itself,
 * identified by its number. It is available, on loan, or on hold for one member, and
 * only its own methods move it between those states. It is not part of the member who
 * borrows it, because a copy has a life when nobody holds it.
 */
public class Copy {

    private final String number;
    private final Book book;
    private Status status = Status.AVAILABLE;
    private Member heldFor;

    public Copy(String number, Book book) {
        this.number = number;
        this.book = book;
    }

    public String number() {
        return number;
    }

    public Book book() {
        return book;
    }

    /**
     * Available means neither on loan nor held for a member.
     */
    public boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    public boolean isOnLoan() {
        return status == Status.ON_LOAN;
    }

    public boolean isOnHold() {
        return status == Status.ON_HOLD;
    }

    public boolean isHeldFor(Member member) {
        return status == Status.ON_HOLD && heldFor == member;
    }

    /**
     * The member the copy is held for, or null when it is not on hold.
     */
    public Member heldFor() {
        return heldFor;
    }

    /**
     * Lends the copy to the member. A copy on loan cannot be lent again, and a copy
     * on hold can be lent only to the member it is held for.
     */
    public void lendTo(Member member) {
        if (status == Status.ON_LOAN) {
            throw new LibraryException(number + " is on loan.");
        }
        if (status == Status.ON_HOLD && heldFor != member) {
            throw new LibraryException(number + " is on hold for " + heldFor.name() + ".");
        }
        status = Status.ON_LOAN;
        heldFor = null;
    }

    /**
     * The copy comes back and is kept aside for the member.
     */
    public void holdFor(Member member) {
        requireOnLoan();
        status = Status.ON_HOLD;
        heldFor = member;
    }

    /**
     * The copy comes back and nobody is waiting for it.
     */
    public void makeAvailable() {
        requireOnLoan();
        status = Status.AVAILABLE;
        heldFor = null;
    }

    private void requireOnLoan() {
        if (status != Status.ON_LOAN) {
            throw new IllegalStateException(number + " is not on loan.");
        }
    }

    @Override
    public String toString() {
        return number + " (" + book.title() + ")";
    }

    private enum Status {
        AVAILABLE, ON_LOAN, ON_HOLD
    }
}
