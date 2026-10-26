package library.domain;

/**
 * A member's request to borrow a book when no copy of it is available. An entity
 * inside the {@link Queue} aggregate: it has a place in the queue, and it ends when
 * the member borrows the held copy. Only the queue creates one, which is why the
 * constructor is not public.
 */
public class Reservation {

    private final Book book;
    private final Member member;

    Reservation(Book book, Member member) {
        this.book = book;
        this.member = member;
    }

    public Book book() {
        return book;
    }

    public Member member() {
        return member;
    }
}
