package library.desktop;

import java.time.format.DateTimeFormatter;

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
import library.domain.Copy;
import library.domain.LibraryException;
import library.domain.Loan;
import library.domain.Member;
import library.ports.driving.ForLendingCopies;
import library.ports.driving.ForPayingFines;

public class BorrowDialog extends Stage {

    public String result = "";

    public BorrowDialog(ForLendingCopies loanService, ForPayingFines memberService) {
        setTitle("Borrow a copy");
        initModality(Modality.APPLICATION_MODAL);

        ComboBox<Member> memberBox = new ComboBox<>(FXCollections.observableArrayList(memberService.members()));
        ComboBox<Copy> copyBox = new ComboBox<>(FXCollections.observableArrayList(loanService.copies()));
        Label message = new Label();
        message.setStyle("-fx-text-fill: red;");

        Button borrow = new Button("Borrow");
        borrow.setOnAction(e -> {
            Member member = memberBox.getValue();
            Copy copy = copyBox.getValue();
            if (member == null || copy == null) {
                message.setText("Choose a member and a copy.");
                return;
            }
            try {
                Loan loan = loanService.borrow(member, copy);
                result = member.name() + " borrowed " + copy.number() + " of " + copy.book().title()
                        + ". It is due on " + loan.dueDate().format(DateTimeFormatter.ofPattern("d MMMM")) + ".";
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
        grid.add(new Label("Copy"), 0, 1);
        grid.add(copyBox, 1, 1);
        grid.add(message, 0, 2, 2, 1);
        grid.add(new HBox(10, borrow, cancel), 0, 3, 2, 1);

        setScene(new Scene(grid));
    }
}
