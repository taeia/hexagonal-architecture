package library.inmemory;

import library.domain.Book;
import library.domain.Copy;
import library.domain.Member;

/**
 * The starting state of Chapter 0. In a real system this would be a SQL script. It
 * takes the in-memory repositories by their concrete types, because clearing a store is
 * not something the core asks for. It does not touch the mailbox: each driver chooses
 * its own notifier, and the window clears the one it shows.
 */
public class StartingState {

    public static void load(InMemoryBookRepository books, InMemoryCopyRepository copies,
                            InMemoryMemberRepository members, InMemoryQueueRepository queues) {
        books.clear();
        copies.clear();
        members.clear();
        queues.clear();

        members.save(new Member("John"));
        members.save(new Member("Jane"));
        members.save(new Member("Mary"));

        Book hobbit = new Book("978-0-547-92822-7", "The Hobbit");
        Book pride = new Book("978-0-14-143951-8", "Pride and Prejudice");
        Book orwell = new Book("978-0-451-52493-5", "Nineteen Eighty-Four");
        books.save(hobbit);
        books.save(pride);
        books.save(orwell);

        copies.save(new Copy("C1", hobbit));
        copies.save(new Copy("C2", pride));
        copies.save(new Copy("C3", pride));
        copies.save(new Copy("C4", orwell));
        copies.save(new Copy("C5", orwell));
    }
}
