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
package org.Open_code_Studio.jmcl.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.Open_code_Studio.jmcl.game.JMCLGameRepository;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.StyleSheets;
import org.Open_code_Studio.jmcl.ui.construct.MessageDialogPane;
import org.Open_code_Studio.jmcl.util.Lang;
import org.Open_code_Studio.jmcl.util.TaskCancellationAction;
import org.Open_code_Studio.jmcl.util.platform.ManagedProcess;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/// A small undecorated window that manages a running Minecraft game instance.
///
/// It tracks the Minecraft game window position and appears next to it,
/// providing quick access to stop the game, open its folder, and view mods.
public final class InstanceManagerWindow extends Stage {

    /// The managed game process.
    private final ManagedProcess process;

    /// The profile associated with this instance.
    private final Profile profile;

    /// The version ID of this instance.
    private final String versionId;

    /// The game repository for accessing game directories.
    private final JMCLGameRepository repository;

    /// Thread monitoring the game process for exit detection.
    private @Nullable Thread monitorThread;

    /// Thread tracking the game window position via osascript.
    private @Nullable Thread trackerThread;

    /// The root layout.
    private final VBox root;

    /// Button to toggle collapse/expand.
    private final IconButton btnCollapse;

    /// Button to stop the game process.
    private final IconButton btnExit;

    /// Button to open the game folder.
    private final IconButton btnFolder;

    /// Button to view installed mods.
    private final IconButton btnMods;

    /// Whether the window is currently collapsed to a thin strip.
    private boolean collapsed;

    /// Width of the manager in expanded state.
    private static final double EXPANDED_WIDTH = 34;

    /// Width of the manager in collapsed state.
    private static final double COLLAPSED_WIDTH = 10;

    /// Creates an instance manager window for a running game.
    ///
    /// @param process    the managed game process.
    /// @param profile    the launcher profile.
    /// @param versionId  the Minecraft version ID.
    /// @param repository the game repository.
    public InstanceManagerWindow(ManagedProcess process, Profile profile, String versionId, JMCLGameRepository repository) {
        this.process = Objects.requireNonNull(process);
        this.profile = Objects.requireNonNull(profile);
        this.versionId = Objects.requireNonNull(versionId);
        this.repository = Objects.requireNonNull(repository);

        initStyle(StageStyle.UNDECORATED);
        initStyle(StageStyle.TRANSPARENT);
        setResizable(false);
        setAlwaysOnTop(true);

        VBox root = new VBox(4);
        root.getStyleClass().add("instance-manager");
        root.setPadding(new Insets(6));
        root.setAlignment(Pos.TOP_CENTER);

        // Clip rounded corners for the undecorated transparent stage
        Rectangle clip = new Rectangle();
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        this.root = root;

        btnCollapse = new IconButton(SVG.KEYBOARD_ARROW_UP, this::toggleCollapse);
        FXUtils.installFastTooltip(btnCollapse, i18n("instance.manager.collapse"));

        btnExit = new IconButton(SVG.CANCEL, () -> {
            process.stop();
            close();
        });
        FXUtils.installFastTooltip(btnExit, i18n("instance.manager.exit"));

        btnFolder = new IconButton(SVG.FOLDER_OPEN, () ->
                FXUtils.openFolder(repository.getRunDirectory(versionId)));
        FXUtils.installFastTooltip(btnFolder, i18n("instance.manager.folder"));

        btnMods = new IconButton(SVG.EXTENSION, this::openModPage);
        FXUtils.installFastTooltip(btnMods, i18n("instance.manager.mods"));

        root.getChildren().setAll(btnCollapse, btnExit, btnFolder, btnMods);

        Scene scene = new Scene(root);
        scene.setFill(null);
        StyleSheets.init(scene);
        setScene(scene);

        positionNearLauncher();
        startWindowTracker();
        startProcessMonitor();
    }

    /// Positions this window near the main launcher as a starting point.
    private void positionNearLauncher() {
        Stage mainStage = Controllers.getStage();
        if (mainStage == null) return;
        setX(mainStage.getX() + mainStage.getWidth() + 4);
        setY(mainStage.getY() + 40);
    }

    /// Toggles between collapsed and expanded state.
    private void toggleCollapse() {
        if (collapsed) {
            expand();
        } else {
            collapse();
        }
    }

    /// Collapses the window into a thin vertical strip.
    private void collapse() {
        collapsed = true;
        Platform.runLater(() -> {
            root.getChildren().clear();
            root.setMinWidth(COLLAPSED_WIDTH);
            root.setMaxWidth(COLLAPSED_WIDTH);
            root.setMinHeight(60);
            root.setMaxHeight(60);
            root.setPrefWidth(COLLAPSED_WIDTH);
            root.setPrefHeight(60);
            root.getStyleClass().add("instance-manager-collapsed");
            root.setOnMouseClicked(e -> toggleCollapse());
            sizeToScene();
        });
    }

    /// Expands the window back to show all manager buttons.
    private void expand() {
        collapsed = false;
        Platform.runLater(() -> {
            root.getStyleClass().remove("instance-manager-collapsed");
            root.setOnMouseClicked(null);
            root.setMinWidth(EXPANDED_WIDTH);
            root.setMaxWidth(EXPANDED_WIDTH);
            root.setMinHeight(VBox.USE_COMPUTED_SIZE);
            root.setMaxHeight(VBox.USE_COMPUTED_SIZE);
            root.setPrefWidth(EXPANDED_WIDTH);
            root.setPrefHeight(VBox.USE_COMPUTED_SIZE);
            root.getChildren().setAll(btnCollapse, btnExit, btnFolder, btnMods);
            sizeToScene();
        });
    }

    /// Starts a background thread that periodically detects the Minecraft game window
    /// and repositions this manager next to it.
    private void startWindowTracker() {
        long pid = process.getProcess().pid();

        trackerThread = Lang.thread(() -> {
            double lastX = Double.NaN, lastY = Double.NaN;

            while (process.isRunning()) {
                try {
                    String[] bounds = getGameWindowBounds(pid);
                    if (bounds != null) {
                        double wx = Double.parseDouble(bounds[0]);
                        double wy = Double.parseDouble(bounds[1]);
                        double ww = Double.parseDouble(bounds[2]);
                        double newX = wx + ww + 4;
                        double newY = wy + 40;
                        if (newX != lastX || newY != lastY) {
                            lastX = newX;
                            lastY = newY;
                            double fx = newX, fy = newY;
                            Platform.runLater(() -> {
                                setX(fx);
                                setY(fy);
                            });
                        }
                    }
                } catch (Exception ignored) {
                    // game window not ready yet, keep current position
                }

                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "GameWindowTracker-" + versionId, true);
    }

    /// Queries the game window bounds for the given process ID.
    ///
    /// @param pid the game process ID.
    /// @return an array of [x, y, width], or null if the window isn't found.
    private static @Nullable String[] getGameWindowBounds(long pid) throws IOException {
        return switch (OperatingSystem.CURRENT_OS) {
            case MACOS -> getMacOSWindowBounds(pid);
            case WINDOWS -> getWindowsWindowBounds(pid);
            case LINUX, FREEBSD -> getLinuxWindowBounds(pid);
            default -> null;
        };
    }

    /// macOS: uses `osascript` with System Events to find the game window by PID.
    /// Tries to locate the main window first, falls back to the first available window.
    private static @Nullable String[] getMacOSWindowBounds(long pid) throws IOException {
        String script =
            "tell application \"System Events\"\n" +
            "  set procList to every process whose unix id is " + pid + "\n" +
            "  if (count of procList) = 0 then return \"\"\n" +
            "  set proc to item 1 of procList\n" +
            "  set winList to every window of proc\n" +
            "  if (count of winList) = 0 then return \"\"\n" +
            "  set target to item 1 of winList\n" +
            "  repeat with w in winList\n" +
            "    try\n" +
            "      if value of attribute \"AXMain\" of w is true then\n" +
            "        set target to w\n" +
            "        exit repeat\n" +
            "      end if\n" +
            "    end try\n" +
            "  end repeat\n" +
            "  try\n" +
            "    set pos to position of target\n" +
            "    set sz to size of target\n" +
            "    return (item 1 of pos as string) & \",\" & (item 2 of pos as string) & \",\" & (item 1 of sz as string)\n" +
            "  on error\n" +
            "    return \"\"\n" +
            "  end try\n" +
            "end tell";

        String output = runScript("osascript", "-e", script);
        if (output.isEmpty()) return null;

        String[] parts = output.split(",");
        if (parts.length >= 3) {
            try {
                // validate numeric
                Double.parseDouble(parts[0].trim());
                Double.parseDouble(parts[1].trim());
                Double.parseDouble(parts[2].trim());
                return new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim()};
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /// Windows: uses PowerShell with C# P/Invoke to EnumWindows and GetWindowRect.
    private static @Nullable String[] getWindowsWindowBounds(long pid) throws IOException {
        String script =
            "$pid = " + pid + ";\n" +
            "$code = @'\n" +
            "using System;\n" +
            "using System.Runtime.InteropServices;\n" +
            "public struct R { public int L,T,Rg,B; }\n" +
            "public class W {\n" +
            "  public delegate bool E(IntPtr h, IntPtr l);\n" +
            "  [DllImport(\"user32\")] public static extern bool EnumWindows(E e, IntPtr l);\n" +
            "  [DllImport(\"user32\")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint p);\n" +
            "  [DllImport(\"user32\")] public static extern bool GetWindowRect(IntPtr h, out R r);\n" +
            "  [DllImport(\"user32\")] public static extern bool IsWindowVisible(IntPtr h);\n" +
            "  public static IntPtr r; public static uint tp;\n" +
            "  public static bool C(IntPtr h, IntPtr l) {\n" +
            "    W.GetWindowThreadProcessId(h, out tp);\n" +
            "    if(tp==" + pid + " && W.IsWindowVisible(h)){r=h; return false;}\n" +
            "    return true;\n" +
            "  }\n" +
            "}\n" +
            "'@;\n" +
            "Add-Type -TypeDefinition $code -ReferencedAssemblies System.Runtime.InteropServices;\n" +
            "[W]::EnumWindows([W+E]::new([W]::C), [IntPtr]::Zero) | Out-Null;\n" +
            "if([W]::r -ne [IntPtr]::Zero){\n" +
            "  $rect = New-Object R;\n" +
            "  [W]::GetWindowRect([W]::r, [ref]$rect);\n" +
            "  Write-Output \"$($rect.L),$($rect.T),$($rect.Rg-$rect.L)\";\n" +
            "}\n";

        String output = runScript("powershell", "-NoProfile", "-Command", script);
        if (output.isEmpty()) return null;

        String[] parts = output.split(",");
        if (parts.length >= 3) {
            try {
                Double.parseDouble(parts[0].trim());
                Double.parseDouble(parts[1].trim());
                Double.parseDouble(parts[2].trim());
                return new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim()};
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /// Linux/FreeBSD: uses `xdotool` to find windows by PID.
    private static @Nullable String[] getLinuxWindowBounds(long pid) throws IOException {
        // try xdotool first
        Process p = new ProcessBuilder("xdotool", "search", "--pid", String.valueOf(pid)).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int exit;
        try { exit = p.waitFor(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }

        if (exit != 0 || output.isEmpty()) return null;

        // get geometry of the first window
        String wid = output.split("\\R")[0].trim();
        if (wid.isEmpty()) return null;

        Process p2 = new ProcessBuilder("xdotool", "getwindowgeometry", "--shell", wid).start();
        String geo = new String(p2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try { p2.waitFor(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }

        String x = null, y = null, w = null;
        for (String line : geo.split("\\R")) {
            line = line.trim();
            if (line.startsWith("X=")) x = line.substring(2);
            else if (line.startsWith("Y=")) y = line.substring(2);
            else if (line.startsWith("WIDTH=")) w = line.substring(6);
        }
        if (x != null && y != null && w != null) {
            return new String[]{x, y, w};
        }
        return null;
    }

    /// Runs a script process and returns trimmed stdout, or empty string on failure.
    private static String runScript(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            // consume stderr to avoid deadlock
            p.getErrorStream().readAllBytes();
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return output;
        } catch (IOException e) {
            return "";
        }
    }

    /// Starts a background thread that monitors the game process.
    /// When the process exits, the manager window closes automatically.
    private void startProcessMonitor() {
        monitorThread = new Thread(() -> {
            while (process.isRunning()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            Platform.runLater(this::close);
        }, "InstanceManagerMonitor-" + versionId);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /// Opens the mod list page for this instance in the launcher.
    private void openModPage() {
        Controllers.getVersionPage().setVersion(versionId, profile);
        Controllers.getVersionPage().showInstanceSettings();
        Controllers.navigate(Controllers.getVersionPage());
    }

    /// A small icon-only button used in the instance manager toolbar.
    private static final class IconButton extends com.jfoenix.controls.JFXButton {
        IconButton(SVG icon, Runnable action) {
            setGraphic(icon.createIcon(16));
            setFocusTraversable(false);
            getStyleClass().add("instance-manager-button");
            setOnAction(e -> action.run());
        }
    }
}