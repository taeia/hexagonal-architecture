package library.ports.driving;

import java.util.List;

import library.domain.Book;
import library.domain.Member;
import library.domain.Reservation;

/**
 * The conversation about reservations: reserve a book, and the questions about books
 * and queues. A driving port.
 */
public interface ForReservingBooks {

    // ---- operations ----

    /**
     * Returns the position of the new reservation in the queue, counting from 1.
     */
    int reserve(Member member, Book book);

    // ---- questions ----

    List<Book> books();

    List<Reservation> reservations();

    int queuePosition(Reservation reservation);
}
