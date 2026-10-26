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
import library.ports.driving.ForPayingFines;
import library.domain.LibraryException;
import library.domain.Member;
import library.domain.Money;

public class PayDialog extends Stage {

    public String result = "";

    public PayDialog(ForPayingFines memberService) {
        setTitle("Pay fines");
        initModality(Modality.APPLICATION_MODAL);

        ComboBox<Member> memberBox = new ComboBox<>(FXCollections.observableArrayList(memberService.members()));
        Label message = new Label();
        message.setStyle("-fx-text-fill: red;");

        Button pay = new Button("Pay");
        pay.setOnAction(e -> {
            Member member = memberBox.getValue();
            if (member == null) {
                message.setText("Choose a member.");
                return;
            }
            try {
                Money paid = memberService.pay(member);
                result = member.name() + " paid " + paid + " and owes nothing now.";
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
        grid.add(message, 0, 1, 2, 1);
        grid.add(new HBox(10, pay, cancel), 0, 2, 2, 1);

        setScene(new Scene(grid));
    }
}
