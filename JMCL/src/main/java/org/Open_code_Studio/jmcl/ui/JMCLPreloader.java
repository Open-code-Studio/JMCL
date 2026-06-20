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

import javafx.animation.*;
import javafx.application.Preloader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.Open_code_Studio.jmcl.Metadata;

/// Preloader splash screen mimicking Android Studio startup style:
/// centered logo + version, with an indeterminate progress bar at bottom-right.
public final class JMCLPreloader extends Preloader {

    private Stage stage;
    private Label messageLabel;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        // --- Left panel: logo + version (Android Studio style) ---
        VBox leftPane = new VBox(12);
        leftPane.setAlignment(Pos.CENTER);
        leftPane.setPadding(new Insets(40, 60, 40, 60));

        // Logo
        ImageView logo = new ImageView(FXUtils.newBuiltinImage("/assets/img/jvm-mcl.png"));
        logo.setFitWidth(96);
        logo.setFitHeight(96);
        logo.setPreserveRatio(true);
        // Fade-in animation for logo
        FadeTransition logoFade = new FadeTransition(Duration.millis(800), logo);
        logoFade.setFromValue(0);
        logoFade.setToValue(1);
        logoFade.play();

        // App name
        Label nameLabel = new Label(Metadata.NAME);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        nameLabel.setTextFill(Color.web("#EEEEEE"));

        // Version
        Label versionLabel = new Label("v" + Metadata.VERSION);
        versionLabel.setFont(Font.font("System", 14));
        versionLabel.setTextFill(Color.web("#999999"));

        // Loading message
        messageLabel = new Label("Loading...");
        messageLabel.setFont(Font.font("System", 12));
        messageLabel.setTextFill(Color.web("#777777"));

        leftPane.getChildren().addAll(logo, nameLabel, versionLabel, messageLabel);

        // --- Bottom-right: indeterminate progress bar ---
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(160);
        progressBar.setMaxWidth(160);
        progressBar.setPrefHeight(4);
        progressBar.setMaxHeight(4);
        progressBar.setStyle(
            "-fx-accent: #4FC3F7;" +
            "-fx-background-color: #333333;" +
            "-fx-control-inner-background: #333333;"
        );
        // Indeterminate = the "small bar moving back and forth" effect
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        // Progress bar container at bottom-right
        HBox progressBox = new HBox(progressBar);
        progressBox.setAlignment(Pos.BOTTOM_RIGHT);
        progressBox.setPadding(new Insets(0, 24, 20, 0));

        // --- Combine layout ---
        BorderPane root = new BorderPane();
        root.setCenter(leftPane);
        root.setBottom(progressBox);
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(30, 30, 30), null, null)));

        Scene scene = new Scene(root, 520, 340);
        scene.setFill(Color.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            // Main app is ready — fade out and close
            if (stage != null) {
                FadeTransition fade = new FadeTransition(Duration.millis(400), stage.getScene().getRoot());
                fade.setFromValue(1);
                fade.setToValue(0);
                fade.setOnFinished(e -> stage.hide());
                fade.play();
            }
        }
    }

    @Override
    public void handleProgressNotification(ProgressNotification info) {
        // Map progress ranges to loading messages
        double p = info.getProgress();
        String msg;
        if (p < 0.2) {
            msg = "Loading configuration...";
        } else if (p < 0.4) {
            msg = "Initializing Java...";
        } else if (p < 0.6) {
            msg = "Building user interface...";
        } else if (p < 0.8) {
            msg = "Checking for updates...";
        } else {
            msg = "Almost ready...";
        }
        if (messageLabel != null) {
            messageLabel.setText(msg);
        }
    }
}
