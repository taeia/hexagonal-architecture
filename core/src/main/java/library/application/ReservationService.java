package library.application;

import java.util.ArrayList;
import java.util.List;

import library.domain.Book;
import library.domain.Member;
import library.domain.Queue;
import library.domain.Reservation;
import library.domain.Reserving;
import library.ports.driven.BookRepository;
import library.ports.driven.QueueRepository;
import library.ports.driving.ForReservingBooks;

/**
 * Reserving. An application service: it asks {@link Reserving} to do the work and
 * reads the position back. Also the questions the screens ask about books and
 * reservations.
 */
public class ReservationService implements ForReservingBooks {

    private final Reserving reserving;
    private final BookRepository books;
    private final QueueRepository queues;

    public ReservationService(Reserving reserving, BookRepository books, QueueRepository queues) {
        this.reserving = reserving;
        this.books = books;
        this.queues = queues;
    }

    // ---- operations ----

    /**
     * Returns the position of the new reservation in the queue, counting from 1.
     */
    public int reserve(Member member, Book book) {
        Reservation reservation = reserving.reserve(member, book);
        return queues.of(book).positionOf(reservation);
    }

    // ---- questions the screens ask ----

    public List<Book> books() {
        return books.all();
    }

    public List<Reservation> reservations() {
        List<Reservation> result = new ArrayList<>();
        for (Queue queue : queues.all()) {
            result.addAll(queue.reservations());
        }
        return result;
    }

    public int queuePosition(Reservation reservation) {
        return queues.of(reservation.book()).positionOf(reservation);
    }
}
