package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The loan limit and the debt rules, tested on a member alone. No service, no
 * repository, no clock: the member holds its loans, so it can count them, and every
 * operation is told the day it happens on.
 */
class MemberTest {

    private final Book hobbit = new Book("978-0-547-92822-7", "The Hobbit");
    private final Book pride = new Book("978-0-14-143951-8", "Pride and Prejudice");
    private final Book orwell = new Book("978-0-451-52493-5", "Nineteen Eighty-Four");
    private final Copy c1 = new Copy("C1", hobbit);
    private final Copy c2 = new Copy("C2", pride);
    private final Copy c3 = new Copy("C3", pride);
    private final Copy c4 = new Copy("C4", orwell);
    private final Member john = new Member("John");
    private final Member jane = new Member("Jane");

    @Test
    void startsWithNoLoansAndNoDebt() {
        assertTrue(john.loans().isEmpty());
        assertEquals(Money.ZERO, john.debt());
        assertFalse(john.owesAnything());
    }

    @Test
    void borrowingLendsTheCopyAndStartsALoan() {
        Loan loan = john.borrow(c1, march(2));

        assertTrue(c1.isOnLoan());
        assertEquals(march(16), loan.dueDate());
        assertEquals(List.of(loan), john.loans());
        assertSame(loan, john.loanOf(c1));
        assertTrue(john.hasOnLoan(hobbit));
        assertFalse(john.hasOnLoan(pride));
    }

    @Test
    void theLoanLimitIsThreeCopies() {
        john.borrow(c1, march(2));
        john.borrow(c2, march(2));
        john.borrow(c3, march(2));

        LibraryException refusal = assertThrows(LibraryException.class, () -> john.borrow(c4, march(2)));
        assertEquals("John has reached the loan limit of 3.", refusal.getMessage());
        assertTrue(c4.isAvailable());
        assertEquals(3, john.loans().size());
    }

    @Test
    void aDebtBlocksBorrowing() {
        Loan loan = john.borrow(c1, march(2));
        john.returnCopy(loan, march(19));

        LibraryException refusal = assertThrows(LibraryException.class, () -> john.borrow(c2, march(19)));
        assertEquals("John owes 1.50 EUR.", refusal.getMessage());
        assertTrue(c2.isAvailable());
    }

    @Test
    void aReturnOnTimeCostsNothingAndEndsTheLoan() {
        Loan loan = john.borrow(c1, march(2));

        assertEquals(0, john.returnCopy(loan, march(16)));
        assertEquals(Money.ZERO, john.debt());
        assertTrue(john.loans().isEmpty());
    }

    @Test
    void aLateReturnIsChargedToTheDebt() {
        Loan loan = john.borrow(c1, march(2));

        assertEquals(3, john.returnCopy(loan, march(19)));
        assertEquals(Money.of("1.50"), john.debt());
        assertTrue(john.loans().isEmpty());
    }

    @Test
    void finesAddUp() {
        Loan loanOfC2 = john.borrow(c2, march(2));
        Loan loanOfC4 = john.borrow(c4, march(2));

        john.returnCopy(loanOfC2, march(18));
        assertEquals(Money.of("1.00"), john.debt());

        john.returnCopy(loanOfC4, march(21));
        assertEquals(Money.of("3.50"), john.debt());
    }

    @Test
    void payingClearsTheWholeDebt() {
        Loan loan = john.borrow(c1, march(2));
        john.returnCopy(loan, march(19));

        assertEquals(Money.of("1.50"), john.pay());
        assertEquals(Money.ZERO, john.debt());
    }

    @Test
    void cannotPayWhenOwingNothing() {
        LibraryException refusal = assertThrows(LibraryException.class, john::pay);
        assertEquals("John owes nothing.", refusal.getMessage());
    }

    @Test
    void renewalMovesTheDueDateFourteenDaysLater() {
        Loan loan = john.borrow(c1, march(2));

        john.renew(loan, march(10));

        assertEquals(march(30), loan.dueDate());
    }

    @Test
    void aDebtBlocksRenewal() {
        Loan loanOfC2 = john.borrow(c2, march(2));
        Loan loanOfC4 = john.borrow(c4, march(10));
        john.returnCopy(loanOfC2, march(18));

        LibraryException refusal = assertThrows(LibraryException.class, () -> john.renew(loanOfC4, march(20)));
        assertEquals("John owes 1.00 EUR.", refusal.getMessage());
        assertEquals(march(24), loanOfC4.dueDate());
    }

    @Test
    void cannotReturnOrRenewAnotherMembersLoan() {
        Loan janes = jane.borrow(c1, march(2));

        assertThrows(IllegalArgumentException.class, () -> john.returnCopy(janes, march(2)));
        assertThrows(IllegalArgumentException.class, () -> john.renew(janes, march(2)));
        assertEquals(List.of(janes), jane.loans());
    }

    private static LocalDate march(int day) {
        return LocalDate.of(2026, 3, day);
    }
}
