package library.inmemory;

import java.util.ArrayList;
import java.util.List;

import library.domain.Member;
import library.ports.driven.MemberRepository;

/**
 * The members in a list. {@code save} adds a member the first time and does nothing
 * after that, because the object in the list is the one that was changed, loans
 * included. A database would write the member and its loans here.
 */
public class InMemoryMemberRepository implements MemberRepository {

    private final List<Member> members = new ArrayList<>();

    @Override
    public List<Member> all() {
        return new ArrayList<>(members);
    }

    @Override
    public void save(Member member) {
        if (!members.contains(member)) {
            members.add(member);
        }
    }

    public void clear() {
        members.clear();
    }
}
