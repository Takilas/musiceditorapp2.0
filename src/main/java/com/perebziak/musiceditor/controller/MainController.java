package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.exception.AppException;
import com.perebziak.musiceditor.model.Track;
import com.perebziak.musiceditor.util.AlertHelper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class MainController {

  private final AppContext context;
  private final SceneNavigator navigator;

  @FXML private Label welcomeLabel;
  @FXML private TextField searchField;
  @FXML private TableView<Track> tracksTable;
  @FXML private TableColumn<Track, String> titleColumn;
  @FXML private TableColumn<Track, Integer> durationColumn;
  @FXML private TableColumn<Track, String> addedDateColumn;
  @FXML private Button deleteButton;
  @FXML private Button playButton;
  @FXML private Slider seekSlider;
  @FXML private Label currentTimeLabel;
  @FXML private Label totalTimeLabel;
  @FXML private Button adminButton;

  public MainController(AppContext context, SceneNavigator navigator) {
    this.context = context;
    this.navigator = navigator;
  }

  @FXML
  private void initialize() {
    welcomeLabel.setText("Вітаємо, " + context.getSessionManager().getCurrentUser().getUsername() + "!");

    titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    durationColumn.setCellValueFactory(new PropertyValueFactory<>("durationSeconds"));
    addedDateColumn.setCellValueFactory(cellData -> {
      var date = cellData.getValue().getAddedDate();
      return new javafx.beans.property.SimpleStringProperty(
          date != null ? date.toLocalDate().toString() : "");
    });

    refreshTable();
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

  private void refreshTable() {
    ObservableList<Track> tracks = FXCollections.observableArrayList(loadTracks());
    tracksTable.setItems(tracks);
  }

  private List<Track> loadTracks() {
    String query = searchField.getText();
    return context.getLibraryService().searchTracks(query);
  }

  @FXML
  private void onSearchClick() {
    refreshTable();
  }

  @FXML
  private void onAddTrackClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Обрати аудіофайл");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Аудіофайли (*.mp3, *.wav)", "*.mp3", "*.wav"));

    File selectedFile = fileChooser.showOpenDialog(tracksTable.getScene().getWindow());
    if (selectedFile == null) {
      return;
    }

    TextInputDialog titleDialog = new TextInputDialog(stripExtension(selectedFile.getName()));
    titleDialog.setTitle("Назва треку");
    titleDialog.setHeaderText(null);
    titleDialog.setContentText("Введіть назву треку:");

    Optional<String> title = titleDialog.showAndWait();
    if (title.isEmpty()) {
      return;
    }

    try {
      context.getLibraryService().addTrack(selectedFile, title.get());
      refreshTable();
      AlertHelper.showInfo("Трек успішно додано до бібліотеки!");
    } catch (AppException e) {
      AlertHelper.showError(e.getMessage());
    }
  }

  @FXML
  private void onDeleteClick() {
    Track selected = tracksTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertHelper.showError("Оберіть трек для видалення");
      return;
    }
    boolean confirmed = AlertHelper.showConfirm("Видалити трек \"" + selected.getTitle() + "\"?");
    if (confirmed) {
      context.getLibraryService().deleteTrack(selected.getId());
      refreshTable();
    }
  }

  @FXML
  private void onEditClick() {
    Track selected = tracksTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertHelper.showError("Оберіть трек для редагування");
      return;
    }
    context.getPlayerService().stop();
    navigator.switchScene("/track-edit-view.fxml", "Музичний редактор — Редагування",
        new TrackEditController(context, navigator, selected));
  }

  @FXML
  private void onPlayClick() {
    Track selected = tracksTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertHelper.showError("Оберіть трек для відтворення");
      return;
    }
    context.getPlayerService().play(
        selected.getFilePath(),
        totalDuration -> {
          int seconds = (int) totalDuration.toSeconds();
          if (seconds > 0 && selected.getDurationSeconds() == 0) {
            javafx.application.Platform.runLater(() -> {
              context.getLibraryService().updateDuration(selected.getId(), seconds);
              refreshTable();
            });
          }
        },
        errorMessage -> javafx.application.Platform.runLater(() ->
            AlertHelper.showError("Не вдалося відтворити файл: " + errorMessage))
    );
  }

  @FXML
  private void onStopClick() {
    context.getPlayerService().stop();
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

  private String stripExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
  }
}