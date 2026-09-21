package com.perebziak.musiceditor.service;

import com.perebziak.musiceditor.exception.ValidationException;
import com.perebziak.musiceditor.model.Playlist;
import com.perebziak.musiceditor.model.Track;
import com.perebziak.musiceditor.repository.PlaylistRepository;

import java.time.LocalDateTime;
import java.util.List;

public class PlaylistService {

  private final PlaylistRepository playlistRepository;

  public PlaylistService(PlaylistRepository playlistRepository) {
    this.playlistRepository = playlistRepository;
  }

  public List<Playlist> getUserPlaylists(Long userId) {
    return playlistRepository.findByUserId(userId);
  }

  public Playlist createPlaylist(Long userId, String name) {
    if (name == null || name.isBlank()) {
      throw new ValidationException("Назва плейлиста не може бути порожньою");
    }
    Playlist playlist = new Playlist();
    playlist.setName(name.trim());
    playlist.setUserId(userId);
    playlist.setCreatedDate(LocalDateTime.now());
    return playlistRepository.save(playlist);
  }

  public void deletePlaylist(Long playlistId) {
    playlistRepository.delete(playlistId);
  }

  public List<Track> getTracks(Long playlistId) {
    return playlistRepository.getTracks(playlistId);
  }

  public void addTrackToPlaylist(Long playlistId, Long trackId) {
    int nextPosition = playlistRepository.getTracks(playlistId).size() + 1;
    playlistRepository.addTrack(playlistId, trackId, nextPosition);
  }

  public void removeTrackFromPlaylist(Long playlistId, Long trackId) {
    playlistRepository.removeTrack(playlistId, trackId);
  }
}