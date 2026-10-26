package library.domain;

/**
 * Thrown when the library refuses an operation. The message says why, in the words of
 * Chapter 0. The refusals come from the model now, so the exception lives with it.
 */
public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }
}
