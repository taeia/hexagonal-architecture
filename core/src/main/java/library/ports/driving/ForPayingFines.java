package library.ports.driving;

import java.util.List;

import library.domain.Member;
import library.domain.Money;

/**
 * The conversation about money: pay what is owed, and the list of members, which every
 * driver needs before it can name one. A driving port.
 */
public interface ForPayingFines {

    // ---- operations ----

    /**
     * Returns the amount paid.
     */
    Money pay(Member member);

    // ---- questions ----

    List<Member> members();
}
