# Hexagonal Architecture

Companion repository for Chapter 7 of [*Domain-Centric Architecture*](https://github.com/taeia/domain-centric-architecture): the library lending application as a hexagon. The core declares ports, adapters plug into them, and the same core is driven from a desktop window, from the command line, and from the scenario tests.

The rules, the aggregates, the repositories, the domain services, and the three application services are those of Chapter 6, and so are the seventeen scenarios. What changed is the shape around them. The two layers inside, `library.domain` and `library.application`, now share one module, `core`, because the hexagon has one inside. The six interfaces the core needs the outside to fulfil, the four repositories, the clock, and the notifier, moved into `library.ports.driven`, and three new interfaces the outside calls, `ForLendingCopies`, `ForReservingBooks`, and `ForPayingFines`, sit in `library.ports.driving`, implemented by the application services. Everything else is an adapter: the in-memory storage, clock, and mailbox on the driven side, and the JavaFX window, the command line, and the tests on the driving side. The command line is new. It reads commands in the words of Chapter 0 and prints its notifications instead of keeping them, and the core did not change to admit it.

Each adapter is a Maven module, and each module's `pom.xml` names what it depends on. `core` depends on nothing. `inmemory` depends on `core`, because it implements the driven ports. `desktop`, `cli`, and `scenarios` each depend on `core`, for the driving ports they call and the domain objects that reach them, and on `inmemory`, because each one's entry point is the configurator that chooses the adapters and wires them. The core does not know that any of the four exists.

The library it implements, with its rules and scenarios, is described in [Chapter 0](https://github.com/taeia/domain-centric-architecture/blob/main/chapters/00-the-library.md). The chapter about this code is [Chapter 7](https://github.com/taeia/domain-centric-architecture/blob/main/chapters/07-hexagonal-architecture.md).

## Running

You need Java 25 and Maven. JavaFX is a Maven dependency, so nothing else has to be installed.

```
mvn install
mvn -pl desktop javafx:run
```

The first command builds all five modules and runs the tests. The second starts the window from the desktop module. Repeat the first command after changing anything outside `desktop`.

A window opens with the library in its starting state. The date picker in the toolbar sets the library's idea of today, so you can walk through the scenarios of Chapter 0 and check the due dates and fines by hand. The *Reset library* button returns to the starting state.

To start the window from an IDE, run `library.desktop.Launcher`. Running `MainWindow` directly fails with *JavaFX runtime components are missing*, because JavaFX is on the classpath and the Java launcher checks for the modules whenever the main class extends `Application`. `Launcher` does not extend it, so the check is skipped.

The same library on the command line:

```
mvn -q -pl cli exec:java
```

Type `help` for the commands. They are written the way Chapter 0 writes its scenarios: `day 2 March`, `borrow John C1`, `return C1`, `renew C1`, `reserve Mary The Hobbit`, `pay Jane`, and `copies`, `members`, `reservations` to look at the library. `quit` ends the session. A script runs the long scenario of Chapter 0 from start to finish:

```
mvn -q -pl cli exec:java -Dexec.args=cli/scripts/a-month-at-the-library.txt
```

## Tests

```
mvn test
```

`ScenarioTest` lives in the `scenarios` module and has one test per scenario of Chapter 0, with the same name. It holds the three driving ports, the way the window and the command line do, and moves a `SettableClock` from day to day. It is the third driver of the core, and it does not start JavaFX.

The `core` module has the unit tests of single rules, unchanged from Chapter 6: `LoanTest` and `MemberTest` pass dates and need no clock, and `LendingTest` and `ReservingTest` use the fakes in the test directory, written against the driven ports. The `cli` module tests the command line: what it prints for what it is told, including the whole month script.

## Layout

```
pom.xml                        the parent: lists the five modules, holds the versions
core/                          the hexagon: domain, application services, and the ports on its edge
  pom.xml                      depends on nothing (JUnit in test scope)
  src/main/java/library/
    domain/
      Member.java              aggregate root: name, debt, loans; borrow, returnCopy, renew, pay
      Loan.java                entity inside Member: due date, isOverdue, fine, each for a given day
      Queue.java               aggregate root: the reservations of one book; join, leave, first
      Reservation.java         entity inside Queue
      Copy.java                aggregate root: available, on loan, or on hold; lendTo, holdFor, makeAvailable
      Book.java                aggregate root: ISBN and title
      Money.java               value object: an amount in euros, never negative
      Fine.java                value object: days of delay and what they cost
      Lending.java             domain service: borrow, return, renew, across aggregates; asks the clock
      Reserving.java           domain service: reserve
      LibraryException.java    thrown when the library refuses an operation
    application/
      LoanService.java         implements ForLendingCopies: calls Lending, notifies through the Notifier port
      ReservationService.java  implements ForReservingBooks: calls Reserving, reads the position back
      MemberService.java       implements ForPayingFines: calls the member, saves
    ports/
      driving/                 what the outside calls
        ForLendingCopies.java
        ForReservingBooks.java
        ForPayingFines.java
      driven/                  what the core calls
        MemberRepository.java
        QueueRepository.java
        CopyRepository.java
        BookRepository.java
        Clock.java
        Notifier.java
  src/test/java/library/domain/
    MemberTest.java            unit tests of single rules, one class per domain class
    LoanTest.java
    QueueTest.java
    CopyTest.java
    MoneyTest.java
    FineTest.java
    LendingTest.java
    ReservingTest.java
    FakeMemberRepository.java  the driven ports, implemented for these tests
    FakeCopyRepository.java
    FakeQueueRepository.java
    FakeClock.java
inmemory/                      driven adapters
  pom.xml                      depends on core
  src/main/java/library/inmemory/
    InMemoryMemberRepository.java   the repository ports, implemented with lists
    InMemoryQueueRepository.java
    InMemoryCopyRepository.java
    InMemoryBookRepository.java
    SettableClock.java         the clock: set by the date picker, the day command, and the tests
    InMemoryNotifier.java      the notifier: adds each message to a list the window shows
    StartingState.java         the starting state of Chapter 0
desktop/                       driving adapter: the JavaFX window
  pom.xml                      depends on core and inmemory
  src/main/java/library/desktop/
    MainWindow.java            start() is the configurator; the rest of the class holds the driving ports
    Launcher.java              starts MainWindow from an IDE
    BorrowDialog.java          the five dialogs, each holding the port it needs
    ReturnDialog.java
    RenewDialog.java
    ReserveDialog.java
    PayDialog.java
cli/                           driving adapter: the command line
  pom.xml                      depends on core and inmemory
  scripts/
    a-month-at-the-library.txt the long scenario of Chapter 0 as a script
  src/main/java/library/cli/
    Cli.java                   main() is the configurator; execute() turns a line into a call on a port
    ConsoleNotifier.java       a driven adapter owned by this driver: prints each message
  src/test/java/library/cli/
    CliTest.java               what the command line prints for what it is told
scenarios/                     driving adapter: the tests
  pom.xml                      depends on core and inmemory
  src/test/java/library/scenarios/
    ScenarioTest.java          one test per scenario
```
