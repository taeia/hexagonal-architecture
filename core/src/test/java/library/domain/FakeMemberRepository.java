package library.domain;

import java.util.ArrayList;
import java.util.List;

import library.ports.driven.MemberRepository;

/**
 * A member repository for the tests of this module. The domain declares the interface,
 * so a test can implement it in a few lines without seeing the infrastructure module.
 * It holds the members it was given, and separately remembers what was saved, so a
 * test can check that a changed root was.
 */
class FakeMemberRepository implements MemberRepository {

    private final List<Member> members = new ArrayList<>();
    final List<Member> saved = new ArrayList<>();

    FakeMemberRepository(Member... members) {
        this.members.addAll(List.of(members));
    }

    @Override
    public List<Member> all() {
        return List.copyOf(members);
    }

    @Override
    public void save(Member member) {
        saved.add(member);
    }
}
