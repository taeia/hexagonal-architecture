package library.ports.driven;

import java.time.LocalDate;

/**
 * The library's idea of today. A driven port: the core asks, and whoever wired the
 * core decides who answers, a date picker, a test, or the operating system.
 */
public interface Clock {

    LocalDate today();
}
