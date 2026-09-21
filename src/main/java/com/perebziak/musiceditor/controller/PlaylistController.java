package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.exception.AppException;
import com.perebziak.musiceditor.model.Playlist;
import com.perebziak.musiceditor.model.Track;
import com.perebziak.musiceditor.util.AlertHelper;
import com.perebziak.musiceditor.util.TimeFormatUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.util.List;

public class PlaylistController {

  private final AppContext context;
  private final SceneNavigator navigator;

  @FXML private Button adminButton;
  @FXML private ListView<Playlist> playlistsListView;
  @FXML private TextField newPlaylistField;
  @FXML private Label selectedPlaylistLabel;
  @FXML private ListView<Track> playlistTracksListView;
  @FXML private ChoiceBox<Track> addTrackChoiceBox;
  @FXML private Label statusLabel;
  @FXML private Slider seekSlider;
  @FXML private Label currentTimeLabel;
  @FXML private Label totalTimeLabel;

  public PlaylistController(AppContext context, SceneNavigator navigator) {
    this.context = context;
    this.navigator = navigator;
  }

  @FXML
  private void initialize() {
    if (context.getSessionManager().isAdmin()) {
      adminButton.setVisible(true);
      adminButton.setManaged(true);
    }

    playlistsListView.setCellFactory(list -> new ListCell<>() {
      @Override
      protected void updateItem(Playlist playlist, boolean empty) {
        super.updateItem(playlist, empty);
        setText(empty || playlist == null ? null : playlist.getName());
      }
    });
    playlistTracksListView.setCellFactory(list -> new ListCell<>() {
      @Override
      protected void updateItem(Track track, boolean empty) {
        super.updateItem(track, empty);
        setText(empty || track == null ? null : track.getTitle());
      }
    });
    addTrackChoiceBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(Track track) {
        return track == null ? "" : track.getTitle();
      }

      @Override
      public Track fromString(String string) {
        return null;
      }
    });

    playlistsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshSelectedPlaylist());

    setupSeekBar();
    refreshPlaylists();
  }

  private void setupSeekBar() {
    context.getPlayerService().totalDurationProperty().addListener((obs, oldVal, newVal) ->
        Platform.runLater(() -> {
          seekSlider.setMax(newVal.toSeconds());
          totalTimeLabel.setText(TimeFormatUtil.format(newVal));
        }));
    context.getPlayerService().currentTimeProperty().addListener((obs, oldVal, newVal) ->
        Platform.runLater(() -> {
          if (!seekSlider.isValueChanging()) {
            seekSlider.setValue(newVal.toSeconds());
          }
          currentTimeLabel.setText(TimeFormatUtil.format(newVal));
        }));
    seekSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
      if (!isChanging) {
        context.getPlayerService().seek(javafx.util.Duration.seconds(seekSlider.getValue()));
      }
    });
  }

  private Long currentUserId() {
    return context.getSessionManager().getCurrentUser().getId();
  }

  private void refreshPlaylists() {
    List<Playlist> playlists = context.getPlaylistService().getUserPlaylists(currentUserId());
    playlistsListView.setItems(FXCollections.observableArrayList(playlists));
  }

  private void refreshSelectedPlaylist() {
    Playlist selected = playlistsListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      selectedPlaylistLabel.setText("Оберіть плейлист");
      playlistTracksListView.setItems(FXCollections.observableArrayList());
      addTrackChoiceBox.setItems(FXCollections.observableArrayList());
      return;
    }

    selectedPlaylistLabel.setText("Плейлист: " + selected.getName());

    List<Track> tracksInPlaylist = context.getPlaylistService().getTracks(selected.getId());
    playlistTracksListView.setItems(FXCollections.observableArrayList(tracksInPlaylist));

    List<Track> allTracks = context.getLibraryService().getAllTracks();
    allTracks.removeIf(t -> tracksInPlaylist.stream().anyMatch(pt -> pt.getId().equals(t.getId())));
    addTrackChoiceBox.setItems(FXCollections.observableArrayList(allTracks));
  }

  @FXML
  private void onCreatePlaylistClick() {
    try {
      context.getPlaylistService().createPlaylist(currentUserId(), newPlaylistField.getText());
      newPlaylistField.clear();
      refreshPlaylists();
    } catch (AppException e) {
      AlertHelper.showError(e.getMessage());
    }
  }

  @FXML
  private void onDeletePlaylistClick() {
    Playlist selected = playlistsListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertHelper.showError("Оберіть плейлист для видалення");
      return;
    }
    boolean confirmed = AlertHelper.showConfirm("Видалити плейлист \"" + selected.getName() + "\"?");
    if (confirmed) {
      context.getPlaylistService().deletePlaylist(selected.getId());
      refreshPlaylists();
      refreshSelectedPlaylist();
    }
  }

  @FXML
  private void onAddTrackClick() {
    Playlist playlist = playlistsListView.getSelectionModel().getSelectedItem();
    Track track = addTrackChoiceBox.getValue();
    if (playlist == null) {
      AlertHelper.showError("Спочатку оберіть плейлист");
      return;
    }
    if (track == null) {
      AlertHelper.showError("Оберіть трек для додавання");
      return;
    }
    context.getPlaylistService().addTrackToPlaylist(playlist.getId(), track.getId());
    refreshSelectedPlaylist();
    statusLabel.setStyle("-fx-text-fill: #27ae60;");
    statusLabel.setText("Трек додано до плейлиста");
  }

  @FXML
  private void onRemoveTrackClick() {
    Playlist playlist = playlistsListView.getSelectionModel().getSelectedItem();
    Track track = playlistTracksListView.getSelectionModel().getSelectedItem();
    if (playlist == null || track == null) {
      AlertHelper.showError("Оберіть плейлист і трек у ньому");
      return;
    }
    context.getPlaylistService().removeTrackFromPlaylist(playlist.getId(), track.getId());
    refreshSelectedPlaylist();
  }

  @FXML
  private void onPlayClick() {
    Track selected = playlistTracksListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertHelper.showError("Оберіть трек для відтворення");
      return;
    }
    context.getPlayerService().play(selected.getFilePath(),
        totalDuration -> {},
        error -> Platform.runLater(() -> AlertHelper.showError("Не вдалося відтворити: " + error)));
  }

  @FXML
  private void onStopClick() {
    context.getPlayerService().stop();
  }

  @FXML
  private void onGoLibraryClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/main-view.fxml", "Музичний редактор — Бібліотека",
        new MainController(context, navigator));
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