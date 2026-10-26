package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ReservingTest {

    private final Book pride = new Book("978-0-14-143951-8", "Pride and Prejudice");
    private final Copy c2 = new Copy("C2", pride);
    private final Copy c3 = new Copy("C3", pride);
    private final Member john = new Member("John");
    private final Member jane = new Member("Jane");
    private final FakeCopyRepository copies = new FakeCopyRepository(c2, c3);
    private final FakeQueueRepository queues = new FakeQueueRepository();
    private final Reserving reserving = new Reserving(copies, queues);

    @Test
    void aBookWithAnAvailableCopyCannotBeReserved() {
        john.borrow(c2, march(2));

        LibraryException refusal = assertThrows(LibraryException.class, () -> reserving.reserve(jane, pride));
        assertEquals("A copy of Pride and Prejudice is available.", refusal.getMessage());
    }

    @Test
    void aMemberWithTheBookOnLoanCannotReserveIt() {
        john.borrow(c2, march(2));
        jane.borrow(c3, march(2));

        LibraryException refusal = assertThrows(LibraryException.class, () -> reserving.reserve(john, pride));
        assertEquals("John has a copy of Pride and Prejudice on loan.", refusal.getMessage());
    }

    @Test
    void aMemberCannotReserveTheSameBookTwice() {
        john.borrow(c2, march(2));
        john.borrow(c3, march(2));
        Queue queue = new Queue(pride);
        queue.join(jane);
        Reserving reserving = new Reserving(copies, new FakeQueueRepository(queue));

        LibraryException refusal = assertThrows(LibraryException.class, () -> reserving.reserve(jane, pride));
        assertEquals("Jane already has a reservation for Pride and Prejudice.", refusal.getMessage());
    }

    @Test
    void otherwiseTheReservationJoinsTheQueueAndTheQueueIsSaved() {
        john.borrow(c2, march(2));
        john.borrow(c3, march(2));

        Reservation reservation = reserving.reserve(jane, pride);

        assertSame(jane, reservation.member());
        assertSame(pride, reservation.book());
        assertEquals(1, queues.saved.size());
        assertEquals(1, queues.saved.get(0).positionOf(reservation));
    }

    private static LocalDate march(int day) {
        return LocalDate.of(2026, 3, day);
    }
}
