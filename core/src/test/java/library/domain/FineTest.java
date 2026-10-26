package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FineTest {

    @Test
    void threeDaysLateCostOneEuroFifty() {
        assertEquals(Money.of("1.50"), new Fine(3).amount());
    }

    @Test
    void oneDayLateCostsFiftyCents() {
        assertEquals(Money.of("0.50"), new Fine(1).amount());
    }

    @Test
    void needsAtLeastOneDayOfDelay() {
        assertThrows(IllegalArgumentException.class, () -> new Fine(0));
    }
}
