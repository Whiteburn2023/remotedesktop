package ru.otus.remotedesktop.client;

import ru.otus.remotedesktop.common.Command;
import ru.otus.remotedesktop.common.ScreenFrame;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;

import java.io.ByteArrayInputStream;

public class ClientController {
    @FXML private BorderPane mainPane;
    @FXML private ImageView screenView;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button connectButton;
    @FXML private Label statusLabel;

    private ConnectionManager connectionManager;
    private double scaleX = 1.0;
    private double scaleY = 1.0;

    @FXML
    public void initialize() {
        screenView.setFocusTraversable(true); //фокус, ловим события клавиатуры

        hostField.setText("192.168.1.104");  //hostField.setText("localhost");
        portField.setText("5900");
        usernameField.setText("admin");
        passwordField.setText("password");

        statusLabel.setText("отключено");
    }

    @FXML
    private void handleConnect() {
        if (connectionManager != null && connectionManager.isConnected()) {
            connectionManager.disconnect();
            connectButton.setText("подключиться");
            statusLabel.setText("отключено");
            screenView.setImage(null);
        } else {
            try {
                String host = hostField.getText();
                int port = Integer.parseInt(portField.getText());
                String username = usernameField.getText();
                String password = passwordField.getText();

                connectionManager = new ConnectionManager();
                boolean success = connectionManager.connect(host, port, username, password);

                if (success) {
                    connectButton.setText("отключиться");
                    statusLabel.setText("подключено к " + host + " : " + port);

                    connectionManager.startReceivingFrames(this::updateScreen);
                } else {
                    showAlert("ошибка" ,"неверные логин/пароль или сервер недоступен");
                    statusLabel.setText("ошибка подключения");
                }
            } catch (NumberFormatException e) {
                showAlert("ошибка", "порт должен быть числом");
            }
        }
    }

    public void updateScreen(ScreenFrame frame) {
        ByteArrayInputStream bis = new ByteArrayInputStream(frame.getImageData());
        Image image = new Image(bis);

        Platform.runLater(() -> {
            screenView.setImage(image);

            if (image.getWidth() > 0 && image.getHeight() > 0) {
                scaleX = frame.getWidth() / screenView.getBoundsInLocal().getWidth();
                scaleY = frame.getHeight() / screenView.getBoundsInLocal().getHeight();
            }
        });
    }

    /** mouse */

    @FXML
    private void handleMouseMoved(MouseEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int x = (int) (event.getX() * scaleX);
            int y = (int) (event.getY() * scaleY);
            connectionManager.sendCommand(Command.MOUSE_MOVE, x, y);
        }
    }

    @FXML
    private void handleMousePressed(MouseEvent event){
        if (connectionManager != null && connectionManager.isConnected()){
            int button = getMouseButton(event);
            connectionManager.sendCommand(Command.MOUSE_PRESS, button);
        }
    }

    @FXML
    private void handleMouseReleased(MouseEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int button = getMouseButton(event);
            connectionManager.sendCommand(Command.MOUSE_RELEASE, button);
        }
    }

    @FXML
    private void handleMouseWheel(ScrollEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int wheelAmt = (int) event.getDeltaY();         //если колесико вертит не в ту сторону, то поставить -
            connectionManager.sendCommand(Command.MOUSE_WHEEL, wheelAmt);
        }
    }

    private int getMouseButton(MouseEvent event) {
        if (event.isPrimaryButtonDown()) return 1; //левая
        if (event.isMiddleButtonDown()) return 2;
        if (event.isSecondaryButtonDown()) return 3; //правая
        return 1;
    }

    /** keyboard */

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int keyCode = event.getCode().getCode();
            connectionManager.sendCommand(Command.KEY_PRESS, keyCode);
        }
    }

    @FXML
    private void handleKeyReleased(KeyEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int keyCode = event.getCode().getCode();
            connectionManager.sendCommand(Command.KEY_RELEASE, keyCode);
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}
