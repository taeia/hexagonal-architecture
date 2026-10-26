package library.application;

import java.util.List;

import library.domain.Member;
import library.domain.Money;
import library.ports.driven.MemberRepository;
import library.ports.driving.ForPayingFines;

/**
 * Paying fines. An application service, and the class behind the {@link ForPayingFines}
 * port: the member knows how to pay, and paying touches one aggregate, so no domain
 * service is needed; this class saves the result. Also the list of members, which every
 * driver asks for.
 */
public class MemberService implements ForPayingFines {

    private final MemberRepository members;

    public MemberService(MemberRepository members) {
        this.members = members;
    }

    // ---- operations ----

    /**
     * Returns the amount paid.
     */
    public Money pay(Member member) {
        Money paid = member.pay();
        members.save(member);
        return paid;
    }

    // ---- questions the screens ask ----

    public List<Member> members() {
        return members.all();
    }
}
