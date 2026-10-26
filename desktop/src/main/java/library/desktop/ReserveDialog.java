package library.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import library.domain.Book;
import library.domain.LibraryException;
import library.domain.Member;
import library.ports.driving.ForPayingFines;
import library.ports.driving.ForReservingBooks;

public class ReserveDialog extends Stage {

    public String result = "";

    public ReserveDialog(ForReservingBooks reservationService, ForPayingFines memberService) {
        setTitle("Reserve a book");
        initModality(Modality.APPLICATION_MODAL);

        ComboBox<Member> memberBox = new ComboBox<>(FXCollections.observableArrayList(memberService.members()));
        ComboBox<Book> bookBox = new ComboBox<>(FXCollections.observableArrayList(reservationService.books()));
        Label message = new Label();
        message.setStyle("-fx-text-fill: red;");

        Button reserve = new Button("Reserve");
        reserve.setOnAction(e -> {
            Member member = memberBox.getValue();
            Book book = bookBox.getValue();
            if (member == null || book == null) {
                message.setText("Choose a member and a book.");
                return;
            }
            try {
                int position = reservationService.reserve(member, book);
                result = member.name() + " reserved " + book.title() + ". Position in the queue: " + position + ".";
                close();
            } catch (LibraryException ex) {
                message.setText(ex.getMessage());
            }
        });

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Member"), 0, 0);
        grid.add(memberBox, 1, 0);
        grid.add(new Label("Book"), 0, 1);
        grid.add(bookBox, 1, 1);
        grid.add(message, 0, 2, 2, 1);
        grid.add(new HBox(10, reserve, cancel), 0, 3, 2, 1);

        setScene(new Scene(grid));
    }
}
