package library.cli;

import java.io.PrintStream;

import library.domain.Member;
import library.ports.driven.Notifier;

/**
 * Tells a member something by printing it. A driven adapter that lives in the module
 * of the driver that owns the console: the command line has nowhere to show a list of
 * past messages, so it shows each one as it is sent. The core calls {@code send} and
 * does not know whether the message was kept, printed, or mailed.
 */
public class ConsoleNotifier implements Notifier {

    private final PrintStream out;

    public ConsoleNotifier(PrintStream out) {
        this.out = out;
    }

    @Override
    public void send(Member member, String message) {
        out.println("Notification for " + member.name() + ": " + message);
    }
}
