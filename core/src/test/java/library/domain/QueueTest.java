package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QueueTest {

    private final Book hobbit = new Book("978-0-547-92822-7", "The Hobbit");
    private final Member jane = new Member("Jane");
    private final Member mary = new Member("Mary");
    private final Member john = new Member("John");
    private final Queue queue = new Queue(hobbit);

    @Test
    void isFirstComeFirstServed() {
        Reservation marys = queue.join(mary);
        Reservation janes = queue.join(jane);

        assertSame(marys, queue.first());
        assertEquals(1, queue.positionOf(marys));
        assertEquals(2, queue.positionOf(janes));
        assertEquals(2, queue.size());
        assertTrue(queue.contains(jane));
        assertFalse(queue.contains(john));
    }

    @Test
    void aMemberCannotJoinTwice() {
        queue.join(mary);

        LibraryException refusal = assertThrows(LibraryException.class, () -> queue.join(mary));
        assertEquals("Mary already has a reservation for The Hobbit.", refusal.getMessage());
        assertEquals(1, queue.size());
    }

    @Test
    void leavingMovesTheNextMemberUp() {
        queue.join(mary);
        Reservation janes = queue.join(jane);

        queue.leave(mary);

        assertFalse(queue.contains(mary));
        assertSame(janes, queue.first());
        assertEquals(1, queue.positionOf(janes));
    }

    @Test
    void aMemberWhoIsNotInItLeavesNothing() {
        queue.join(mary);

        queue.leave(john);

        assertEquals(1, queue.size());
    }

    @Test
    void hasNobodyFirstWhenEmpty() {
        assertTrue(queue.isEmpty());
        assertThrows(IllegalStateException.class, queue::first);
    }
}
