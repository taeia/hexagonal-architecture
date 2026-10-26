package library.domain;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * The fact that a member has borrowed a copy. An entity inside the {@link Member}
 * aggregate: it lasts from the day the copy is borrowed to the day it is returned, and
 * its due date moves when it is renewed. Only its member can create, renew, or end
 * it, which is why the constructor and {@code renew} are not public. It knows whether
 * it is overdue and what the fine would be on a given day. It does not know what day
 * it is; it is told.
 */
public class Loan {

    public static final Period LOAN_PERIOD = Period.ofDays(14);

    private final Copy copy;
    private final Member member;
    private LocalDate dueDate;

    /**
     * A loan that starts on the given day. The due date is that day plus the loan period.
     */
    Loan(Copy copy, Member member, LocalDate borrowedOn) {
        this.copy = copy;
        this.member = member;
        this.dueDate = borrowedOn.plus(LOAN_PERIOD);
    }

    public Copy copy() {
        return copy;
    }

    public Member member() {
        return member;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    /**
     * A loan is overdue after its due date. On the due date it is not.
     */
    public boolean isOverdue(LocalDate today) {
        return today.isAfter(dueDate);
    }

    /**
     * The number of calendar days from the due date to the given day, or 0 if the loan
     * is not overdue on that day.
     */
    public long daysLate(LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
    }

    /**
     * The fine for returning the copy on the given day. Only an overdue loan has one.
     */
    public Fine fine(LocalDate today) {
        if (!isOverdue(today)) {
            throw new IllegalStateException("The loan is not overdue.");
        }
        return new Fine(daysLate(today));
    }

    /**
     * Moves the due date one loan period later. An overdue loan cannot be renewed,
     * because fines are charged only on return and renewing would avoid them forever.
     * The other conditions for a renewal, no debt and no reservation, involve other
     * objects and are checked by {@link Member} and {@link Lending}.
     */
    void renew(LocalDate today) {
        if (isOverdue(today)) {
            throw new LibraryException("The loan is overdue.");
        }
        dueDate = dueDate.plus(LOAN_PERIOD);
    }

    @Override
    public String toString() {
        return copy.number() + " (" + copy.book().title() + "), " + member.name()
                + ", due " + dueDate.format(DateTimeFormatter.ofPattern("d MMMM"));
    }
}
