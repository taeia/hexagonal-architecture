package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The operations that cross aggregates, tested without any adapter module. The
 * repositories and the clock are the fakes in this directory, built on the interfaces
 * the core declares as driven ports. The clock belongs to this test and to no other.
 */
class LendingTest {

    private final Book hobbit = new Book("978-0-547-92822-7", "The Hobbit");
    private final Copy c1 = new Copy("C1", hobbit);
    private final Member john = new Member("John");
    private final Member mary = new Member("Mary");
    private final FakeMemberRepository members = new FakeMemberRepository(john, mary);
    private final FakeCopyRepository copies = new FakeCopyRepository(c1);
    private final FakeQueueRepository queues = new FakeQueueRepository();
    private final FakeClock clock = new FakeClock(march(2));
    private final Lending lending = new Lending(members, copies, queues, clock);

    @Test
    void borrowingStartsALoanAndSavesTheMemberAndTheCopy() {
        Loan loan = lending.borrow(john, c1);

        assertSame(c1, loan.copy());
        assertEquals(march(16), loan.dueDate());
        assertTrue(c1.isOnLoan());
        assertEquals(List.of(loan), john.loans());
        assertEquals(List.of(john), members.saved);
        assertEquals(List.of(c1), copies.saved);
    }

    @Test
    void aReturnedCopyNobodyWaitsForBecomesAvailable() {
        Loan loan = lending.borrow(john, c1);
        clock.set(march(16));

        assertEquals(0, lending.returnCopy(loan));

        assertTrue(c1.isAvailable());
        assertTrue(john.loans().isEmpty());
    }

    @Test
    void aLateReturnChargesTheMember() {
        Loan loan = lending.borrow(john, c1);
        clock.set(march(19));

        assertEquals(3, lending.returnCopy(loan));

        assertEquals(Money.of("1.50"), john.debt());
        assertTrue(c1.isAvailable());
    }

    @Test
    void aReturnedCopyIsHeldForTheFirstInTheQueue() {
        Queue queue = queueWith(mary);
        Lending lending = new Lending(members, copies, new FakeQueueRepository(queue), clock);
        Loan loan = lending.borrow(john, c1);

        lending.returnCopy(loan);

        assertTrue(c1.isHeldFor(mary));
        assertSame(mary, queue.first().member());
    }

    @Test
    void borrowingAHeldCopyEndsTheReservation() {
        Queue queue = queueWith(mary);
        FakeQueueRepository queues = new FakeQueueRepository(queue);
        Lending lending = new Lending(members, copies, queues, clock);
        lending.returnCopy(lending.borrow(john, c1));

        lending.borrow(mary, c1);

        assertTrue(c1.isOnLoan());
        assertTrue(queue.isEmpty());
        assertEquals(List.of(queue), queues.saved);
    }

    @Test
    void aReservationBlocksRenewal() {
        Lending lending = new Lending(members, copies, new FakeQueueRepository(queueWith(mary)), clock);
        Loan loan = lending.borrow(john, c1);

        LibraryException refusal = assertThrows(LibraryException.class, () -> lending.renew(loan));
        assertEquals("The Hobbit is reserved.", refusal.getMessage());
    }

    @Test
    void renewalMovesTheDueDateWhenNobodyWaits() {
        Loan loan = lending.borrow(john, c1);
        clock.set(march(10));

        lending.renew(loan);

        assertEquals(march(30), loan.dueDate());
    }

    private Queue queueWith(Member member) {
        Queue queue = new Queue(hobbit);
        queue.join(member);
        return queue;
    }

    private static LocalDate march(int day) {
        return LocalDate.of(2026, 3, day);
    }
}
