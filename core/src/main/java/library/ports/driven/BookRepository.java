package library.ports.driven;

import java.util.List;

import library.domain.Book;

/**
 * The books the library holds. A driven port.
 */
public interface BookRepository {

    List<Book> all();

    void save(Book book);
}
