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
package org.Open_code_Studio.jmcl.ui.nbt;

import com.jfoenix.controls.JFXButton;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Skin;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.TreeViewSkin;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.glavo.nbt.NBTElement;
import org.glavo.nbt.tag.Tag;
import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.construct.MessageDialogPane;
import org.Open_code_Studio.jmcl.ui.construct.PageCloseEvent;
import org.Open_code_Studio.jmcl.ui.construct.SpinnerPane;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorPage;
import org.Open_code_Studio.jmcl.util.StringUtils;
import org.Open_code_Studio.jmcl.util.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

import static org.Open_code_Studio.jmcl.ui.FXUtils.onEscPressed;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/**
 * @author Glavo
 */
public final class NBTEditorPage extends SpinnerPane implements DecoratorPage {
    private final ReadOnlyObjectWrapper<State> state;
    private final Path file;
    private final NBTFileType type;

    private final BorderPane root = new BorderPane();

    /// The root NBT element read from file, kept to enable saving.
    private @Nullable NBTElement rootElement;

    /// The TreeView displaying the NBT structure.
    private @Nullable TreeView<NBTElement> treeView;

    /// The root tree item of the tree view.
    private @Nullable NBTTreeItem rootTreeItem;

    public NBTEditorPage(Path file) throws IOException {
        getStyleClass().add("gray-background");

        this.state = new ReadOnlyObjectWrapper<>(State.fromTitle(i18n("nbt.title", file.toString())));
        this.file = file;

        //noinspection DataFlowIssue
        this.type = NBTFileType.ofFile(file);
        if (type == null) {
            throw new IOException("Unknown type of file " + file);
        }

        setContent(root);
        setLoading(true);

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(8));
        actions.setAlignment(Pos.CENTER_RIGHT);

        JFXButton saveButton = FXUtils.newRaisedButton(i18n("button.save"));
        saveButton.setOnAction(e -> {
            try {
                save();
            } catch (IOException ex) {
                LOG.warning("Failed to save NBT file", ex);
                Controllers.dialog(i18n("nbt.save.failed") + "\n\n" + StringUtils.getStackTrace(ex));
            }
        });

        JFXButton cancelButton = FXUtils.newRaisedButton(i18n("button.cancel"));
        cancelButton.setOnAction(e -> fireEvent(new PageCloseEvent()));
        onEscPressed(this, cancelButton::fire);

        actions.getChildren().setAll(saveButton, cancelButton);

        Task.supplyAsync(() -> type.read(file))
                .whenComplete(Schedulers.javafx(), (result, exception) -> {
                    if (exception == null) {
                        setLoading(false);

                        this.rootElement = result;
                        NBTTreeItem rootItem = new NBTTreeItem(result, FileUtils.getName(file));
                        this.rootTreeItem = rootItem;

                        var view = new TreeView<>(rootItem) {
                            @Override
                            protected Skin<?> createDefaultSkin() {
                                return new TreeViewSkin<>(this) {
                                    {
                                        FXUtils.smoothScrolling(getVirtualFlow());
                                    }
                                };
                            }
                        };
                        view.setCellFactory(ignored -> new NBTTreeCell());
                        view.addEventHandler(TreeItem.<NBTElement>branchExpandedEvent(), event -> {
                            TreeItem<NBTElement> item = event.getTreeItem();
                            if (item.getValue() instanceof Tag && item.getChildren().size() == 1)
                                item.getChildren().get(0).setExpanded(true);
                        });
                        rootItem.setExpanded(true);
                        this.treeView = view;

                        VBox wrapper = new VBox();
                        wrapper.getChildren().setAll(actions, view);
                        VBox.setVgrow(view, javafx.scene.layout.Priority.ALWAYS);

                        BorderPane.setMargin(wrapper, new Insets(10));
                        onEscPressed(view, cancelButton::fire);
                        this.root.setCenter(wrapper);
                    } else {
                        LOG.warning("Fail to open nbt file", exception);
                        Controllers.dialog(i18n("nbt.open.failed") + "\n\n" + StringUtils.getStackTrace(exception), null, MessageDialogPane.MessageType.WARNING, cancelButton::fire);
                    }
                }).start();
    }

    /// Saves the current NBT element tree back to the file.
    public void save() throws IOException {
        if (rootElement == null) {
            throw new IOException("No NBT data loaded");
        }
        type.write(file, rootElement);
        LOG.info("NBT file saved: " + file);
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state;
    }
}
