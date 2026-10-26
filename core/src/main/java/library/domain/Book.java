package library.domain;

/**
 * A title the library holds. An aggregate root with nothing inside it but itself,
 * identified by its ISBN, although nothing about it changes in this book until
 * Chapter 10.
 */
public class Book {

    private final String isbn;
    private final String title;

    public Book(String isbn, String title) {
        this.isbn = isbn;
        this.title = title;
    }

    public String isbn() {
        return isbn;
    }

    public String title() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
