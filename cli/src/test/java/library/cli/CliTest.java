package library.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import library.application.LoanService;
import library.application.MemberService;
import library.application.ReservationService;
import library.domain.Lending;
import library.domain.Reserving;
import library.inmemory.InMemoryBookRepository;
import library.inmemory.InMemoryCopyRepository;
import library.inmemory.InMemoryMemberRepository;
import library.inmemory.InMemoryQueueRepository;
import library.inmemory.SettableClock;
import library.inmemory.StartingState;

/**
 * Tests of the command-line adapter: what it prints for what it is told. The library
 * behind it is the real core with the in-memory adapters, wired the way {@code main}
 * wires them, with the console replaced by a buffer the test can read.
 */
class CliTest {

    private final ByteArrayOutputStream console = new ByteArrayOutputStream();
    private final Cli cli = cli(new PrintStream(console, true, StandardCharsets.UTF_8));

    private static Cli cli(PrintStream out) {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryCopyRepository copies = new InMemoryCopyRepository();
        InMemoryMemberRepository members = new InMemoryMemberRepository();
        InMemoryQueueRepository queues = new InMemoryQueueRepository();
        SettableClock clock = new SettableClock();
        Lending lending = new Lending(members, copies, queues, clock);
        Reserving reserving = new Reserving(copies, queues);
        Runnable reset = () -> StartingState.load(books, copies, members, queues);
        reset.run();
        return new Cli(new LoanService(lending, members, copies, new ConsoleNotifier(out)),
                new ReservationService(reserving, books, queues),
                new MemberService(members),
                clock, reset, out);
    }

    @Test
    void borrowingSaysWhenTheCopyIsDue() {
        cli.execute("day 2 March");
        assertEquals("John borrowed C1 of The Hobbit. It is due on 16 March.", cli.execute("borrow John C1"));
    }

    @Test
    void aRefusalIsReportedInTheLibrarysWords() {
        cli.execute("day 2 March");
        cli.execute("borrow John C1");
        assertEquals("The library refuses. C1 is on loan.", cli.execute("borrow Jane C1"));
    }

    @Test
    void theNotificationIsPrintedWhenACopyIsHeld() {
        cli.execute("day 2 March");
        cli.execute("borrow John C1");
        cli.execute("reserve Mary The Hobbit");
        cli.execute("day 10 March");
        String result = cli.execute("return C1");

        assertEquals("John returned C1 on time. C1 is on hold for Mary, who has been notified.", result);
        assertEquals("Notification for Mary: The Hobbit is waiting for you.", printed().get(0));
    }

    @Test
    void theDayMovesTheClock() {
        assertEquals("It is 18 March.", cli.execute("day 18 march"));
        assertEquals("Jane borrowed C3 of Pride and Prejudice. It is due on 1 April.", cli.execute("borrow Jane C3"));
    }

    @Test
    void quitSaysGoodbyeAndStopsReading() throws IOException {
        cli.run(new BufferedReader(new StringReader("day 2 March\nquit\nborrow John C1\n")), false);
        assertEquals(List.of("> It is 2 March.", "> Goodbye."), printed());
    }

    @Test
    void aMistakeIsAnswered() {
        assertEquals("Unknown command: lend. Type help.", cli.execute("lend John C1"));
        assertEquals("There is no copy C9.", cli.execute("borrow John C9"));
        assertEquals("There is no member called Bob.", cli.execute("borrow Bob C1"));
        assertEquals("C1 is not on loan.", cli.execute("return C1"));
        assertEquals("Write it as in \"borrow John C1\".", cli.execute("borrow John"));
        assertEquals("Write the day as in \"day 2 March\".", cli.execute("day tomorrow"));
        assertEquals("Write the day as in \"day 2 March\".", cli.execute("day 31 February"));
    }

    @Test
    void theMonthAtTheLibraryRunsAsAScript() throws IOException {
        try (BufferedReader script = Files.newBufferedReader(Path.of("scripts/a-month-at-the-library.txt"))) {
            cli.run(script, true);
        }
        List<String> lines = printed();

        assertEquals(List.of(
                "> day 16 March",
                "It is 16 March.",
                "> return C1",
                "Notification for Mary: The Hobbit is waiting for you.",
                "John returned C1 on time. C1 is on hold for Mary, who has been notified.",
                "> day 17 March",
                "It is 17 March.",
                "> borrow Jane C1",
                "The library refuses. C1 is on hold for Mary.",
                "> borrow Mary C1",
                "Mary borrowed C1 of The Hobbit. It is due on 31 March."),
                lines.subList(lines.indexOf("> day 16 March"), lines.indexOf("> reservations")));
        assertTrue(lines.contains("The library refuses. The Hobbit is reserved."));
        assertTrue(lines.contains("Jane renewed the loan of C2. It is now due on 30 March."));
        assertTrue(lines.contains("Jane returned C2 3 days late and now owes 1.50 EUR."));
        assertTrue(lines.contains("Jane paid 1.50 EUR and owes nothing now."));
        assertEquals("Mary: 0 loans, owes 1.50 EUR", lines.get(lines.size() - 1));
    }

    private List<String> printed() {
        return List.of(console.toString(StandardCharsets.UTF_8).split("\\R"));
    }
}
