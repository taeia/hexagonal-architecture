package library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void cannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("-0.50"));
    }

    @Test
    void hasWholeCents() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("0.005"));
    }

    @Test
    void addsUp() {
        assertEquals(Money.of("3.50"), Money.of("1.00").plus(Money.of("2.50")));
    }

    @Test
    void multiplies() {
        assertEquals(Money.of("1.50"), Money.of("0.50").times(3));
    }

    @Test
    void isTheSameMoneyWhateverTheSpelling() {
        assertEquals(Money.of("1.5"), Money.of("1.50"));
        assertTrue(Money.of("0").isZero());
    }

    @Test
    void isShownWithTwoDecimalsAndTheCurrency() {
        assertEquals("1.50 EUR", Money.of("1.5").toString());
        assertEquals("0.00 EUR", Money.ZERO.toString());
    }
}
