/*
 * JMCL
 * Copyright (C) 2021  Open Code Studio and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.Open_code_Studio.jmcl.ui.main;

import com.google.gson.JsonObject;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPopup;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.download.DefaultDependencyManager;
import org.Open_code_Studio.jmcl.download.DownloadProvider;
import org.Open_code_Studio.jmcl.download.VersionList;
import org.Open_code_Studio.jmcl.download.game.GameRemoteVersionInfo;
import org.Open_code_Studio.jmcl.download.game.GameRemoteVersions;
import org.Open_code_Studio.jmcl.game.ReleaseType;
import org.Open_code_Studio.jmcl.game.Version;
import org.Open_code_Studio.jmcl.setting.DownloadProviders;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.Profiles;
import org.Open_code_Studio.jmcl.task.GetTask;
import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.theme.Themes;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.animation.AnimationUtils;
import org.Open_code_Studio.jmcl.ui.animation.ContainerAnimations;
import org.Open_code_Studio.jmcl.ui.animation.TransitionPane;
import org.Open_code_Studio.jmcl.ui.construct.MessageDialogPane;
import org.Open_code_Studio.jmcl.ui.construct.TwoLineListItem;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorPage;
import org.Open_code_Studio.jmcl.ui.versions.GameListPopupMenu;
import org.Open_code_Studio.jmcl.ui.versions.Versions;
import org.Open_code_Studio.jmcl.upgrade.RemoteVersion;
import org.Open_code_Studio.jmcl.upgrade.UpdateChecker;
import org.Open_code_Studio.jmcl.upgrade.UpdateHandler;
import org.Open_code_Studio.jmcl.util.*;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.Open_code_Studio.jmcl.util.i18n.I18n;
import org.Open_code_Studio.jmcl.util.javafx.BindingMapping;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.Open_code_Studio.jmcl.util.platform.Platform;
import org.Open_code_Studio.jmcl.util.versioning.GameVersionNumber;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import static org.Open_code_Studio.jmcl.download.RemoteVersion.Type.RELEASE;
import static org.Open_code_Studio.jmcl.setting.ConfigHolder.config;
import static org.Open_code_Studio.jmcl.ui.FXUtils.SINE;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public final class MainPage extends StackPane implements DecoratorPage {
    private static final String ANNOUNCEMENT = "announcement";
    private static final String MINECRAFT_CHANGELOG = "minecraft_changelog";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
 
     {
         getStyleClass().add("md3-main-page");
     }

    private final StringProperty currentGame = new SimpleStringProperty(this, "currentGame");
    private final BooleanProperty showUpdate = new SimpleBooleanProperty(this, "showUpdate");
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(this, "state");
    private final ObjectProperty<RemoteVersion> latestVersion = new SimpleObjectProperty<>(this, "latestVersion");
    private final ObservableList<Version> versions = FXCollections.observableArrayList();
    private Profile profile;

    private TransitionPane announcementPane;
    private final VBox announcementBox;
    private final ScrollPane announcementScrollPane;
    private final StackPane updatePane;
    private final JFXButton menuButton;

    {
        HBox titleNode = new HBox(8);
        titleNode.setPadding(new Insets(0, 0, 0, 2));
        titleNode.setAlignment(Pos.CENTER_LEFT);

        ImageView titleIcon = new ImageView(FXUtils.newBuiltinImage("/assets/img/icon-title.png"));
        Label titleLabel = new Label(Metadata.FULL_TITLE);
        if (I18n.isUpsideDown()) {
            titleIcon.setRotate(180);
            titleLabel.setRotate(180);
        }
        titleLabel.getStyleClass().add("jfx-decorator-title");
        titleLabel.textFillProperty().bind(Themes.titleFillProperty());
        titleNode.getChildren().setAll(titleIcon, titleLabel);

        state.setValue(new State(null, titleNode, false, false, true));

        setPadding(new Insets(20));
        FXUtils.setOverflowHidden(this);

        // ── Announcement area ─────────────────────────────────────────────
        announcementBox = new VBox(16);
        announcementBox.setPadding(new Insets(15));

        // Nightly / dev channel notice
        if (Metadata.isNightly() || (Metadata.isDev() && !Objects.equals(Metadata.VERSION, config().getShownTips().get(ANNOUNCEMENT)))) {
            String title;
            String content;
            if (Metadata.isNightly()) {
                title = i18n("update.channel.nightly.title");
                content = i18n("update.channel.nightly.hint");
            } else {
                title = i18n("update.channel.dev.title");
                content = i18n("update.channel.dev.hint");
            }

            VBox announcementCard = new VBox();

            BorderPane titleBar = new BorderPane();
            titleBar.getStyleClass().add("title");
            titleBar.setLeft(new Label(title));

            JFXButton btnHide = new JFXButton();
            btnHide.setOnAction(e -> {
                announcementBox.getChildren().remove(announcementCard);
                if (Metadata.isDev()) {
                    config().getShownTips().put(ANNOUNCEMENT, Metadata.VERSION);
                }
            });
            btnHide.getStyleClass().add("announcement-close-button");
            btnHide.setGraphic(SVG.CLOSE.createIcon(20));
            titleBar.setRight(btnHide);

            TextFlow body = FXUtils.segmentToTextFlow(content, Controllers::onHyperlinkAction);
            body.setLineSpacing(4);

            announcementCard.getChildren().setAll(titleBar, body);
            announcementCard.setSpacing(16);
            announcementCard.getStyleClass().addAll("card", "announcement", "elev-2");

            announcementBox.getChildren().add(announcementCard);
        }

        // Wrap the content in a scrollable pane
        announcementScrollPane = new ScrollPane(announcementBox);
        announcementScrollPane.setFitToWidth(true);
        announcementScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        announcementScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        announcementScrollPane.getStyleClass().add("announcement-scroll-pane");

        announcementPane = new TransitionPane();
        announcementPane.setContent(announcementScrollPane, ContainerAnimations.NONE);

        // Wrap in a plain Pane with explicit clip to absolutely constrain visible area.
        // A plain Pane does not reflow children based on its own size — it just clips
        // whatever overflows, which is exactly what we want.
        Pane announcementContainer = new Pane(announcementPane);
        // Width: fill entire available width. Right-side elements (launchPane,
        // updatePane) are overlays in the StackPane — they render on top and don't
        // need reserved layout space.
        announcementContainer.prefWidthProperty().bind(
                widthProperty()
        );
        announcementContainer.prefHeightProperty().bind(
                Bindings.max(heightProperty().subtract(120), 180.0)
        );
        announcementContainer.maxWidthProperty().bind(announcementContainer.prefWidthProperty());
        announcementContainer.maxHeightProperty().bind(announcementContainer.prefHeightProperty());
        FXUtils.setOverflowHidden(announcementContainer);
        // Make the inner TransitionPane fill the container
        announcementPane.prefWidthProperty().bind(announcementContainer.widthProperty());
        announcementPane.prefHeightProperty().bind(announcementContainer.heightProperty());
        StackPane.setAlignment(announcementContainer, Pos.TOP_LEFT);
        StackPane.setMargin(announcementContainer, new Insets(0));
        getChildren().add(announcementContainer);

        // Fetch Mojang changelog asynchronously (not blocking constructor)
        javafx.application.Platform.runLater(this::fetchMinecraftChangelogAnnouncement);

        updatePane = new StackPane();
        updatePane.setVisible(false);
        updatePane.getStyleClass().addAll("bubble", "card", "elev-1");
        FXUtils.setLimitWidth(updatePane, 230);
        FXUtils.setLimitHeight(updatePane, 55);
        StackPane.setAlignment(updatePane, Pos.TOP_RIGHT);
        FXUtils.onClicked(updatePane, this::onUpgrade);
        updatePane.setCursor(Cursor.HAND);
        FXUtils.onChange(showUpdateProperty(), this::showUpdate);

        {
            HBox hBox = new HBox();
            hBox.setSpacing(12);
            hBox.setAlignment(Pos.CENTER_LEFT);
            StackPane.setAlignment(hBox, Pos.CENTER_LEFT);
            StackPane.setMargin(hBox, new Insets(9, 12, 9, 16));
            {
                TwoLineListItem prompt = new TwoLineListItem();
                prompt.setSubtitle(i18n("update.bubble.subtitle"));
                prompt.setPickOnBounds(false);
                prompt.titleProperty().bind(BindingMapping.of(latestVersionProperty()).map(latestVersion ->
                        latestVersion == null ? "" : i18n("update.bubble.title", latestVersion.version())));

                hBox.getChildren().setAll(SVG.UPDATE.createIcon(20), prompt);
            }

            JFXButton closeUpdateButton = new JFXButton();
            closeUpdateButton.setGraphic(SVG.CLOSE.createIcon(10));
            StackPane.setAlignment(closeUpdateButton, Pos.TOP_RIGHT);
            closeUpdateButton.getStyleClass().add("toggle-icon-tiny");
            StackPane.setMargin(closeUpdateButton, new Insets(5));
            closeUpdateButton.setOnAction(e -> closeUpdateBubble());

            updatePane.getChildren().setAll(hBox, closeUpdateButton);
        }

        HBox launchPane = new HBox();
        launchPane.getStyleClass().add("launch-pane");
        FXUtils.onScroll(launchPane, versions, list -> {
            String currentId = getCurrentGame();
            return Lang.indexWhere(list, instance -> instance.getId().equals(currentId));
        }, it -> profile.setSelectedVersion(it.getId()));

        StackPane.setAlignment(launchPane, Pos.BOTTOM_RIGHT);
        {
            JFXButton launchButton = new JFXButton();
            launchButton.getStyleClass().addAll("launch-button", "md3-elevated-button");
            launchButton.setDefaultButton(true);
            {
                VBox graphic = new VBox();
                graphic.setAlignment(Pos.CENTER);
                Label launchLabel = new Label();
                launchLabel.setStyle("-fx-font-size: 16px;");
                Label currentLabel = new Label();
                currentLabel.setStyle("-fx-font-size: 12px;");

                FXUtils.onChangeAndOperate(currentGameProperty(), new Consumer<>() {
                    private Tooltip tooltip;

                    @Override
                    public void accept(String currentGame) {
                        if (currentGame == null) {
                            launchLabel.setText(i18n("version.launch.empty"));
                            currentLabel.setText(null);
                            graphic.getChildren().setAll(launchLabel);
                            FXUtils.setOnActionWithCooldown(launchButton, MainPage.this::launchNoGame);
                            if (tooltip == null)
                                tooltip = new Tooltip(i18n("version.launch.empty.tooltip"));
                            FXUtils.installFastTooltip(launchButton, tooltip);
                        } else {
                            launchLabel.setText(i18n("version.launch"));
                            currentLabel.setText(currentGame);
                            graphic.getChildren().setAll(launchLabel, currentLabel);
                            FXUtils.setOnActionWithCooldown(launchButton, MainPage.this::launch);
                            if (tooltip != null)
                                Tooltip.uninstall(launchButton, tooltip);
                        }
                    }
                });

                launchButton.setGraphic(graphic);
            }

            menuButton = new JFXButton();
            menuButton.getStyleClass().addAll("menu-button", "md3-icon-button");
            menuButton.setOnAction(e -> GameListPopupMenu.show(
                    menuButton,
                    JFXPopup.PopupVPosition.BOTTOM,
                    JFXPopup.PopupHPosition.RIGHT,
                    0,
                    -menuButton.getHeight(),
                    profile, versions
            ));
            FXUtils.installFastTooltip(menuButton, i18n("version.switch"));
            menuButton.setGraphic(SVG.ARROW_DROP_UP.createIcon(30));

            EventHandler<MouseEvent> secondaryClickHandle = event -> {
                if (event.getButton() == MouseButton.SECONDARY && event.getClickCount() == 1) {
                    menuButton.fire();
                    event.consume();
                }
            };
            launchButton.addEventHandler(MouseEvent.MOUSE_CLICKED, secondaryClickHandle);
            menuButton.addEventHandler(MouseEvent.MOUSE_CLICKED, secondaryClickHandle);

            launchPane.getChildren().setAll(launchButton, menuButton);
        }

        getChildren().addAll(updatePane, launchPane);

    }

    private void showUpdate(boolean show) {
        doAnimation(show);

        if (show && !config().isDisableAutoShowUpdateDialog()
                && getLatestVersion() != null
                && !Objects.equals(config().getPromptedVersion(), getLatestVersion().version())) {
            Controllers.dialog(new MessageDialogPane.Builder("", i18n("update.bubble.title", getLatestVersion().version()), MessageDialogPane.MessageType.INFO)
                    .addAction(i18n("button.view"), () -> {
                        config().setPromptedVersion(getLatestVersion().version());
                        onUpgrade();
                    })
                    .addCancel(null)
                    .build());
        }
    }

    private void doAnimation(boolean show) {
        if (AnimationUtils.isAnimationEnabled()) {
            Duration duration = Duration.millis(320);
            Timeline nowAnimation = new Timeline();
            nowAnimation.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(updatePane.translateXProperty(), show ? 260 : 0, SINE)),
                    new KeyFrame(duration,
                            new KeyValue(updatePane.translateXProperty(), show ? 0 : 260, SINE)));
            if (show) nowAnimation.getKeyFrames().add(
                    new KeyFrame(Duration.ZERO, e -> updatePane.setVisible(true)));
            else nowAnimation.getKeyFrames().add(
                    new KeyFrame(duration, e -> updatePane.setVisible(false)));
            nowAnimation.play();
        } else {
            updatePane.setVisible(show);
        }
    }

    private void launch() {
        Profile profile = Profiles.getSelectedProfile();
        Versions.launch(profile, profile.getSelectedVersion());
    }

    private void launchNoGame() {
        DownloadProvider downloadProvider = DownloadProviders.getDownloadProvider();
        VersionList<?> versionList = downloadProvider.getVersionListById("game");

        Holder<String> gameVersionHolder = new Holder<>();
        Task<?> task = versionList.refreshAsync("")
                .thenSupplyAsync(() -> versionList.getVersions("").stream()
                        .filter(it -> it.getVersionType() == RELEASE)
                        .filter(it -> NativePatcher.checkSupportedStatus(GameVersionNumber.asGameVersion(it.getGameVersion()), Platform.SYSTEM_PLATFORM, OperatingSystem.SYSTEM_VERSION) != NativePatcher.SupportStatus.UNSUPPORTED)
                        .sorted()
                        .findFirst()
                        .orElseThrow(() -> new IOException("No versions found")))
                .thenComposeAsync(version -> {
                    Profile profile = Profiles.getSelectedProfile();
                    DefaultDependencyManager dependency = profile.getDependency();
                    String gameVersion = gameVersionHolder.value = version.getGameVersion();

                    return dependency.gameBuilder()
                            .name(gameVersion)
                            .gameVersion(gameVersion)
                            .buildAsync();
                })
                .whenComplete(any -> profile.getRepository().refreshVersions())
                .whenComplete(Schedulers.javafx(), (result, exception) -> {
                    if (exception == null) {
                        profile.setSelectedVersion(gameVersionHolder.value);
                        launch();
                    } else if (exception instanceof CancellationException) {
                        Controllers.showToast(i18n("message.cancelled"));
                    } else {
                        LOG.warning("Failed to install game", exception);
                        Controllers.dialog(StringUtils.getStackTrace(exception),
                                i18n("install.failed"),
                                MessageDialogPane.MessageType.WARNING);
                    }
                });
        Controllers.taskDialog(task, i18n("version.launch.empty.installing"), TaskCancellationAction.NORMAL);
    }

    private void onUpgrade() {
        RemoteVersion target = UpdateChecker.getLatestVersion();
        if (target == null) {
            return;
        }
        UpdateHandler.updateFrom(target);
    }

    private void closeUpdateBubble() {
        showUpdate.unbind();
        showUpdate.set(false);
    }

    @Override
    public ReadOnlyObjectWrapper<State> stateProperty() {
        return state;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getCurrentGame() {
        return currentGame.get();
    }

    public StringProperty currentGameProperty() {
        return currentGame;
    }

    public void setCurrentGame(String currentGame) {
        this.currentGame.set(currentGame);
    }

    public ObservableList<Version> getVersions() {
        return versions;
    }

    public boolean isShowUpdate() {
        return showUpdate.get();
    }

    public BooleanProperty showUpdateProperty() {
        return showUpdate;
    }

    public void setShowUpdate(boolean showUpdate) {
        this.showUpdate.set(showUpdate);
    }

    public RemoteVersion getLatestVersion() {
        return latestVersion.get();
    }

    public ObjectProperty<RemoteVersion> latestVersionProperty() {
        return latestVersion;
    }

    public void setLatestVersion(RemoteVersion latestVersion) {
        this.latestVersion.set(latestVersion);
    }

    public void initVersions(Profile profile, List<Version> versions) {
        FXUtils.checkFxUserThread();
        this.profile = profile;
        this.versions.setAll(versions);
    }

    // ── Mojang changelog announcement ─────────────────────────────────────

    /// Asynchronously fetches the Mojang version manifest and shows a changelog
    /// announcement card on the home page if a new release has been detected.
    /// Also attempts to fetch the version's wiki page image.
    private void fetchMinecraftChangelogAnnouncement() {
        // Try to use pre-fetched cache (loaded in background during splash)
        var cached = ChangelogPrefetcher.getCachedData();
        if (cached.isDone()) {
            ChangelogPrefetcher.ChangelogData data = cached.getNow(null);
            if (data != null) {
                String versionId = data.version().gameVersion();
                if (!Metadata.isDev()) {
                    String lastShown = (String) config().getShownTips().get(MINECRAFT_CHANGELOG);
                    if (versionId.equals(lastShown)) return;
                }
                createChangelogCard(data.version(), data.imageUrl());
                return;
            }
        }
        // Fallback: fetch live
        doFetchChangelogLive();
    }

    private void doFetchChangelogLive() {
        new GetTask(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest.json"))
                .thenGetJsonAsync(GameRemoteVersions.class)
                .whenComplete(Schedulers.javafx(), (versions, exception) -> {
                    if (exception != null || versions == null) {
                        LOG.warning("Failed to fetch Mojang version manifest", exception);
                        return;
                    }

                    // Find the latest release version (sorted by release time)
                    GameRemoteVersionInfo latestRelease = null;
                    for (GameRemoteVersionInfo v : versions.versions()) {
                        if (v.type() == ReleaseType.RELEASE) {
                            if (latestRelease == null || v.releaseTime().isAfter(latestRelease.releaseTime())) {
                                latestRelease = v;
                            }
                        }
                    }
                    if (latestRelease == null) return;

                    String versionId = latestRelease.gameVersion();
                    // In dev mode, always show the changelog regardless of shownTips cache
                    if (!Metadata.isDev()) {
                        String lastShown = (String) config().getShownTips().get(MINECRAFT_CHANGELOG);
                        if (versionId.equals(lastShown)) return;
                    }

                    GameRemoteVersionInfo finalVersion = latestRelease;

                    // Fetch the wiki banner image for this version
                    String wikiImageUrl = "https://minecraft.wiki/api.php?action=query"
                            + "&titles=Java_Edition_" + versionId
                            + "&prop=pageimages&format=json&pithumbsize=960";

                    new GetTask(URI.create(wikiImageUrl))
                            .whenComplete(Schedulers.javafx(), (json, imgException) -> {
                                String imageUrl = null;
                                if (imgException == null && json != null) {
                                    try {
                                        JsonObject root = JsonUtils.fromNonNullJson(json, JsonObject.class);
                                        JsonObject query = root.getAsJsonObject("query");
                                        if (query != null) {
                                            JsonObject pages = query.getAsJsonObject("pages");
                                            if (pages != null) {
                                                for (var entry : pages.entrySet()) {
                                                    JsonObject page = entry.getValue().getAsJsonObject();
                                                    if (page != null && page.has("thumbnail")) {
                                                        imageUrl = page.getAsJsonObject("thumbnail")
                                                                .get("source").getAsString();
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        LOG.warning("Failed to parse wiki image for " + versionId, e);
                                    }
                                }
                                createChangelogCard(finalVersion, imageUrl);
                            }).start();
                }).start();
    }

    /// Creates and adds a changelog announcement card for a Mojang game version.
    /// The card is added to the scrollable announcement area alongside existing content.
    /// @param imageUrl optional wiki banner image URL; if null, a programmatic gradient banner is used
    private void createChangelogCard(GameRemoteVersionInfo version, @Nullable String imageUrl) {
        String versionId = version.gameVersion();
        String typeName = i18n("version.game.release");
        String dateStr = DATE_FORMATTER.format(version.releaseTime());
        String wikiUrl = "https://minecraft.wiki/w/Java_Edition_" + versionId;

        VBox card = new VBox();

        // ── Banner (wiki image if available, otherwise gradient fallback) ──
        if (imageUrl != null) {
            ImageView imageView = new ImageView(imageUrl);
            imageView.setPreserveRatio(true);
            imageView.fitWidthProperty().bind(card.widthProperty());
            imageView.setFitHeight(160);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            clip.widthProperty().bind(imageView.fitWidthProperty());
            clip.heightProperty().bind(imageView.fitHeightProperty());
            imageView.setClip(clip);
            card.getChildren().add(imageView);
        } else {
            // Fallback: programmatic Minecraft-themed gradient banner
            StackPane banner = new StackPane();
            banner.setPrefHeight(120);
            banner.setMinHeight(120);
            banner.setMaxHeight(120);
            banner.setStyle("-fx-background-color: linear-gradient(to bottom, "
                    + "#5a8f3f 0%, #7a8f3b 30%, #8b6f3b 60%, #6b4a2a 100%);");
            Label bannerTitle = new Label(i18n("minecraft.changelog.title", versionId));
            bannerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
            StackPane.setAlignment(bannerTitle, Pos.BOTTOM_LEFT);
            StackPane.setMargin(bannerTitle, new Insets(0, 0, 16, 20));
            banner.getChildren().add(bannerTitle);
            javafx.scene.shape.Rectangle bannerClip = new javafx.scene.shape.Rectangle();
            bannerClip.setArcWidth(16);
            bannerClip.setArcHeight(16);
            bannerClip.widthProperty().bind(banner.widthProperty());
            bannerClip.heightProperty().bind(banner.heightProperty());
            banner.setClip(bannerClip);
            card.getChildren().add(banner);
        }

        // ── Title bar ──────────────────────────────────────────────────────
        BorderPane titleBar = new BorderPane();
        titleBar.getStyleClass().add("title");
        titleBar.setLeft(new Label(i18n("minecraft.changelog.title", versionId)));

        JFXButton btnHide = new JFXButton();
        btnHide.setOnAction(e -> {
            announcementBox.getChildren().remove(card);
            // In dev mode, don't persist so changelog shows on next restart
            if (!Metadata.isDev()) {
                config().getShownTips().put(MINECRAFT_CHANGELOG, versionId);
            }
        });
        btnHide.getStyleClass().add("announcement-close-button");
        btnHide.setGraphic(SVG.CLOSE.createIcon(20));
        titleBar.setRight(btnHide);

        String content = String.format(
                "<b>Minecraft %s</b><br/>%s | %s<br/><a href=\"%s\">%s →</a>",
                versionId, typeName, dateStr, wikiUrl, i18n("minecraft.changelog.view"));
        TextFlow body = FXUtils.segmentToTextFlow(content, Controllers::onHyperlinkAction);
        body.setLineSpacing(4);

        card.getChildren().addAll(titleBar, body);
        card.setSpacing(16);
        card.getStyleClass().addAll("card", "announcement", "elev-2");

        // Add the changelog card to the persistent announcement box
        announcementBox.getChildren().add(card);
    }
}
