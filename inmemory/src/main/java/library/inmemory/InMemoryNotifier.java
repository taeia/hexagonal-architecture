package library.inmemory;

import java.util.ArrayList;
import java.util.List;

import library.domain.Member;
import library.ports.driven.Notifier;

/**
 * Sends messages to members by adding them to a list that the main window shows. A
 * driven adapter: the core says what a notifier is, through the port, and this is one
 * way to be one. The command line has another, which prints instead of keeping, and a
 * real channel would be a third. The core cannot tell them apart.
 */
public class InMemoryNotifier implements Notifier {

    private final List<String> sent = new ArrayList<>();

    @Override
    public void send(Member member, String message) {
        sent.add(member.name() + ": " + message);
    }

    public List<String> sent() {
        return new ArrayList<>(sent);
    }

    public void clear() {
        sent.clear();
    }
}
