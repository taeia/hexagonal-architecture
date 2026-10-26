package library.domain;

import java.time.LocalDate;

import library.ports.driven.Clock;
import library.ports.driven.CopyRepository;
import library.ports.driven.MemberRepository;
import library.ports.driven.QueueRepository;

/**
 * Borrowing, returning, and renewing. A domain service: each of these operations
 * crosses the boundary between aggregates, a member and a copy, or a copy and the
 * queue for its book, and no root can do that for another. The rules themselves are
 * in the roots. This class brings the roots together, asks the clock what day it is,
 * and saves the roots it changed. It is the only place in the domain that asks.
 */
public class Lending {

    private final MemberRepository members;
    private final CopyRepository copies;
    private final QueueRepository queues;
    private final Clock clock;

    public Lending(MemberRepository members, CopyRepository copies, QueueRepository queues, Clock clock) {
        this.members = members;
        this.copies = copies;
        this.queues = queues;
        this.clock = clock;
    }

    public Loan borrow(Member member, Copy copy) {
        LocalDate today = clock.today();
        boolean held = copy.isHeldFor(member);
        Loan loan = member.borrow(copy, today);
        if (held) {
            Queue queue = queues.of(copy.book());
            queue.leave(member);
            queues.save(queue);
        }
        members.save(member);
        copies.save(copy);
        return loan;
    }

    /**
     * Returns the number of days the copy was late, or 0 if it was on time.
     */
    public long returnCopy(Loan loan) {
        LocalDate today = clock.today();
        Member member = loan.member();
        Copy copy = loan.copy();
        long daysLate = member.returnCopy(loan, today);
        Queue queue = queues.of(copy.book());
        if (queue.isEmpty()) {
            copy.makeAvailable();
        } else {
            copy.holdFor(queue.first().member());
        }
        members.save(member);
        copies.save(copy);
        return daysLate;
    }

    /**
     * The queue is asked before the member, because asking the member to renew is
     * renewing: the check that is only a check has to come first.
     */
    public void renew(Loan loan) {
        Book book = loan.copy().book();
        if (!queues.of(book).isEmpty()) {
            throw new LibraryException(book.title() + " is reserved.");
        }
        Member member = loan.member();
        member.renew(loan, clock.today());
        members.save(member);
    }
}
