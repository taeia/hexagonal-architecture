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
import library.ports.driving.ForLendingCopies;
import library.domain.LibraryException;
import library.domain.Loan;

public class RenewDialog extends Stage {

    public String result = "";

    public RenewDialog(ForLendingCopies loanService) {
        setTitle("Renew a loan");
        initModality(Modality.APPLICATION_MODAL);

        ComboBox<Loan> loanBox = new ComboBox<>(FXCollections.observableArrayList(loanService.loans()));
        loanBox.setPrefWidth(320);
        Label message = new Label();
        message.setStyle("-fx-text-fill: red;");

        Button renew = new Button("Renew");
        renew.setOnAction(e -> {
            Loan loan = loanBox.getValue();
            if (loan == null) {
                message.setText("Choose a loan.");
                return;
            }
            try {
                loanService.renew(loan);
                result = loan.member().name() + " renewed the loan of " + loan.copy().number()
                        + ". It is now due on " + loan.dueDate().format(DateTimeFormatter.ofPattern("d MMMM")) + ".";
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
        grid.add(new Label("Loan"), 0, 0);
        grid.add(loanBox, 1, 0);
        grid.add(message, 0, 1, 2, 1);
        grid.add(new HBox(10, renew, cancel), 0, 2, 2, 1);

        setScene(new Scene(grid));
    }
}
