package library.inmemory;

import java.util.ArrayList;
import java.util.List;

import library.domain.Book;
import library.ports.driven.BookRepository;

public class InMemoryBookRepository implements BookRepository {

    private final List<Book> books = new ArrayList<>();

    @Override
    public List<Book> all() {
        return new ArrayList<>(books);
    }

    @Override
    public void save(Book book) {
        if (!books.contains(book)) {
            books.add(book);
        }
    }

    public void clear() {
        books.clear();
    }
}
