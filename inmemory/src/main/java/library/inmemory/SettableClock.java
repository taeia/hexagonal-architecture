package library.inmemory;

import java.time.LocalDate;

import library.ports.driven.Clock;

/**
 * A clock that is set by hand. The date picker in the window sets it, so does the day
 * command on the command line, and so do the scenario tests, so that the scenarios of
 * Chapter 0 can be walked through day by day. It starts on 1 March 2026, where the
 * scenarios begin. A library that ran for real would wire a clock that reads the system
 * date instead, and nothing in the core would notice.
 */
public class SettableClock implements Clock {

    private LocalDate today = LocalDate.of(2026, 3, 1);

    @Override
    public LocalDate today() {
        return today;
    }

    public void set(LocalDate date) {
        today = date;
    }
}
