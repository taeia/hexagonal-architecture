package library.ports.driven;

import library.domain.Member;

/**
 * Tells a member something. A driven port: the core says what it wants said and to
 * whom, and an adapter decides what sending means, a list in a window, a line on a
 * console, or a real channel. Chapter 6 put this interface in the domain and had the
 * application layer call it, and asked what to make of that. The hexagon's answer is
 * that both are inside, and a port belongs to the inside as a whole.
 */
public interface Notifier {

    void send(Member member, String message);
}
