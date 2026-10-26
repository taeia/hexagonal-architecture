package library.desktop;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import library.application.LoanService;
import library.application.MemberService;
import library.application.ReservationService;
import library.domain.Copy;
import library.domain.Lending;
import library.domain.Loan;
import library.domain.Member;
import library.domain.Reservation;
import library.domain.Reserving;
import library.inmemory.InMemoryBookRepository;
import library.inmemory.InMemoryCopyRepository;
import library.inmemory.InMemoryMemberRepository;
import library.inmemory.InMemoryNotifier;
import library.inmemory.InMemoryQueueRepository;
import library.inmemory.SettableClock;
import library.inmemory.StartingState;
import library.ports.driving.ForLendingCopies;
import library.ports.driving.ForPayingFines;
import library.ports.driving.ForReservingBooks;

public class MainWindow extends Application {

    private ForLendingCopies loanService;
    private ForReservingBooks reservationService;
    private ForPayingFines memberService;
    private InMemoryNotifier notifier;

    private final ObservableList<Copy> copies = FXCollections.observableArrayList();
    private final ObservableList<Member> members = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private final ObservableList<String> notifications = FXCollections.observableArrayList();
    private Label status;

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // The configurator for this program: the driven adapters, then the domain
        // services that use them, then the application services, which the rest of the
        // window holds as driving ports. This method is the one place in the desktop
        // adapter that knows which storage, which clock, and which mailbox the library
        // runs on. The command line has a configurator of its own and chooses differently.
        InMemoryBookRepository bookRepository = new InMemoryBookRepository();
        InMemoryCopyRepository copyRepository = new InMemoryCopyRepository();
        InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository();
        SettableClock clock = new SettableClock();
        notifier = new InMemoryNotifier();
        Lending lending = new Lending(memberRepository, copyRepository, queueRepository, clock);
        Reserving reserving = new Reserving(copyRepository, queueRepository);
        loanService = new LoanService(lending, memberRepository, copyRepository, notifier);
        reservationService = new ReservationService(reserving, bookRepository, queueRepository);
        memberService = new MemberService(memberRepository);

        // The starting state is loaded where the repositories are built. No service owns
        // the whole library any more, so none of them can offer a reset(). The mailbox is
        // this window's, so the window empties it.
        Runnable resetLibrary = () -> {
            StartingState.load(bookRepository, copyRepository, memberRepository, queueRepository);
            notifier.clear();
        };
        resetLibrary.run();

        DatePicker datePicker = new DatePicker(clock.today());
        datePicker.setOnAction(e -> {
            clock.set(datePicker.getValue());
            refresh();
        });

        Button borrow = new Button("Borrow");
        borrow.setOnAction(e -> {
            BorrowDialog dialog = new BorrowDialog(loanService, memberService);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button returnButton = new Button("Return");
        returnButton.setOnAction(e -> {
            ReturnDialog dialog = new ReturnDialog(loanService);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button renew = new Button("Renew");
        renew.setOnAction(e -> {
            RenewDialog dialog = new RenewDialog(loanService);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button reserve = new Button("Reserve");
        reserve.setOnAction(e -> {
            ReserveDialog dialog = new ReserveDialog(reservationService, memberService);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button pay = new Button("Pay");
        pay.setOnAction(e -> {
            PayDialog dialog = new PayDialog(memberService);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button reset = new Button("Reset library");
        reset.setOnAction(e -> {
            resetLibrary.run();
            status.setText("The library is back in its starting state.");
            refresh();
        });

        HBox toolbar = new HBox(10, new Label("Today:"), datePicker, borrow, returnButton, renew, reserve, pay, reset);
        toolbar.setPadding(new Insets(10));

        TableView<Copy> copyTable = new TableView<>(copies);
        TableColumn<Copy, String> copyColumn = new TableColumn<>("Copy");
        copyColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().number()));
        TableColumn<Copy, String> bookColumn = new TableColumn<>("Book");
        bookColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().book().title()));
        bookColumn.setPrefWidth(160);
        TableColumn<Copy, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(c -> {
            Copy copy = c.getValue();
            if (copy.isOnHold()) {
                return new ReadOnlyStringWrapper("on hold for " + copy.heldFor().name());
            }
            return new ReadOnlyStringWrapper(copy.isOnLoan() ? "on loan" : "available");
        });
        statusColumn.setPrefWidth(120);
        TableColumn<Copy, String> borrowerColumn = new TableColumn<>("Borrower");
        borrowerColumn.setCellValueFactory(c -> {
            Loan loan = loanService.loanOf(c.getValue());
            return new ReadOnlyStringWrapper(loan == null ? "" : loan.member().name());
        });
        TableColumn<Copy, String> dueColumn = new TableColumn<>("Due");
        dueColumn.setCellValueFactory(c -> {
            Loan loan = loanService.loanOf(c.getValue());
            return new ReadOnlyStringWrapper(loan == null ? "" : loan.dueDate().format(DateTimeFormatter.ofPattern("d MMMM")));
        });
        copyTable.getColumns().add(copyColumn);
        copyTable.getColumns().add(bookColumn);
        copyTable.getColumns().add(statusColumn);
        copyTable.getColumns().add(borrowerColumn);
        copyTable.getColumns().add(dueColumn);

        TableView<Member> memberTable = new TableView<>(members);
        TableColumn<Member, String> nameColumn = new TableColumn<>("Member");
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        TableColumn<Member, String> loansColumn = new TableColumn<>("Loans");
        loansColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(loanService.loanCount(c.getValue()))));
        TableColumn<Member, String> debtColumn = new TableColumn<>("Debt");
        debtColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().debt().toString()));
        memberTable.getColumns().add(nameColumn);
        memberTable.getColumns().add(loansColumn);
        memberTable.getColumns().add(debtColumn);

        TableView<Reservation> reservationTable = new TableView<>(reservations);
        TableColumn<Reservation, String> reservedBookColumn = new TableColumn<>("Book");
        reservedBookColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().book().title()));
        reservedBookColumn.setPrefWidth(160);
        TableColumn<Reservation, String> reservedByColumn = new TableColumn<>("Member");
        reservedByColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().member().name()));
        TableColumn<Reservation, String> positionColumn = new TableColumn<>("Position");
        positionColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(reservationService.queuePosition(c.getValue()))));
        reservationTable.getColumns().add(reservedBookColumn);
        reservationTable.getColumns().add(reservedByColumn);
        reservationTable.getColumns().add(positionColumn);

        ListView<String> notificationList = new ListView<>(notifications);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(titled("Copies", copyTable), 0, 0);
        grid.add(titled("Members", memberTable), 1, 0);
        grid.add(titled("Reservations", reservationTable), 0, 1);
        grid.add(titled("Notifications", notificationList), 1, 1);

        status = new Label("Welcome to the library.");
        status.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(grid);
        root.setBottom(status);

        refresh();
        stage.setTitle("Library");
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    private void done(String result) {
        status.setText(result);
        refresh();
    }

    private VBox titled(String title, Node content) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(5, label, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    private void refresh() {
        copies.setAll(loanService.copies());
        members.setAll(memberService.members());
        reservations.setAll(reservationService.reservations());
        notifications.setAll(notifier.sent());
    }
}
