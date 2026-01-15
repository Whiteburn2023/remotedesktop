package ru.otus.remotedesktop.client;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicLong;

public class ClientController {
    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @FXML private BorderPane mainPane;
    @FXML private ImageView screenView;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button connectButton;
    @FXML private Label statusLabel;
    @FXML private Label fpsLabel;
    @FXML private CheckBox enableVideoCheck;

    private ConnectionManager connectionManager;
    private VideoClient videoClient;
    private Java2DFrameConverter converter;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private Thread statsThread;
    private final AtomicLong frameCount = new AtomicLong(0);
    private long lastStatTime = System.currentTimeMillis();

    @FXML
    public void initialize() {
        screenView.setFocusTraversable(true);
        screenView.setOnMouseClicked(event -> screenView.requestFocus());

        hostField.setText("localhost");
        portField.setText("5900");
        usernameField.setText("admin");
        passwordField.setText("password");
        statusLabel.setText("Отключено");

        enableVideoCheck.setSelected(true);

        converter = new Java2DFrameConverter();
    }

    @FXML
    private void handleConnect() {
        if (connectionManager != null && connectionManager.isConnected()) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        try {
            String host = hostField.getText();
            int controlPort = Integer.parseInt(portField.getText());
            int videoPort = controlPort + 1; // Видеопорт на +1 от управляющего
            String username = usernameField.getText();
            String password = passwordField.getText();

            // 1. Подключаемся для управления
            connectionManager = new ConnectionManager();
            boolean controlConnected = connectionManager.connect(host, controlPort, username, password);

            if (!controlConnected) {
                showAlert("Ошибка", "Не удалось подключиться для управления");
                return;
            }

            // 2. Подключаемся для видео (если включено)
            if (enableVideoCheck.isSelected()) {
                videoClient = new VideoClient(frame -> updateVideoFrame(frame));
                boolean videoConnected = videoClient.connect(host, videoPort);

                if (!videoConnected) {
                    showAlert("Предупреждение", "Видеопоток недоступен, работает только управление");
                    // Продолжаем без видео
                }
            }

            updateUIForConnectedState();
            startStatsThread();

        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Порт должен быть числом");
        } catch (Exception e) {
            showAlert("Ошибка подключения", e.getMessage());
            logger.error("Ошибка подключения: {}", e.getMessage());
        }
    }

    private void updateVideoFrame(Frame frame) {
        frameCount.incrementAndGet();

        Platform.runLater(() -> {
            try {
                // Конвертируем Frame из JavaCV в JavaFX Image
                BufferedImage bufferedImage = converter.convert(frame);
                if (bufferedImage != null) {
                    Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                    screenView.setImage(image);

                    // Обновляем коэффициенты масштабирования
                    if (image.getWidth() > 0 && image.getHeight() > 0) {
                        double viewWidth = screenView.getBoundsInLocal().getWidth();
                        double viewHeight = screenView.getBoundsInLocal().getHeight();

                        if (viewWidth > 0 && viewHeight > 0) {
                            scaleX = image.getWidth() / viewWidth;
                            scaleY = image.getHeight() / viewHeight;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Ошибка конвертации кадра: {}", e.getMessage());
            }
        });
    }

    private void disconnect() {
        if (videoClient != null) {
            videoClient.disconnect();
        }

        if (connectionManager != null) {
            connectionManager.disconnect();
        }

        updateUIForDisconnectedState();
        stopStatsThread();

        Platform.runLater(() -> {
            screenView.setImage(null);
            statusLabel.setText("Отключено");
        });
    }

    private void startStatsThread() {
        stopStatsThread();
        statsThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    long currentTime = System.currentTimeMillis();
                    long frames = frameCount.get();
                    double fps = frames * 1000.0 / (currentTime - lastStatTime);

                    Platform.runLater(() -> {
                        fpsLabel.setText(String.format("FPS: %.1f", fps));
                        statusLabel.setText(String.format("Подключено (FPS: %.1f)", fps));
                    });

                    frameCount.set(0);
                    lastStatTime = currentTime;
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
    }

    private void stopStatsThread() {
        if (statsThread != null && statsThread.isAlive()) {
            statsThread.interrupt();
        }
    }

    private void updateUIForConnectedState() {
        Platform.runLater(() -> {
            connectButton.setText("Отключиться");
            statusLabel.setText("Подключено");
            statusLabel.setStyle("-fx-text-fill: green;");
        });
    }

    private void updateUIForDisconnectedState() {
        Platform.runLater(() -> {
            connectButton.setText("Подключиться");
            statusLabel.setText("Отключено");
            statusLabel.setStyle("-fx-text-fill: red;");
            fpsLabel.setText("FPS: 0.0");
        });
    }

    // Обработчики мыши и клавиатуры остаются без изменений
    @FXML
    private void handleMouseMoved(MouseEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int x = (int) (event.getX() * scaleX);
            int y = (int) (event.getY() * scaleY);
            connectionManager.sendCommand(ru.otus.remotedesktop.common.Command.MOUSE_MOVE, x, y);
        }
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int button = getMouseButton(event);
            connectionManager.sendCommand(ru.otus.remotedesktop.common.Command.MOUSE_PRESS, button);
        }
    }

    @FXML
    private void handleMouseReleased(MouseEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int button = getMouseButton(event);
            connectionManager.sendCommand(ru.otus.remotedesktop.common.Command.MOUSE_RELEASE, button);
        }
    }

    @FXML
    private void handleMouseWheel(ScrollEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int wheelAmt = (int) event.getDeltaY();
            connectionManager.sendCommand(ru.otus.remotedesktop.common.Command.MOUSE_WHEEL, wheelAmt);
        }
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int keyCode = event.getCode().getCode();
            connectionManager.sendCommand(ru.otus.remotedesktop.common.Command.KEY_PRESS, keyCode);
        }
    }

    @FXML
    private void handleKeyReleased(KeyEvent event) {
        if (connectionManager != null && connectionManager.isConnected()) {
            int keyCode = event.getCode().getCode();
            connectionManager.sendCommand(ru.otus.remotedesktop.common.Command.KEY_RELEASE, keyCode);
        }
    }

    private int getMouseButton(MouseEvent event) {
        MouseButton button = event.getButton();
        if (button == MouseButton.PRIMARY) return 1;
        if (button == MouseButton.MIDDLE) return 2;
        if (button == MouseButton.SECONDARY) return 3;
        return 1;
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
