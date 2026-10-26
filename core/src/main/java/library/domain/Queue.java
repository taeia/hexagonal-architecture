package library.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The reservations for one book, in the order they were made. An aggregate root: a
 * reservation exists only inside the queue of its book, and the queue is the only
 * thing that adds or removes one. It answers who is first, who is in it, and in which
 * position.
 */
public class Queue {

    private final Book book;
    private final List<Reservation> reservations = new ArrayList<>();

    public Queue(Book book) {
        this.book = book;
    }

    public Book book() {
        return book;
    }

    /**
     * The reservations in order. The list is a copy: members join and leave through
     * the queue, not through this list.
     */
    public List<Reservation> reservations() {
        return List.copyOf(reservations);
    }

    public boolean isEmpty() {
        return reservations.isEmpty();
    }

    public int size() {
        return reservations.size();
    }

    /**
     * The reservation at the head of the queue. The queue must not be empty.
     */
    public Reservation first() {
        if (isEmpty()) {
            throw new IllegalStateException("Nobody has reserved " + book.title() + ".");
        }
        return reservations.get(0);
    }

    public boolean contains(Member member) {
        return reservationOf(member) != null;
    }

    /**
     * The position of a reservation, counting from 1.
     */
    public int positionOf(Reservation reservation) {
        int index = reservations.indexOf(reservation);
        if (index < 0) {
            throw new IllegalArgumentException("The reservation is not in the queue.");
        }
        return index + 1;
    }

    /**
     * The member joins the end of the queue. A member cannot be in it twice.
     */
    public Reservation join(Member member) {
        if (contains(member)) {
            throw new LibraryException(member.name() + " already has a reservation for " + book.title() + ".");
        }
        Reservation reservation = new Reservation(book, member);
        reservations.add(reservation);
        return reservation;
    }

    /**
     * The member's reservation ends, and everyone behind them moves up. A member who
     * is not in the queue leaves nothing.
     */
    public void leave(Member member) {
        Reservation reservation = reservationOf(member);
        if (reservation != null) {
            reservations.remove(reservation);
        }
    }

    private Reservation reservationOf(Member member) {
        for (Reservation reservation : reservations) {
            if (reservation.member() == member) {
                return reservation;
            }
        }
        return null;
    }
}
