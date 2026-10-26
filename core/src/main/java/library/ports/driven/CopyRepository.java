package library.ports.driven;

import java.util.List;

import library.domain.Book;
import library.domain.Copy;

/**
 * The copies the library owns. A driven port.
 */
public interface CopyRepository {

    List<Copy> all();

    List<Copy> ofBook(Book book);

    void save(Copy copy);
}
