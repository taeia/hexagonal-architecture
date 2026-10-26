package library.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person registered with the library, with their debt and their loans. An aggregate
 * root: a loan exists only inside the member who holds it, and the member is the only
 * thing that starts, renews, or ends one. The loan limit and the debt rules are
 * enforced here, because everything they need is here. The one thing a member does
 * not have is the date, so every operation that depends on it is told the day.
 */
public class Member {

    public static final int LOAN_LIMIT = 3;

    private final String name;
    private Money debt = Money.ZERO;
    private final List<Loan> loans = new ArrayList<>();

    public Member(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Money debt() {
        return debt;
    }

    public boolean owesAnything() {
        return !debt.isZero();
    }

    /**
     * The member's current loans, in the order they were made. The list is a copy:
     * loans are started and ended through the member, not through this list.
     */
    public List<Loan> loans() {
        return List.copyOf(loans);
    }

    /**
     * The member's loan of the copy, or null if the member does not have it.
     */
    public Loan loanOf(Copy copy) {
        for (Loan loan : loans) {
            if (loan.copy() == copy) {
                return loan;
            }
        }
        return null;
    }

    public boolean hasOnLoan(Book book) {
        for (Loan loan : loans) {
            if (loan.copy().book() == book) {
                return true;
            }
        }
        return false;
    }

    /**
     * Borrows the copy on the given day. The member must be under the loan limit and
     * owe nothing, and the copy must agree to be lent. The copy is asked last, because
     * asking it is lending it, and by then nothing else can refuse.
     */
    public Loan borrow(Copy copy, LocalDate today) {
        if (loans.size() >= LOAN_LIMIT) {
            throw new LibraryException(name + " has reached the loan limit of " + LOAN_LIMIT + ".");
        }
        requireNoDebt();
        copy.lendTo(this);
        Loan loan = new Loan(copy, this, today);
        loans.add(loan);
        return loan;
    }

    /**
     * The copy comes back on the given day. A late return is charged to the debt, and
     * the loan ends. Returns the number of days the copy was late, or 0 if it was on
     * time. What happens to the copy next depends on the queue, and {@link Lending}
     * decides it.
     */
    public long returnCopy(Loan loan, LocalDate today) {
        requireOwn(loan);
        if (loan.isOverdue(today)) {
            charge(loan.fine(today));
        }
        loans.remove(loan);
        return loan.daysLate(today);
    }

    /**
     * Renews one of the member's loans on the given day. The member must owe nothing,
     * and the loan must not be overdue, which the loan checks itself. Whether the book
     * is reserved involves the queue, and {@link Lending} checks it.
     */
    public void renew(Loan loan, LocalDate today) {
        requireOwn(loan);
        requireNoDebt();
        loan.renew(today);
    }

    /**
     * Clears the whole debt and returns the amount paid. There are no partial payments,
     * and a member who owes nothing cannot pay.
     */
    public Money pay() {
        if (!owesAnything()) {
            throw new LibraryException(name + " owes nothing.");
        }
        Money paid = debt;
        debt = Money.ZERO;
        return paid;
    }

    /**
     * Fines from several late returns add up. Only a return can charge a member.
     */
    private void charge(Fine fine) {
        debt = debt.plus(fine.amount());
    }

    private void requireNoDebt() {
        if (owesAnything()) {
            throw new LibraryException(name + " owes " + debt + ".");
        }
    }

    private void requireOwn(Loan loan) {
        if (!loans.contains(loan)) {
            throw new IllegalArgumentException(name + " does not hold " + loan + ".");
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
