/*
 * JMCL
 * Copyright (C) 2026 OCS
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
package org.Open_code_Studio.jmcl.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.ui.main.ChangelogPrefetcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/// Preloader splash screen:
/// - Rounded dark rectangle window
/// - JMCL icon on the left
/// - Version text "JMCL-{VERSION}" at top-left
/// - Indeterminate progress bar at bottom-right
/// - Loading status text below icon
public final class JMCLPreloader extends Preloader {

    private static final int WIDTH = 560;
    private static final int HEIGHT = 320;
    private static final int RADIUS = 16;
    private static final long MIN_DISPLAY_MS = 3000;

    /// Completes when the main window can show (behind preloader, minimized).
    private static final CompletableFuture<Void> READY = new CompletableFuture<>();
    /// Completes when changelog is loaded and preloader should close.
    private static final CompletableFuture<Void> RESTORE = new CompletableFuture<>();

    public static CompletableFuture<Void> readyFuture() { return READY; }
    public static CompletableFuture<Void> restoreFuture() { return RESTORE; }

    /// Called by MainPage when the changelog card has been rendered in the UI.
    public static void onChangelogRendered() {
        RESTORE.complete(null);
    }

    private Stage stage;
    private Label statusLabel;
    private long startTime;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.startTime = System.currentTimeMillis();
        Platform.setImplicitExit(false);

        // Close JVM native splash (-splash:) if still showing
        try {
            java.awt.SplashScreen splash = java.awt.SplashScreen.getSplashScreen();
            if (splash != null && splash.isVisible()) splash.close();
        } catch (Throwable ignored) {}

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setAlwaysOnTop(true);

        // === Top-left: version label ===
        Label versionLabel = new Label("JMCL-" + Metadata.VERSION);
        versionLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        versionLabel.setTextFill(Color.web("#AAAAAA"));
        versionLabel.setPadding(new Insets(20, 0, 0, 28));

        // === Left: logo ===
        ImageView logo = new ImageView(FXUtils.newBuiltinImage("/assets/img/jvm-mcl.png"));
        logo.setFitWidth(64);
        logo.setFitHeight(64);
        logo.setPreserveRatio(true);
        FadeTransition logoFade = new FadeTransition(Duration.millis(600), logo);
        logoFade.setFromValue(0);
        logoFade.setToValue(1);
        logoFade.play();

        // === Status text below logo ===
        statusLabel = new Label("Loading...");
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setTextFill(Color.web("#888888"));

        VBox leftPane = new VBox(8, logo, statusLabel);
        leftPane.setAlignment(Pos.CENTER_LEFT);
        leftPane.setPadding(new Insets(0, 0, 0, 28));

        // === Bottom-right: indeterminate progress bar (MD3 linear style, matching JMCL) ===
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(140);
        progressBar.setMaxWidth(140);
        progressBar.setPrefHeight(6);
        progressBar.setMaxHeight(6);
        progressBar.getStyleClass().add("md3-linear-progress");
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        HBox progressBox = new HBox(progressBar);
        progressBox.setAlignment(Pos.BOTTOM_RIGHT);
        progressBox.setPadding(new Insets(0, 28, 24, 0));

        // === Layout ===
        BorderPane root = new BorderPane();
        root.setTop(versionLabel);
        root.setCenter(leftPane);
        root.setBottom(progressBox);
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(28, 28, 30), CornerRadii.EMPTY, Insets.EMPTY)));
        root.setStyle("-fx-background-radius: " + RADIUS + ";");

        // Clip for rounded corners (UNDECORATED stages need manual clipping)
        Rectangle clip = new Rectangle(WIDTH, HEIGHT);
        clip.setArcWidth(RADIUS * 2);
        clip.setArcHeight(RADIUS * 2);
        root.setClip(clip);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        // Inline MD3 linear progress style (matches JMCL's .progress-bar.md3-linear-progress)
        scene.getStylesheets().add("data:text/css," +
            ".progress-bar.md3-linear-progress > .track {" +
            "  -fx-background-color: #3A3A3D;" +
            "  -fx-background-radius: 4px;" +
            "  -fx-background-insets: 0;" +
            "  -fx-padding: 0;" +
            "}" +
            ".progress-bar.md3-linear-progress > .bar {" +
            "  -fx-background-color: #4FC3F7;" +
            "  -fx-background-radius: 4px;" +
            "  -fx-padding: 3px;" +
            "}"
        );
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            // Signal: main window can show (behind preloader, minimized)
            READY.complete(null);

            // Wait for both: min display time AND changelog, then signal restore
            long elapsed = System.currentTimeMillis() - startTime;
            long minDelay = Math.max(0, MIN_DISPLAY_MS - elapsed);
            CompletableFuture<Void> minWait = CompletableFuture.runAsync(() -> {
                try { Thread.sleep(minDelay); } catch (InterruptedException ignored) {}
            });
            CompletableFuture<?> changelog = ChangelogPrefetcher.getCachedData()
                    .orTimeout(8, TimeUnit.SECONDS).exceptionally(ex -> null);

            CompletableFuture.allOf(minWait, changelog).thenRunAsync(() -> {
                if (stage != null) {
                    stage.setAlwaysOnTop(false);
                    stage.hide();
                }
                // RESTORE is completed by MainPage.onChangelogRendered() after card appears
            }, Platform::runLater);
        }
    }

    @Override
    public void handleProgressNotification(ProgressNotification info) {
        double p = info.getProgress();
        String msg;
        if (p < 0.2) {
            msg = "Loading configuration...";
        } else if (p < 0.4) {
            msg = "Initializing Java...";
        } else if (p < 0.6) {
            msg = "Building interface...";
        } else if (p < 0.8) {
            msg = "Checking updates...";
        } else {
            msg = "Almost ready...";
        }
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(msg));
        }
    }
}
