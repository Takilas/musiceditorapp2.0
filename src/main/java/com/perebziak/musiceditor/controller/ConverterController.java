package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.exception.AppException;
import com.perebziak.musiceditor.model.Track;
import com.perebziak.musiceditor.util.AlertHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

public class ConverterController {

  private static final String[] FORMATS = {"MP3", "WAV", "MP4"};

  private final AppContext context;
  private final SceneNavigator navigator;

  @FXML private ChoiceBox<Track> trackChoiceBox;
  @FXML private ChoiceBox<String> formatChoiceBox;
  @FXML private Label priceLabel;

  @FXML private TextField cardNumberField;
  @FXML private TextField expiryField;
  @FXML private PasswordField cvvField;

  @FXML private Button payButton;
  @FXML private ProgressIndicator progressIndicator;
  @FXML private Label statusLabel;
  @FXML private Button adminButton;

  public ConverterController(AppContext context, SceneNavigator navigator) {
    this.context = context;
    this.navigator = navigator;
  }

  @FXML
  private void initialize() {
    trackChoiceBox.setItems(FXCollections.observableArrayList(
        context.getLibraryService().getAllTracks()));
    trackChoiceBox.setConverter(new javafx.util.StringConverter<>() {
      @Override
      public String toString(Track track) {
        return track == null ? "" : track.getTitle();
      }

      @Override
      public Track fromString(String string) {
        return null;
      }
    });

    formatChoiceBox.setItems(FXCollections.observableArrayList(FORMATS));
    formatChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePrice());

    progressIndicator.setVisible(false);

    if (context.getSessionManager().isAdmin()) {
      adminButton.setVisible(true);
      adminButton.setManaged(true);
    }
  }

  private void updatePrice() {
    String format = formatChoiceBox.getValue();
    if (format == null) {
      priceLabel.setText("");
      return;
    }
    BigDecimal price = context.getConversionService().getPrice(format);
    priceLabel.setText("Вартість конвертації: " + price + " ₴");
  }

  @FXML
  private void onPayAndConvertClick() {
    Track track = trackChoiceBox.getValue();
    String format = formatChoiceBox.getValue();

    if (track == null) {
      AlertHelper.showError("Оберіть трек для конвертації");
      return;
    }
    if (format == null) {
      AlertHelper.showError("Оберіть цільовий формат");
      return;
    }

    payButton.setDisable(true);
    progressIndicator.setVisible(true);
    statusLabel.setStyle("-fx-text-fill: gray;");
    statusLabel.setText("Обробка оплати...");

    Long userId = context.getSessionManager().getCurrentUser().getId();
    String cardNumber = cardNumberField.getText();
    String expiry = expiryField.getText();
    String cvv = cvvField.getText();

    Task<Void> conversionTask = new Task<>() {
      @Override
      protected Void call() {
        context.getConversionService().convertTrack(userId, track, format, cardNumber, expiry, cvv);
        return null;
      }
    };

    conversionTask.setOnSucceeded(e -> Platform.runLater(() -> {
      progressIndicator.setVisible(false);
      payButton.setDisable(false);
      statusLabel.setStyle("-fx-text-fill: #27ae60;");
      statusLabel.setText("Готово! Файл сконвертовано у формат " + format + ".");
    }));

    conversionTask.setOnFailed(e -> Platform.runLater(() -> {
      progressIndicator.setVisible(false);
      payButton.setDisable(false);
      Throwable ex = conversionTask.getException();
      String message = ex instanceof AppException ? ex.getMessage() : "Сталася помилка під час конвертації";
      statusLabel.setStyle("-fx-text-fill: #c0392b;");
      statusLabel.setText(message);
    }));

    new Thread(conversionTask).start();
  }

  @FXML
  private void onGoLibraryClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/main-view.fxml", "Музичний редактор — Бібліотека",
        new MainController(context, navigator));
  }

  @FXML
  private void onOpenPlaylistsClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/playlist-view.fxml", "Музичний редактор — Плейлисти",
        new PlaylistController(context, navigator));
  }

  @FXML
  private void onOpenSettingsClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/settings-view.fxml", "Музичний редактор — Налаштування",
        new SettingsController(context, navigator));
  }

  @FXML
  private void onOpenAdminClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/admin-view.fxml", "Музичний редактор — Адміністрування",
        new AdminController(context, navigator));
  }
}