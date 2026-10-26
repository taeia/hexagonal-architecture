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
import library.ports.driving.ForLendingCopies;
import library.domain.Copy;
import library.domain.Loan;

public class ReturnDialog extends Stage {

    public String result = "";

    public ReturnDialog(ForLendingCopies loanService) {
        setTitle("Return a copy");
        initModality(Modality.APPLICATION_MODAL);

        ComboBox<Loan> loanBox = new ComboBox<>(FXCollections.observableArrayList(loanService.loans()));
        loanBox.setPrefWidth(320);
        Label message = new Label();
        message.setStyle("-fx-text-fill: red;");

        Button returnButton = new Button("Return");
        returnButton.setOnAction(e -> {
            Loan loan = loanBox.getValue();
            if (loan == null) {
                message.setText("Choose a loan.");
                return;
            }
            long daysLate = loanService.returnCopy(loan);
            Copy copy = loan.copy();
            String name = loan.member().name();
            if (daysLate > 0) {
                result = name + " returned " + copy.number() + " " + daysLate + " days late and now owes "
                        + loan.member().debt() + ".";
            } else {
                result = name + " returned " + copy.number() + " on time.";
            }
            if (copy.isOnHold()) {
                result += " " + copy.number() + " is on hold for " + copy.heldFor().name() + ", who has been notified.";
            }
            close();
        });

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Loan"), 0, 0);
        grid.add(loanBox, 1, 0);
        grid.add(message, 0, 1, 2, 1);
        grid.add(new HBox(10, returnButton, cancel), 0, 2, 2, 1);

        setScene(new Scene(grid));
    }
}
