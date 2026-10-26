package library.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import library.application.LoanService;
import library.application.MemberService;
import library.application.ReservationService;
import library.domain.Book;
import library.domain.Copy;
import library.domain.Lending;
import library.domain.LibraryException;
import library.domain.Loan;
import library.domain.Member;
import library.domain.Money;
import library.domain.Reserving;
import library.inmemory.InMemoryBookRepository;
import library.inmemory.InMemoryCopyRepository;
import library.inmemory.InMemoryMemberRepository;
import library.inmemory.InMemoryNotifier;
import library.inmemory.InMemoryQueueRepository;
import library.inmemory.SettableClock;
import library.inmemory.StartingState;
import library.ports.driving.ForLendingCopies;
import library.ports.driving.ForPayingFines;
import library.ports.driving.ForReservingBooks;

/**
 * One test per scenario of Chapter 0, with the same name. A driving adapter: the test
 * holds the three driving ports, as the window and the command line do, and its setup
 * is the configurator for this driver. It chooses the in-memory adapters and the
 * mailbox it can read back.
 */
class ScenarioTest {

    private final InMemoryBookRepository bookRepository = new InMemoryBookRepository();
    private final InMemoryCopyRepository copyRepository = new InMemoryCopyRepository();
    private final InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
    private final InMemoryQueueRepository queueRepository = new InMemoryQueueRepository();
    private final InMemoryNotifier notifier = new InMemoryNotifier();
    private final SettableClock clock = new SettableClock();
    private ForLendingCopies loans;
    private ForReservingBooks reservations;
    private ForPayingFines members;

    @BeforeEach
    void startingState() {
        StartingState.load(bookRepository, copyRepository, memberRepository, queueRepository);
        notifier.clear();
        Lending lending = new Lending(memberRepository, copyRepository, queueRepository, clock);
        Reserving reserving = new Reserving(copyRepository, queueRepository);
        loans = new LoanService(lending, memberRepository, copyRepository, notifier);
        reservations = new ReservationService(reserving, bookRepository, queueRepository);
        members = new MemberService(memberRepository);
    }

    @Test
    @DisplayName("Borrowing and returning on time")
    void borrowingAndReturningOnTime() {
        day(3, 2);
        Loan loan = loans.borrow(member("John"), copy("C2"));
        assertEquals(date(3, 16), loan.dueDate());

        day(3, 16);
        assertEquals(0, loans.returnCopy(loan));
        assertEquals(Money.ZERO, member("John").debt());
        assertTrue(copy("C2").isAvailable());
    }

    @Test
    @DisplayName("A copy cannot be lent twice")
    void aCopyCannotBeLentTwice() {
        day(3, 2);
        loans.borrow(member("John"), copy("C1"));

        day(3, 3);
        refused(() -> loans.borrow(member("Jane"), copy("C1")), "C1 is on loan.");
    }

    @Test
    @DisplayName("The loan limit")
    void theLoanLimit() {
        day(3, 2);
        loans.borrow(member("John"), copy("C1"));
        loans.borrow(member("John"), copy("C2"));
        loans.borrow(member("John"), copy("C4"));
        assertEquals(3, loans.loanCount(member("John")));
        refused(() -> loans.borrow(member("John"), copy("C5")), "John has reached the loan limit of 3.");

        day(3, 5);
        assertEquals(0, loans.returnCopy(loan("C4")));
        loans.borrow(member("John"), copy("C5"));
        assertEquals(3, loans.loanCount(member("John")));
    }

    @Test
    @DisplayName("A late return creates a fine")
    void aLateReturnCreatesAFine() {
        day(3, 2);
        Loan loan = loans.borrow(member("Jane"), copy("C3"));
        assertEquals(date(3, 16), loan.dueDate());

        day(3, 19);
        assertEquals(3, loans.returnCopy(loan));
        assertEquals(Money.of("1.50"), member("Jane").debt());
    }

    @Test
    @DisplayName("A debt blocks borrowing")
    void aDebtBlocksBorrowing() {
        day(3, 1);
        Loan loan = loans.borrow(member("John"), copy("C1"));
        assertEquals(date(3, 15), loan.dueDate());

        day(3, 18);
        assertEquals(3, loans.returnCopy(loan));
        assertEquals(Money.of("1.50"), member("John").debt());
        refused(() -> loans.borrow(member("John"), copy("C2")), "John owes 1.50 EUR.");
    }

    @Test
    @DisplayName("Fines add up")
    void finesAddUp() {
        day(3, 2);
        loans.borrow(member("Jane"), copy("C2"));
        loans.borrow(member("Jane"), copy("C4"));

        day(3, 18);
        assertEquals(2, loans.returnCopy(loan("C2")));
        assertEquals(Money.of("1.00"), member("Jane").debt());

        day(3, 21);
        assertEquals(5, loans.returnCopy(loan("C4")));
        assertEquals(Money.of("3.50"), member("Jane").debt());
    }

    @Test
    @DisplayName("Paying fines")
    void payingFines() {
        day(3, 2);
        loans.borrow(member("Jane"), copy("C3"));

        day(3, 19);
        loans.returnCopy(loan("C3"));
        assertEquals(Money.of("1.50"), member("Jane").debt());

        day(3, 20);
        assertEquals(Money.of("1.50"), members.pay(member("Jane")));
        assertEquals(Money.ZERO, member("Jane").debt());
        Loan loan = loans.borrow(member("Jane"), copy("C2"));
        assertEquals(date(4, 3), loan.dueDate());
        refused(() -> members.pay(member("Mary")), "Mary owes nothing.");
    }

    @Test
    @DisplayName("Renewing a loan")
    void renewingALoan() {
        day(3, 2);
        Loan loan = loans.borrow(member("Mary"), copy("C4"));
        assertEquals(date(3, 16), loan.dueDate());

        day(3, 10);
        loans.renew(loan);
        assertEquals(date(3, 30), loan.dueDate());

        day(3, 28);
        loans.renew(loan);
        assertEquals(date(4, 13), loan.dueDate());

        day(4, 13);
        assertEquals(0, loans.returnCopy(loan));
        assertEquals(Money.ZERO, member("Mary").debt());
    }

    @Test
    @DisplayName("A reservation blocks renewal")
    void aReservationBlocksRenewal() {
        day(3, 2);
        Loan loan = loans.borrow(member("John"), copy("C1"));

        day(3, 5);
        assertEquals(1, reservations.reserve(member("Mary"), book("The Hobbit")));

        day(3, 10);
        refused(() -> loans.renew(loan), "The Hobbit is reserved.");
    }

    @Test
    @DisplayName("An overdue loan cannot be renewed")
    void anOverdueLoanCannotBeRenewed() {
        day(3, 2);
        Loan loan = loans.borrow(member("Jane"), copy("C5"));

        day(3, 18);
        refused(() -> loans.renew(loan), "The loan is overdue.");
        assertEquals(2, loans.returnCopy(loan));
        assertEquals(Money.of("1.00"), member("Jane").debt());
    }

    @Test
    @DisplayName("A debt blocks renewal")
    void aDebtBlocksRenewal() {
        day(3, 2);
        loans.borrow(member("Jane"), copy("C2"));

        day(3, 10);
        Loan loanOfC4 = loans.borrow(member("Jane"), copy("C4"));
        assertEquals(date(3, 24), loanOfC4.dueDate());

        day(3, 18);
        loans.returnCopy(loan("C2"));
        assertEquals(Money.of("1.00"), member("Jane").debt());

        day(3, 20);
        refused(() -> loans.renew(loanOfC4), "Jane owes 1.00 EUR.");
    }

    @Test
    @DisplayName("When a book can be reserved")
    void whenABookCanBeReserved() {
        day(3, 2);
        refused(() -> reservations.reserve(member("Jane"), book("Pride and Prejudice")),
                "A copy of Pride and Prejudice is available.");
        loans.borrow(member("John"), copy("C2"));
        loans.borrow(member("Mary"), copy("C3"));

        day(3, 3);
        refused(() -> reservations.reserve(member("John"), book("Pride and Prejudice")),
                "John has a copy of Pride and Prejudice on loan.");
        assertEquals(1, reservations.reserve(member("Jane"), book("Pride and Prejudice")));

        day(3, 4);
        refused(() -> reservations.reserve(member("Jane"), book("Pride and Prejudice")),
                "Jane already has a reservation for Pride and Prejudice.");
    }

    @Test
    @DisplayName("Reserving ignores the loan limit and debt")
    void reservingIgnoresTheLoanLimitAndDebt() {
        day(3, 2);
        loans.borrow(member("John"), copy("C1"));
        loans.borrow(member("Mary"), copy("C2"));
        loans.borrow(member("Mary"), copy("C4"));
        loans.borrow(member("Mary"), copy("C5"));
        loans.borrow(member("Jane"), copy("C3"));

        day(3, 3);
        assertEquals(1, reservations.reserve(member("Mary"), book("The Hobbit")));

        day(3, 18);
        loans.returnCopy(loan("C3"));
        assertEquals(Money.of("1.00"), member("Jane").debt());
        assertEquals(2, reservations.reserve(member("Jane"), book("The Hobbit")));
    }

    @Test
    @DisplayName("A returned copy is held and the member is notified")
    void aReturnedCopyIsHeldAndTheMemberIsNotified() {
        day(3, 2);
        Loan loan = loans.borrow(member("John"), copy("C1"));

        day(3, 4);
        reservations.reserve(member("Mary"), book("The Hobbit"));

        day(3, 5);
        reservations.reserve(member("Jane"), book("The Hobbit"));

        day(3, 10);
        assertEquals(0, loans.returnCopy(loan));
        assertTrue(copy("C1").isHeldFor(member("Mary")));
        assertTrue(notifier.sent().contains("Mary: The Hobbit is waiting for you."));
    }

    @Test
    @DisplayName("A held copy is only for the member it is held for")
    void aHeldCopyIsOnlyForTheMemberItIsHeldFor() {
        day(3, 2);
        loans.borrow(member("John"), copy("C1"));

        day(3, 4);
        reservations.reserve(member("Mary"), book("The Hobbit"));

        day(3, 5);
        reservations.reserve(member("Jane"), book("The Hobbit"));

        day(3, 10);
        loans.returnCopy(loan("C1"));
        assertTrue(copy("C1").isHeldFor(member("Mary")));
        assertTrue(notifier.sent().contains("Mary: The Hobbit is waiting for you."));

        day(3, 11);
        refused(() -> loans.borrow(member("Jane"), copy("C1")), "C1 is on hold for Mary.");

        day(3, 12);
        Loan loan = loans.borrow(member("Mary"), copy("C1"));
        assertEquals(date(3, 26), loan.dueDate());
        assertFalse(copy("C1").isOnHold());
        assertEquals(1, reservations.reservations().size());
        assertSame(member("Jane"), reservations.reservations().get(0).member());
    }

    @Test
    @DisplayName("A hold waits while the member cannot borrow")
    void aHoldWaitsWhileTheMemberCannotBorrow() {
        day(3, 2);
        loans.borrow(member("Mary"), copy("C2"));

        day(3, 5);
        Loan loanOfC1 = loans.borrow(member("John"), copy("C1"));
        assertEquals(date(3, 19), loanOfC1.dueDate());

        day(3, 6);
        reservations.reserve(member("Mary"), book("The Hobbit"));

        day(3, 18);
        assertEquals(2, loans.returnCopy(loan("C2")));
        assertEquals(Money.of("1.00"), member("Mary").debt());

        day(3, 19);
        assertEquals(0, loans.returnCopy(loanOfC1));
        assertTrue(copy("C1").isHeldFor(member("Mary")));
        assertTrue(notifier.sent().contains("Mary: The Hobbit is waiting for you."));
        refused(() -> loans.borrow(member("Mary"), copy("C1")), "Mary owes 1.00 EUR.");
        assertTrue(copy("C1").isHeldFor(member("Mary")));
        refused(() -> loans.borrow(member("Jane"), copy("C1")), "C1 is on hold for Mary.");

        day(3, 20);
        assertEquals(Money.of("1.00"), members.pay(member("Mary")));
        Loan loan = loans.borrow(member("Mary"), copy("C1"));
        assertEquals(date(4, 3), loan.dueDate());
    }

    @Test
    @DisplayName("A month at the library")
    void aMonthAtTheLibrary() {
        day(3, 2);
        Loan johnsLoan = loans.borrow(member("John"), copy("C1"));
        Loan janesLoan = loans.borrow(member("Jane"), copy("C2"));
        assertEquals(date(3, 16), johnsLoan.dueDate());
        assertEquals(date(3, 16), janesLoan.dueDate());

        day(3, 4);
        refused(() -> loans.borrow(member("Mary"), copy("C1")), "C1 is on loan.");
        assertEquals(1, reservations.reserve(member("Mary"), book("The Hobbit")));

        day(3, 10);
        refused(() -> loans.renew(johnsLoan), "The Hobbit is reserved.");

        day(3, 12);
        loans.renew(janesLoan);
        assertEquals(date(3, 30), janesLoan.dueDate());

        day(3, 16);
        assertEquals(0, loans.returnCopy(johnsLoan));
        assertTrue(copy("C1").isHeldFor(member("Mary")));
        assertTrue(notifier.sent().contains("Mary: The Hobbit is waiting for you."));

        day(3, 17);
        refused(() -> loans.borrow(member("Jane"), copy("C1")), "C1 is on hold for Mary.");
        Loan marysLoan = loans.borrow(member("Mary"), copy("C1"));
        assertEquals(date(3, 31), marysLoan.dueDate());
        assertTrue(reservations.reservations().isEmpty());

        day(4, 2);
        assertEquals(3, loans.returnCopy(janesLoan));
        assertEquals(Money.of("1.50"), member("Jane").debt());
        refused(() -> loans.borrow(member("Jane"), copy("C4")), "Jane owes 1.50 EUR.");

        day(4, 3);
        assertEquals(Money.of("1.50"), members.pay(member("Jane")));
        Loan loan = loans.borrow(member("Jane"), copy("C4"));
        assertEquals(date(4, 17), loan.dueDate());
        assertEquals(3, loans.returnCopy(marysLoan));
        assertEquals(Money.of("1.50"), member("Mary").debt());
    }

    // ---- helpers ----

    private void day(int month, int day) {
        clock.set(date(month, day));
    }

    private LocalDate date(int month, int day) {
        return LocalDate.of(2026, month, day);
    }

    private void refused(Executable operation, String reason) {
        LibraryException exception = assertThrows(LibraryException.class, operation);
        assertEquals(reason, exception.getMessage());
    }

    private Member member(String name) {
        for (Member member : memberRepository.all()) {
            if (member.name().equals(name)) {
                return member;
            }
        }
        throw new IllegalArgumentException(name);
    }

    private Copy copy(String number) {
        for (Copy copy : copyRepository.all()) {
            if (copy.number().equals(number)) {
                return copy;
            }
        }
        throw new IllegalArgumentException(number);
    }

    private Book book(String title) {
        for (Book book : bookRepository.all()) {
            if (book.title().equals(title)) {
                return book;
            }
        }
        throw new IllegalArgumentException(title);
    }

    private Loan loan(String copyNumber) {
        return loans.loanOf(copy(copyNumber));
    }
}
