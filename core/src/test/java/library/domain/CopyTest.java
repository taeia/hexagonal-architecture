package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CopyTest {

    private final Member john = new Member("John");
    private final Member jane = new Member("Jane");
    private final Member mary = new Member("Mary");
    private final Copy c1 = new Copy("C1", new Book("978-0-547-92822-7", "The Hobbit"));

    @Test
    void startsAvailable() {
        assertTrue(c1.isAvailable());
        assertFalse(c1.isOnLoan());
        assertFalse(c1.isOnHold());
    }

    @Test
    void cannotBeLentTwice() {
        c1.lendTo(john);
        LibraryException refusal = assertThrows(LibraryException.class, () -> c1.lendTo(jane));
        assertEquals("C1 is on loan.", refusal.getMessage());
        assertTrue(c1.isOnLoan());
    }

    @Test
    void onHoldIsOnlyForTheMemberItIsHeldFor() {
        c1.lendTo(john);
        c1.holdFor(mary);
        assertTrue(c1.isHeldFor(mary));
        assertFalse(c1.isAvailable());

        LibraryException refusal = assertThrows(LibraryException.class, () -> c1.lendTo(jane));
        assertEquals("C1 is on hold for Mary.", refusal.getMessage());

        c1.lendTo(mary);
        assertTrue(c1.isOnLoan());
        assertFalse(c1.isOnHold());
    }

    @Test
    void becomesAvailableWhenReturnedAndNobodyIsWaiting() {
        c1.lendTo(john);
        c1.makeAvailable();
        assertTrue(c1.isAvailable());
    }

    @Test
    void onlyACopyOnLoanCanComeBack() {
        assertThrows(IllegalStateException.class, () -> c1.holdFor(mary));
        assertThrows(IllegalStateException.class, c1::makeAvailable);
    }
}
