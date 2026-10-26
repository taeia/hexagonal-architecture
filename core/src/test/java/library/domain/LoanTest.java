package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * The due date, lateness, and the fine, tested on a loan alone. No clock and no setup:
 * each test says which day it is asking about.
 */
class LoanTest {

    private final Member john = new Member("John");
    private final Copy c1 = new Copy("C1", new Book("978-0-547-92822-7", "The Hobbit"));
    private final Loan loan = john.borrow(c1, march(2));

    @Test
    void isDueFourteenDaysAfterBorrowing() {
        assertEquals(march(16), loan.dueDate());
    }

    @Test
    void isNotOverdueOnTheDueDate() {
        assertFalse(loan.isOverdue(march(16)));
        assertEquals(0, loan.daysLate(march(16)));
    }

    @Test
    void isOverdueTheDayAfterTheDueDate() {
        assertTrue(loan.isOverdue(march(17)));
        assertEquals(1, loan.daysLate(march(17)));
    }

    @Test
    void threeDaysLateIsAFineOfOneEuroFifty() {
        assertEquals(3, loan.daysLate(march(19)));
        assertEquals(Money.of("1.50"), loan.fine(march(19)).amount());
    }

    @Test
    void hasNoFineWhileItIsNotOverdue() {
        assertThrows(IllegalStateException.class, () -> loan.fine(march(16)));
    }

    @Test
    void renewalMovesTheDueDateFourteenDaysLater() {
        loan.renew(march(10));
        assertEquals(march(30), loan.dueDate());
    }

    @Test
    void cannotBeRenewedWhenOverdue() {
        LibraryException refusal = assertThrows(LibraryException.class, () -> loan.renew(march(18)));
        assertEquals("The loan is overdue.", refusal.getMessage());
        assertEquals(march(16), loan.dueDate());
    }

    private static LocalDate march(int day) {
        return LocalDate.of(2026, 3, day);
    }
}
