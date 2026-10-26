package library.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import library.application.LoanService;
import library.application.MemberService;
import library.application.ReservationService;
import library.domain.Book;
import library.domain.Copy;
import library.domain.Lending;
import library.domain.LibraryException;
import library.domain.Loan;
import library.domain.Member;
import library.domain.Reservation;
import library.domain.Reserving;
import library.inmemory.InMemoryBookRepository;
import library.inmemory.InMemoryCopyRepository;
import library.inmemory.InMemoryMemberRepository;
import library.inmemory.InMemoryQueueRepository;
import library.inmemory.SettableClock;
import library.inmemory.StartingState;
import library.ports.driving.ForLendingCopies;
import library.ports.driving.ForPayingFines;
import library.ports.driving.ForReservingBooks;

/**
 * The library on the command line. A driving adapter: it reads commands written in the
 * words of Chapter 0, calls the three driving ports, and prints what happened. It holds
 * the ports and nothing behind them, and it names a class from the application package
 * only in {@link #main}, which is the configurator for this program.
 *
 * Commands are one per line. {@code day 2 March} moves the clock. {@code borrow John C1},
 * {@code return C1}, {@code renew C1}, {@code reserve Mary The Hobbit}, and
 * {@code pay Jane} are the five operations. {@code copies}, {@code members}, and
 * {@code reservations} show the library, {@code reset} puts it back in its starting
 * state, and {@code quit} ends the session.
 */
public class Cli {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_WITH_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final ForLendingCopies lending;
    private final ForReservingBooks reserving;
    private final ForPayingFines paying;
    private final SettableClock clock;
    private final Runnable reset;
    private final PrintStream out;

    public Cli(ForLendingCopies lending, ForReservingBooks reserving, ForPayingFines paying,
               SettableClock clock, Runnable reset, PrintStream out) {
        this.lending = lending;
        this.reserving = reserving;
        this.paying = paying;
        this.clock = clock;
        this.reset = reset;
        this.out = out;
    }

    /**
     * Reads commands from a script file if one is given, otherwise from the terminal.
     */
    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ENGLISH);

        // The configurator for the command line: the same core as the window, the same
        // in-memory storage and clock, and a different mailbox. The window keeps the
        // messages in a list it shows; the command line prints them as they are sent.
        // This is the whole difference between the two programs, and the core cannot
        // see it.
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryCopyRepository copies = new InMemoryCopyRepository();
        InMemoryMemberRepository members = new InMemoryMemberRepository();
        InMemoryQueueRepository queues = new InMemoryQueueRepository();
        SettableClock clock = new SettableClock();
        ConsoleNotifier notifier = new ConsoleNotifier(System.out);
        Lending lending = new Lending(members, copies, queues, clock);
        Reserving reserving = new Reserving(copies, queues);
        Runnable reset = () -> StartingState.load(books, copies, members, queues);
        reset.run();
        Cli cli = new Cli(new LoanService(lending, members, copies, notifier),
                new ReservationService(reserving, books, queues),
                new MemberService(members),
                clock, reset, System.out);

        if (args.length > 0) {
            try (BufferedReader script = Files.newBufferedReader(Path.of(args[0]))) {
                cli.run(script, true);
            }
        } else {
            cli.run(new BufferedReader(new InputStreamReader(System.in)), false);
        }
    }

    /**
     * Runs commands until the input ends or a {@code quit}. When reading a script, each
     * command is echoed behind a prompt, so that the output reads like a session.
     */
    public void run(BufferedReader in, boolean echo) throws IOException {
        if (!echo) {
            out.print("> ");
        }
        String line;
        while ((line = in.readLine()) != null) {
            line = line.strip();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (echo) {
                    out.println("> " + line);
                }
                if (line.equals("quit")) {
                    return;
                }
                out.println(execute(line));
            }
            if (!echo) {
                out.print("> ");
            }
        }
    }

    /**
     * Executes one command and returns the sentence to print.
     */
    public String execute(String line) {
        String[] words = line.strip().split("\\s+", 2);
        String command = words[0];
        String rest = words.length > 1 ? words[1].strip() : "";
        try {
            return switch (command) {
                case "day" -> day(rest);
                case "borrow" -> borrow(rest);
                case "return" -> returnCopy(rest);
                case "renew" -> renew(rest);
                case "reserve" -> reserve(rest);
                case "pay" -> pay(rest);
                case "copies" -> copies();
                case "members" -> members();
                case "reservations" -> reservations();
                case "reset" -> reset();
                case "help" -> help();
                default -> "Unknown command: " + command + ". Type help.";
            };
        } catch (LibraryException e) {
            return "The library refuses. " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    // ---- the clock ----

    private String day(String text) {
        try {
            clock.set(LocalDate.parse(text + " 2026", DAY_WITH_YEAR));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Write the day as in \"day 2 March\".");
        }
        return "It is " + clock.today().format(DAY) + ".";
    }

    // ---- the five operations ----

    private String borrow(String rest) {
        String[] words = twoWords(rest, "borrow John C1");
        Member member = member(words[0]);
        Copy copy = copy(words[1]);
        Loan loan = lending.borrow(member, copy);
        return member.name() + " borrowed " + copy.number() + " of " + copy.book().title()
                + ". It is due on " + loan.dueDate().format(DAY) + ".";
    }

    private String returnCopy(String rest) {
        Loan loan = loanOf(copy(oneWord(rest, "return C1")));
        long daysLate = lending.returnCopy(loan);
        Copy copy = loan.copy();
        String name = loan.member().name();
        String result;
        if (daysLate > 0) {
            result = name + " returned " + copy.number() + " " + daysLate + " days late and now owes "
                    + loan.member().debt() + ".";
        } else {
            result = name + " returned " + copy.number() + " on time.";
        }
        if (copy.isOnHold()) {
            result += " " + copy.number() + " is on hold for " + copy.heldFor().name() + ", who has been notified.";
        }
        return result;
    }

    private String renew(String rest) {
        Loan loan = loanOf(copy(oneWord(rest, "renew C1")));
        lending.renew(loan);
        return loan.member().name() + " renewed the loan of " + loan.copy().number()
                + ". It is now due on " + loan.dueDate().format(DAY) + ".";
    }

    private String reserve(String rest) {
        String[] words = twoWords(rest, "reserve Mary The Hobbit");
        Member member = member(words[0]);
        Book book = book(words[1]);
        int position = reserving.reserve(member, book);
        return member.name() + " reserved " + book.title() + ". Position in the queue: " + position + ".";
    }

    private String pay(String rest) {
        Member member = member(oneWord(rest, "pay Jane"));
        return member.name() + " paid " + paying.pay(member) + " and owes nothing now.";
    }

    // ---- the library as it stands ----

    private String copies() {
        StringBuilder result = new StringBuilder();
        for (Copy copy : lending.copies()) {
            result.append(copy.number()).append("  ").append(copy.book().title()).append(": ");
            if (copy.isOnHold()) {
                result.append("on hold for ").append(copy.heldFor().name());
            } else if (copy.isOnLoan()) {
                Loan loan = lending.loanOf(copy);
                result.append("on loan to ").append(loan.member().name())
                        .append(", due ").append(loan.dueDate().format(DAY));
            } else {
                result.append("available");
            }
            result.append('\n');
        }
        return result.toString().stripTrailing();
    }

    private String members() {
        StringBuilder result = new StringBuilder();
        for (Member member : paying.members()) {
            int loans = lending.loanCount(member);
            result.append(member.name()).append(": ").append(loans).append(loans == 1 ? " loan" : " loans")
                    .append(", owes ").append(member.debt()).append('\n');
        }
        return result.toString().stripTrailing();
    }

    private String reservations() {
        if (reserving.reservations().isEmpty()) {
            return "No reservations.";
        }
        StringBuilder result = new StringBuilder();
        for (Reservation reservation : reserving.reservations()) {
            result.append(reservation.book().title()).append(": ").append(reservation.member().name())
                    .append(", position ").append(reserving.queuePosition(reservation)).append('\n');
        }
        return result.toString().stripTrailing();
    }

    private String reset() {
        reset.run();
        return "The library is back in its starting state.";
    }

    private String help() {
        return """
                day 2 March              set the date
                borrow John C1           a member borrows a copy
                return C1                a copy comes back
                renew C1                 the loan of a copy is renewed
                reserve Mary The Hobbit  a member reserves a book
                pay Jane                 a member pays what they owe
                copies, members, reservations
                reset, help, quit""";
    }

    // ---- finding things by the names Chapter 0 uses ----

    private Member member(String name) {
        for (Member member : paying.members()) {
            if (member.name().equals(name)) {
                return member;
            }
        }
        throw new IllegalArgumentException("There is no member called " + name + ".");
    }

    private Copy copy(String number) {
        for (Copy copy : lending.copies()) {
            if (copy.number().equals(number)) {
                return copy;
            }
        }
        throw new IllegalArgumentException("There is no copy " + number + ".");
    }

    private Book book(String title) {
        for (Book book : reserving.books()) {
            if (book.title().equals(title)) {
                return book;
            }
        }
        throw new IllegalArgumentException("There is no book called " + title + ".");
    }

    private Loan loanOf(Copy copy) {
        Loan loan = lending.loanOf(copy);
        if (loan == null) {
            throw new IllegalArgumentException(copy.number() + " is not on loan.");
        }
        return loan;
    }

    private static String oneWord(String rest, String example) {
        if (rest.isEmpty() || rest.contains(" ")) {
            throw new IllegalArgumentException("Write it as in \"" + example + "\".");
        }
        return rest;
    }

    private static String[] twoWords(String rest, String example) {
        String[] words = rest.split("\\s+", 2);
        if (words.length < 2) {
            throw new IllegalArgumentException("Write it as in \"" + example + "\".");
        }
        return words;
    }
}
