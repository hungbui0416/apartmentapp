package io.github.btmxh.apartmentapp;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;

public class PickServiceFee {
    private Stage stage;
    public TextField searchBar;
    public ListView<ServiceFee> result;
    private ServiceFee ans;

    public void initialize() {
        result.setCellFactory(l -> new ListCell<>(){
            @Override
            protected void updateItem(ServiceFee serviceFee, boolean b) {
                super.updateItem(serviceFee, b);
                if(b || serviceFee == null) {
                    setText("");
                } else {
                    setText(serviceFee.getName());
                }
            }
        });
        searchBar.textProperty().addListener((a1, a2, value) -> {
            try {
                updateItems(value);
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            updateItems("");
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateItems(String query) throws SQLException, IOException {
        final var ans = DatabaseConnection.getInstance().getServiceFees(query, Integer.MAX_VALUE, 0);
        result.setItems(FXCollections.observableArrayList(ans));
    }

    public void submit(ActionEvent actionEvent) {
        if(result.getSelectionModel().getSelectedItem() == null) {
            Announcement.show("Lỗi", "Chưa chọn khoản phí nào", "Hãy nhấn chọn khoản phí trong bảng kết quả");
            return;
        }

        ans = result.getSelectionModel().getSelectedItem();
        stage.close();
    }

    public void cancel(ActionEvent actionEvent) {
        stage.close();
    }

    public void setStage(Stage stage) {
        searchBar.setText("");
        this.stage = stage;
    }

    public ServiceFee getAns() {
        return ans;
    }

    public static ServiceFee open(Window window) throws IOException {
        final var loader = Utils.fxmlLoader("/pick-service-fee.fxml");
        final Parent content = loader.load();
        final PickServiceFee controller = loader.getController();
        final var stage = new Stage();

        stage.initOwner(window);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(content));
        controller.setStage(stage);
        stage.showAndWait();

        return controller.getAns();
    }
}

