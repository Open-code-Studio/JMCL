/*
 * JMCL
 * Copyright (C) 2026  OCS
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
package org.Open_code_Studio.jmcl.ui.construct;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.SVGContainer;
import org.Open_code_Studio.jmcl.setting.StyleSheets;
import org.Open_code_Studio.jmcl.util.Log4jLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.Open_code_Studio.jmcl.util.Lang.thread;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

/**
 * Dialog for viewing task installation logs, styled like {@code LogWindow}.
 */
public final class TaskLogDialog extends Stage {

    private static final Log4jLevel[] LEVELS = {Log4jLevel.FATAL, Log4jLevel.ERROR, Log4jLevel.WARN, Log4jLevel.INFO, Log4jLevel.DEBUG};
    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile("\\[[^]]+/(" + String.join("|", Arrays.stream(LEVELS).map(Enum::name).toArray(String[]::new)) + ")\\]");

    private final ObservableList<LogEntry> allEntries = FXCollections.observableArrayList();
    private final Map<Log4jLevel, SimpleIntegerProperty> levelCountMap = new EnumMap<>(Log4jLevel.class);
    private final BooleanProperty[] showLevel = new BooleanProperty[LEVELS.length];
    private final StringProperty[] buttonText = new StringProperty[LEVELS.length];

    private final ListView<LogEntry> listView = new ListView<>();
    private final BooleanProperty autoScroll = new SimpleBooleanProperty(true);

    private final Timeline pollTimer;
    private int lastLogLength = 0;

    public TaskLogDialog() {
        setTitle(i18n("log.viewer"));
        initModality(Modality.NONE);
        initStyle(StageStyle.UNDECORATED);

        // Initialize level maps
        for (Log4jLevel level : Log4jLevel.values()) {
            levelCountMap.put(level, new SimpleIntegerProperty());
        }

        // ── Level toggle button text bindings ──
        for (int i = 0; i < LEVELS.length; i++) {
            showLevel[i] = new SimpleBooleanProperty(true);
            buttonText[i] = new SimpleStringProperty();
            buttonText[i].bind(Bindings.concat(levelCountMap.get(LEVELS[i]), " ", LEVELS[i].name().toLowerCase(Locale.ROOT)));
        }

        // ── Layout ──
        BorderPane root = new BorderPane();
        root.getStyleClass().add("task-log-window");

        // --- Title bar ---
        Label titleLabel = new Label(i18n("log.viewer"));
        titleLabel.getStyleClass().add("task-log-title");

        SVGContainer closeIcon = SVG.CLOSE.createIcon(18);
        closeIcon.getStyleClass().add("task-log-close-icon");

        StackPane closeButton = new StackPane(closeIcon);
        closeButton.getStyleClass().add("task-log-close-button");
        closeButton.setCursor(Cursor.HAND);
        closeButton.setOnMouseClicked(e -> close());

        HBox titleBar = new HBox(titleLabel, closeButton);
        titleBar.getStyleClass().add("task-log-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Window dragging via title bar
        final double[] dragOffset = new double[2];
        titleBar.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX();
            dragOffset[1] = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            setX(e.getScreenX() - dragOffset[0]);
            setY(e.getScreenY() - dragOffset[1]);
        });

        root.setTop(titleBar);

        // --- Center: ListView ---
        VBox centerBox = new VBox(3);
        centerBox.setPadding(new Insets(0, 0, 3, 0));

        // Level toggle bar
        HBox toggleBar = new HBox(3);
        toggleBar.setPadding(new Insets(3, 8, 3, 8));
        toggleBar.setAlignment(Pos.CENTER_RIGHT);
        for (int i = 0; i < LEVELS.length; i++) {
            ToggleButton button = new ToggleButton();
            button.getStyleClass().addAll("log-toggle", LEVELS[i].name().toLowerCase(Locale.ROOT));
            button.textProperty().bind(buttonText[i]);
            button.setSelected(true);
            showLevel[i].bind(button.selectedProperty());
            toggleBar.getChildren().add(button);
        }
        centerBox.getChildren().add(toggleBar);

        // ListView
        listView.getStyleClass().add("no-horizontal-scrollbar");
        listView.getProperties().put("no-smooth-scrolling", true);
        listView.setItems(FXCollections.observableArrayList());
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setStyle("-fx-font-family: \"'Menlo', 'Monaco', 'Courier New', monospace\"; -fx-font-size: 11px;");

        listView.getItems().addListener((InvalidationListener) observable -> {
            if (!listView.getItems().isEmpty() && autoScroll.get())
                listView.scrollTo(listView.getItems().size() - 1);
        });

        listView.setCellFactory(x -> new ListCell<>() {
            private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");
            private static final PseudoClass FATAL = PseudoClass.getPseudoClass("fatal");
            private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
            private static final PseudoClass WARN = PseudoClass.getPseudoClass("warn");
            private static final PseudoClass INFO = PseudoClass.getPseudoClass("info");
            private static final PseudoClass DEBUG = PseudoClass.getPseudoClass("debug");

            {
                getStyleClass().add("log-window-list-cell");
                setPadding(new Insets(2));
                setWrapText(true);
                setGraphic(null);
            }

            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);

                pseudoClassStateChanged(EMPTY, empty);
                if (item != null && !empty) {
                    pseudoClassStateChanged(FATAL, item.level() == Log4jLevel.FATAL);
                    pseudoClassStateChanged(ERROR, item.level() == Log4jLevel.ERROR);
                    pseudoClassStateChanged(WARN, item.level() == Log4jLevel.WARN);
                    pseudoClassStateChanged(INFO, item.level() == Log4jLevel.INFO);
                    pseudoClassStateChanged(DEBUG, item.level() == Log4jLevel.DEBUG);
                } else {
                    pseudoClassStateChanged(FATAL, false);
                    pseudoClassStateChanged(ERROR, false);
                    pseudoClassStateChanged(WARN, false);
                    pseudoClassStateChanged(INFO, false);
                    pseudoClassStateChanged(DEBUG, false);
                }

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.text());
                }
            }
        });

        listView.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                if (listView.getSelectionModel().isEmpty())
                    return;
                StringBuilder sb = new StringBuilder();
                for (LogEntry item : listView.getSelectionModel().getSelectedItems()) {
                    if (item != null) {
                        if (item.text() != null)
                            sb.append(item.text());
                        sb.append('\n');
                    }
                }
                FXUtils.copyText(sb.toString(), null);
            }
        });

        VBox.setVgrow(listView, Priority.ALWAYS);
        centerBox.getChildren().add(listView);
        root.setCenter(centerBox);

        // --- Bottom: auto-scroll, export, clear ---
        BorderPane bottomBar = new BorderPane();
        bottomBar.setPadding(new Insets(0, 8, 8, 8));

        CheckBox autoScrollCheckBox = new CheckBox(i18n("logwindow.autoscroll"));
        autoScrollCheckBox.setSelected(true);
        autoScroll.bind(autoScrollCheckBox.selectedProperty());

        HBox rightBox = new HBox(6);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Button exportButton = new Button(i18n("button.export"));
        exportButton.setOnAction(e -> onExportLogs());

        Button clearButton = new Button(i18n("button.clear"));
        clearButton.setOnAction(e -> onClear());

        rightBox.getChildren().setAll(exportButton, clearButton);
        bottomBar.setLeft(autoScrollCheckBox);
        bottomBar.setRight(rightBox);

        root.setBottom(bottomBar);

        // Rounded corners clip
        Rectangle clip = new Rectangle();
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        Scene scene = new Scene(root, 800, 480);
        scene.setFill(null);
        StyleSheets.init(scene);
        setScene(scene);

        FXUtils.setIcon(this);

        // Level filter listeners
        for (int i = 0; i < LEVELS.length; i++) {
            int idx = i;
            showLevel[i].addListener(o -> shakeLogs());
        }

        // Polling timer
        pollTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> pollLogs()));
        pollTimer.setCycleCount(Timeline.INDEFINITE);
    }

    public void startPolling() {
        pollTimer.play();
    }

    public void stopPolling() {
        pollTimer.stop();
    }

    private void pollLogs() {
        String currentLogs = LOG.getLogs();
        if (currentLogs.isEmpty() && lastLogLength > 0) {
            // Log was cleared
            allEntries.clear();
            listView.getItems().clear();
            for (Log4jLevel level : Log4jLevel.values()) {
                levelCountMap.get(level).set(0);
            }
            lastLogLength = 0;
            return;
        }
        if (currentLogs.length() <= lastLogLength)
            return;

        String newContent = currentLogs.substring(lastLogLength);
        lastLogLength = currentLogs.length();

        // Parse new lines
        String[] lines = newContent.split("\n", -1);
        Log4jLevel lastLevel = null;
        for (String line : lines) {
            if (line.isEmpty()) continue;
            Log4jLevel level = parseLevel(line);
            if (level == null)
                level = lastLevel;
            else
                lastLevel = level;

            LogEntry entry = new LogEntry(line, level);
            allEntries.add(entry);

            // Update count for the level
            if (level != null) {
                SimpleIntegerProperty count = levelCountMap.get(level);
                if (count != null)
                    count.set(count.get() + 1);
            }

            // Add to visible list if level is shown
            if (level != null) {
                int idx = Arrays.asList(LEVELS).indexOf(level);
                if (idx >= 0 && showLevel[idx].get()) {
                    listView.getItems().add(entry);
                }
            } else {
                // Show entries with unknown level too
                listView.getItems().add(entry);
            }
        }

        // Auto-scroll
        if (!listView.getItems().isEmpty() && autoScroll.get())
            listView.scrollTo(listView.getItems().size() - 1);
    }

    private void shakeLogs() {
        listView.getItems().setAll(
                allEntries.stream()
                        .filter(entry -> {
                            if (entry.level() == null) return true;
                            int idx = Arrays.asList(LEVELS).indexOf(entry.level());
                            return idx >= 0 && showLevel[idx].get();
                        })
                        .collect(Collectors.toList())
        );
        if (!listView.getItems().isEmpty() && autoScroll.get())
            listView.scrollTo(listView.getItems().size() - 1);
    }

    private void onExportLogs() {
        thread(() -> {
            Path logFile = Paths.get("task-exported-logs-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")) + ".log").toAbsolutePath();
            try {
                Files.write(logFile, allEntries.stream().map(LogEntry::text).collect(Collectors.toList()));
            } catch (IOException e) {
                LOG.warning("Failed to export task logs", e);
                return;
            }
            FXUtils.showFileInExplorer(logFile);
        });
    }

    private void onClear() {
        listView.getItems().clear();
        allEntries.clear();
        for (Log4jLevel level : Log4jLevel.values()) {
            levelCountMap.get(level).set(0);
        }
        lastLogLength = 0;
    }

    @Override
    public void close() {
        stopPolling();
        super.close();
    }

    private static Log4jLevel parseLevel(String line) {
        Matcher m = LOG_LEVEL_PATTERN.matcher(line);
        if (m.find()) {
            return switch (m.group(1)) {
                case "FATAL" -> Log4jLevel.FATAL;
                case "ERROR" -> Log4jLevel.ERROR;
                case "WARN" -> Log4jLevel.WARN;
                case "INFO" -> Log4jLevel.INFO;
                case "DEBUG" -> Log4jLevel.DEBUG;
                default -> null;
            };
        }
        // Fallback: try guessLevel for Minecraft-style logs or other formats
        return Log4jLevel.guessLevel(line);
    }

    private record LogEntry(String text, Log4jLevel level) {
    }
}