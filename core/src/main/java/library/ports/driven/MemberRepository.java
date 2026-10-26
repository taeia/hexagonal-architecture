package library.ports.driven;

import java.util.List;

import library.domain.Member;

/**
 * The members of the library, with their loans. A driven port: the core says what it
 * needs from storage, in its own terms, and an adapter outside the core provides it.
 * Evans calls this a repository, and it is one; the hexagon calls it a port, because
 * from the hexagon's edge a repository, a clock, and a mailbox are the same kind of
 * thing. A member is stored whole, loans included.
 */
public interface MemberRepository {

    List<Member> all();

    void save(Member member);
}
