package library.application;

import java.util.ArrayList;
import java.util.List;

import library.domain.Copy;
import library.domain.Lending;
import library.domain.Loan;
import library.domain.Member;
import library.ports.driven.CopyRepository;
import library.ports.driven.MemberRepository;
import library.ports.driven.Notifier;
import library.ports.driving.ForLendingCopies;

/**
 * Borrowing, returning, and renewing. An application service: it asks {@link Lending}
 * to do the work and sends the notification through the {@link Notifier} the domain
 * declares. The rules are in the aggregates, and the saving is done by the domain
 * service that changed them. Also the questions the screens ask about loans and copies.
 */
public class LoanService implements ForLendingCopies {

    private final Lending lending;
    private final MemberRepository members;
    private final CopyRepository copies;
    private final Notifier notifier;

    public LoanService(Lending lending, MemberRepository members, CopyRepository copies, Notifier notifier) {
        this.lending = lending;
        this.members = members;
        this.copies = copies;
        this.notifier = notifier;
    }

    // ---- operations ----

    public Loan borrow(Member member, Copy copy) {
        return lending.borrow(member, copy);
    }

    /**
     * Returns the number of days the copy was late, or 0 if it was on time.
     */
    public long returnCopy(Loan loan) {
        long daysLate = lending.returnCopy(loan);
        Copy copy = loan.copy();
        if (copy.isOnHold()) {
            notifier.send(copy.heldFor(), copy.book().title() + " is waiting for you.");
        }
        return daysLate;
    }

    public void renew(Loan loan) {
        lending.renew(loan);
    }

    // ---- questions the screens ask ----

    public List<Copy> copies() {
        return copies.all();
    }

    public List<Loan> loans() {
        List<Loan> result = new ArrayList<>();
        for (Member member : members.all()) {
            result.addAll(member.loans());
        }
        return result;
    }

    /**
     * The loan of the copy, or null if it is not on loan. Loans live inside members, so
     * the question is put to each member in turn.
     */
    public Loan loanOf(Copy copy) {
        for (Member member : members.all()) {
            Loan loan = member.loanOf(copy);
            if (loan != null) {
                return loan;
            }
        }
        return null;
    }

    public int loanCount(Member member) {
        return member.loans().size();
    }
}
