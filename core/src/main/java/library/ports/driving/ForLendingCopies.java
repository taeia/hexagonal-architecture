package library.ports.driving;

import java.util.List;

import library.domain.Copy;
import library.domain.Loan;
import library.domain.Member;

/**
 * The conversation the outside world has with the library about lending: borrow a copy,
 * return it, renew the loan, and the questions a screen asks before and after. A
 * driving port, named the way Cockburn names ports, for the purpose of the conversation.
 * The window, the command line, and the scenario tests all hold one of these and never
 * see the class behind it.
 */
public interface ForLendingCopies {

    // ---- operations ----

    Loan borrow(Member member, Copy copy);

    /**
     * Returns the number of days the copy was late, or 0 if it was on time.
     */
    long returnCopy(Loan loan);

    void renew(Loan loan);

    // ---- questions ----

    List<Copy> copies();

    List<Loan> loans();

    /**
     * The loan of the copy, or null if it is not on loan.
     */
    Loan loanOf(Copy copy);

    int loanCount(Member member);
}
