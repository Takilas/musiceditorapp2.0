package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.exception.AppException;
import com.perebziak.musiceditor.model.Track;
import com.perebziak.musiceditor.util.AlertHelper;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class TrackEditController {

  private final AppContext context;
  private final SceneNavigator navigator;
  private final Track originalTrack;

  @FXML private Label titleLabel;
  @FXML private Label durationLabel;

  @FXML private TextField startField;
  @FXML private TextField endField;

  @FXML private Slider volumeSlider;
  @FXML private Label volumeValueLabel;

  @FXML private Slider speedSlider;
  @FXML private Label speedValueLabel;

  @FXML private Slider pitchSlider;
  @FXML private Label pitchValueLabel;

  @FXML private ListView<Track> versionsListView;
  @FXML private Label statusLabel;

  @FXML private Slider seekSlider;
  @FXML private Label currentTimeLabel;
  @FXML private Label totalTimeLabel;

  @FXML private Button adminButton;

  public TrackEditController(AppContext context, SceneNavigator navigator, Track originalTrack) {
    this.context = context;
    this.navigator = navigator;
    this.originalTrack = originalTrack;
  }

  @FXML
  private void initialize() {
    titleLabel.setText("🎵 " + originalTrack.getTitle());
    durationLabel.setText("Тривалість оригіналу: " + originalTrack.getDurationSeconds() + " сек");

    volumeSlider.valueProperty().addListener((obs, o, n) ->
        volumeValueLabel.setText(String.format("%.0f%%", n.doubleValue())));
    speedSlider.valueProperty().addListener((obs, o, n) ->
        speedValueLabel.setText(String.format("%.0f%%", n.doubleValue())));
    pitchSlider.valueProperty().addListener((obs, o, n) ->
        pitchValueLabel.setText(String.format("%.0f%%", n.doubleValue())));

    versionsListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
      @Override
      protected void updateItem(Track track, boolean empty) {
        super.updateItem(track, empty);
        setText(empty || track == null ? null : track.getTitle());
      }
    });

    refreshVersions();
    setupSeekBar();

    if (context.getSessionManager().isAdmin()) {
      adminButton.setVisible(true);
      adminButton.setManaged(true);
    }
  }

  private void setupSeekBar() {
    context.getPlayerService().totalDurationProperty().addListener((obs, oldVal, newVal) ->
        javafx.application.Platform.runLater(() -> {
          seekSlider.setMax(newVal.toSeconds());
          totalTimeLabel.setText(com.perebziak.musiceditor.util.TimeFormatUtil.format(newVal));
        }));
    context.getPlayerService().currentTimeProperty().addListener((obs, oldVal, newVal) ->
        javafx.application.Platform.runLater(() -> {
          if (!seekSlider.isValueChanging()) {
            seekSlider.setValue(newVal.toSeconds());
          }
          currentTimeLabel.setText(com.perebziak.musiceditor.util.TimeFormatUtil.format(newVal));
        }));
    seekSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
      if (!isChanging) {
        context.getPlayerService().seek(javafx.util.Duration.seconds(seekSlider.getValue()));
      }
    });
  }

  private void refreshVersions() {
    List<Track> versions = new ArrayList<>();
    versions.add(originalTrack);
    versions.addAll(context.getLibraryService().getEditedVersions(originalTrack.getId()));
    versionsListView.setItems(FXCollections.observableArrayList(versions));
    versionsListView.getSelectionModel().selectFirst();
  }

  private Long currentUserId() {
    return context.getSessionManager().getCurrentUser().getId();
  }

  @FXML
  private void onApplyCutClick() {
    try {
      int start = Integer.parseInt(startField.getText().trim());
      int end = Integer.parseInt(endField.getText().trim());
      context.getAudioEditService().cut(originalTrack, start, end, currentUserId());
      showSuccess("Створено обрізану версію треку");
    } catch (NumberFormatException e) {
      AlertHelper.showError("Введіть коректні значення секунд (цілі числа)");
    } catch (AppException e) {
      AlertHelper.showError(e.getMessage());
    }
  }

  @FXML
  private void onApplyVolumeClick() {
    try {
      double gain = volumeSlider.getValue() / 100.0;
      context.getAudioEditService().changeVolume(originalTrack, gain, currentUserId());
      showSuccess("Створено версію зі зміненою гучністю");
    } catch (AppException e) {
      AlertHelper.showError(e.getMessage());
    }
  }

  @FXML
  private void onApplySpeedClick() {
    try {
      double speed = speedSlider.getValue() / 100.0;
      context.getAudioEditService().changeSpeed(originalTrack, speed, currentUserId());
      showSuccess("Створено версію зі зміненою швидкістю");
    } catch (AppException e) {
      AlertHelper.showError(e.getMessage());
    }
  }

  @FXML
  private void onApplyPitchClick() {
    try {
      double pitch = pitchSlider.getValue() / 100.0;
      context.getAudioEditService().changePitch(originalTrack, pitch, currentUserId());
      showSuccess("Створено версію зі зміненою висотою тону");
    } catch (AppException e) {
      AlertHelper.showError(e.getMessage());
    }
  }

  private void showSuccess(String message) {
    refreshVersions();
    statusLabel.setStyle("-fx-text-fill: #27ae60;");
    statusLabel.setText(message);
  }

  @FXML
  private void onPlayVersionClick() {
    Track selected = versionsListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertHelper.showError("Оберіть версію для відтворення");
      return;
    }
    context.getPlayerService().play(selected.getFilePath(),
        totalDuration -> {},
        error -> javafx.application.Platform.runLater(() ->
            AlertHelper.showError("Не вдалося відтворити: " + error)));
  }

  @FXML
  private void onStopVersionClick() {
    context.getPlayerService().stop();
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
  private void onOpenConverterClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/converter-view.fxml", "Музичний редактор — Конвертер",
        new ConverterController(context, navigator));
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