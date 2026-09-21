package com.perebziak.musiceditor.core;

import com.perebziak.musiceditor.db.ConnectionPool;
import com.perebziak.musiceditor.db.DatabaseManager;
import com.perebziak.musiceditor.repository.*;
import com.perebziak.musiceditor.service.*;
import com.perebziak.musiceditor.util.AppConfig;
import com.perebziak.musiceditor.util.AudioStorage;
import com.perebziak.musiceditor.util.PasswordHasher;
import com.perebziak.musiceditor.util.SessionManager;
import com.perebziak.musiceditor.util.ValidationUtil;

import lombok.Getter;

@Getter
public class AppContext {

  private static AppContext instance;

  private final ConnectionPool connectionPool;
  private final DatabaseManager databaseManager;

  private final GenreRepository genreRepository;
  private final ArtistRepository artistRepository;
  private final AlbumRepository albumRepository;
  private final TrackRepository trackRepository;
  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final VerificationCodeRepository verificationCodeRepository;
  private final ConversionOrderRepository conversionOrderRepository;

  private final AppConfig appConfig;
  private final EmailService emailService;
  private final PasswordHasher passwordHasher;
  private final ValidationUtil validationUtil;

  private final AuthService authService;
  private final SessionManager sessionManager;

  private final AudioStorage audioStorage;
  private final LibraryService libraryService;
  private final PlayerService playerService;

  private final FfmpegService ffmpegService;
  private final PaymentService paymentService;
  private final ConversionService conversionService;
  private final AudioEditService audioEditService;
  private final AdminService adminService;
  private final PlaylistService playlistService;

  private AppContext() {
    this.connectionPool = ConnectionPool.getInstance();
    this.databaseManager = new DatabaseManager(connectionPool);
    databaseManager.initializeIfNeeded();

    this.genreRepository = new SqliteGenreRepository(connectionPool);
    this.artistRepository = new SqliteArtistRepository(connectionPool);
    this.albumRepository = new SqliteAlbumRepository(connectionPool);
    this.trackRepository = new SqliteTrackRepository(connectionPool, genreRepository);
    this.playlistRepository = new SqlitePlaylistRepository(connectionPool, trackRepository);
    this.userRepository = new SqliteUserRepository(connectionPool);
    this.verificationCodeRepository = new SqliteVerificationCodeRepository(connectionPool);
    this.conversionOrderRepository = new SqliteConversionOrderRepository(connectionPool);

    this.appConfig = new AppConfig();
    this.emailService = new EmailService(appConfig);
    this.passwordHasher = new PasswordHasher();
    this.validationUtil = new ValidationUtil();

    this.authService = new AuthService(userRepository, verificationCodeRepository,
        emailService, passwordHasher, validationUtil);

    this.sessionManager = SessionManager.getInstance();

    this.audioStorage = new AudioStorage();
    this.libraryService = new LibraryService(trackRepository, audioStorage);
    this.playerService = new PlayerService();

    this.ffmpegService = new FfmpegService();
    this.paymentService = new PaymentService();
    this.conversionService = new ConversionService(
        conversionOrderRepository, trackRepository, ffmpegService, paymentService);
    this.audioEditService = new AudioEditService(trackRepository, ffmpegService);
    this.adminService = new AdminService(userRepository);
    this.playlistService = new PlaylistService(playlistRepository);
  }

  public static synchronized AppContext getInstance() {
    if (instance == null) {
      instance = new AppContext();
    }
    return instance;
  }
}