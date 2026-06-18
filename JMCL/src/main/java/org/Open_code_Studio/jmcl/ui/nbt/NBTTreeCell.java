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

import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import org.glavo.nbt.NBTElement;
import org.glavo.nbt.NBTParent;
import org.glavo.nbt.chunk.Chunk;
import org.glavo.nbt.chunk.ChunkRegion;
import org.glavo.nbt.tag.*;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public final class NBTTreeCell extends TreeCell<@Nullable NBTElement> {

    private static @Nullable Image getIcon(NBTElement element) {
        if (element instanceof Tag tag) {
            TagType<?> type = tag.getType();
            if (type == TagType.BYTE) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Byte.png");
            } else if (type == TagType.SHORT) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Short.png");
            } else if (type == TagType.INT) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Int.png");
            } else if (type == TagType.LONG) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Long.png");
            } else if (type == TagType.FLOAT) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Float.png");
            } else if (type == TagType.DOUBLE) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Double.png");
            } else if (type == TagType.STRING) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_String.png");
            } else if (type == TagType.BYTE_ARRAY) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Byte_Array.png");
            } else if (type == TagType.INT_ARRAY) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Int_Array.png");
            } else if (type == TagType.LONG_ARRAY) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Long_Array.png");
            } else if (type == TagType.LIST) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_List.png");
            } else if (type == TagType.COMPOUND) {
                return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Compound.png");
            } else {
                return null;
            }
        } else if (element instanceof ChunkRegion)
            return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_List.png");
        else if (element instanceof Chunk)
            return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Compound.png");
        else
            return null;
    }

    private final ImageView imageView;
    private TextField textField;
    private boolean editing;

    public NBTTreeCell() {
        imageView = new ImageView();
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        setGraphic(imageView);

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isEmpty() && getItem() instanceof ValueTag<?>) {
                beginEdit();
            }
        });
    }

    private NBTTreeItem getNBTTreeItem() {
        return (NBTTreeItem) getTreeItem();
    }

    /// Starts inline editing for the current ValueTag.
    private void beginEdit() {
        if (editing) return;
        NBTElement item = getItem();
        if (!(item instanceof ValueTag<?> tag)) return;

        editing = true;

        // Create label + text field in an HBox
        String name = getDisplayName();
        String value = tag.getAsString();

        textField = new TextField(value);
        textField.setPrefWidth(150);
        textField.selectAll();

        textField.setOnAction(ev -> commitEdit());
        textField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                abortEdit();
            }
        });
        textField.focusedProperty().addListener((obs, old, isFocused) -> {
            if (!isFocused && editing) {
                commitEdit();
            }
        });

        HBox editBox = new HBox(4);
        if (name != null && !name.isEmpty()) {
            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(name + ": ");
            nameLabel.setStyle("-fx-text-fill: -fx-text-base-color;");
            editBox.getChildren().add(nameLabel);
        }
        editBox.getChildren().add(textField);

        setText(null);
        setGraphic(editBox);
        textField.requestFocus();
    }

    /// Commits the current edit: parses the text, creates a new Tag, and updates the tree.
    private void commitEdit() {
        if (!editing) return;
        editing = false;

        String newText = textField.getText().trim();
        NBTElement item = getItem();
        if (!(item instanceof Tag oldTag)) {
            restoreDisplay();
            return;
        }

        Tag newTag = null;
        try {
            newTag = parseTag(oldTag, newText);
        } catch (Exception ex) {
            Logger.LOG.warning("Failed to parse NBT value: " + newText);
            restoreDisplay();
            return;
        }

        if (newTag == null) {
            restoreDisplay();
            return;
        }

        // Replace the old tag in its parent
        ParentTag<?> parent = oldTag.getParentTag();
        if (parent instanceof CompoundTag compound) {
            // addTag replaces existing tags with the same name
            compound.addTag(newTag);
        } else if (parent instanceof ListTag<?>) {
            @SuppressWarnings("unchecked")
            ListTag<Tag> listTag = (ListTag<Tag>) parent;
            int idx = oldTag.getIndex();
            if (idx >= 0 && idx < listTag.size()) {
                // Snapshot all tags, replace at index, clear and re-add
                List<Tag> tags = new ArrayList<>();
                listTag.forEach(tags::add);
                tags.set(idx, newTag);
                listTag.clear();
                tags.forEach(listTag::addTag);
            }
        }

        // Update the tree item value
        NBTTreeItem treeItem = getNBTTreeItem();
        treeItem.setValue(newTag);
        updateItem(newTag, false);
        setGraphic(imageView);
    }

    /// Cancels editing and restores the original display.
    private void abortEdit() {
        if (!editing) return;
        editing = false;
        NBTElement item = getItem();
        if (item != null) {
            updateItem(item, false);
        }
        setGraphic(imageView);
    }

    /// Restores the normal display state.
    private void restoreDisplay() {
        NBTElement item = getItem();
        if (item != null) {
            updateItem(item, false);
        }
        setGraphic(imageView);
    }

    /// Parses new text into a Tag of the same type as oldTag.
    private static @Nullable Tag parseTag(Tag oldTag, String text) {
        String name = oldTag.getName();
        TagType<?> type = oldTag.getType();
        Tag tag;

        if (type == TagType.BYTE) {
            tag = new ByteTag(Byte.parseByte(text));
        } else if (type == TagType.SHORT) {
            tag = new ShortTag(Short.parseShort(text));
        } else if (type == TagType.INT) {
            tag = new IntTag(Integer.parseInt(text));
        } else if (type == TagType.LONG) {
            tag = new LongTag(parseLong(text));
        } else if (type == TagType.FLOAT) {
            tag = new FloatTag(Float.parseFloat(text));
        } else if (type == TagType.DOUBLE) {
            tag = new DoubleTag(Double.parseDouble(text));
        } else if (type == TagType.STRING) {
            tag = new StringTag(text);
        } else {
            return null;
        }

        tag.setName(name);
        return tag;
    }

    /// Parses a long value, supporting both decimal and hex (0x...) formats.
    private static long parseLong(String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Long.parseLong(text.substring(2), 16);
        }
        return Long.parseLong(text);
    }

    @Override
    public void updateItem(@Nullable NBTElement item, boolean empty) {
        super.updateItem(item, empty);

        if (editing) return;

        if (empty || item == null) {
            imageView.setImage(null);
            setText(null);
            setGraphic(imageView);
            return;
        }

        imageView.setImage(getIcon(item));

        String name = getDisplayName();

        String text;
        if (item instanceof ParentTag<?> parentTag) {
            text = i18n("nbt.entries", parentTag.size());
        } else if (item instanceof ValueTag<?> valueTag) {
            text = valueTag.getAsString();
        } else {
            text = null;
        }

        if (text == null) {
            setText(name);
        } else {
            setText(name + ": " + text);
        }
    }

    /// Gets the display name for the current item.
    private String getDisplayName() {
        String overrideName = getNBTTreeItem().getOverrideName();
        if (overrideName != null) return overrideName;

        NBTElement value = getNBTTreeItem().getValue();
        if (value instanceof Tag tag) {
            if (tag.getParentTag() instanceof ListTag<?>) {
                return Integer.toString(tag.getIndex());
            } else {
                return tag.getName();
            }
        } else if (value instanceof Chunk chunk) {
            return "Chunk (" + chunk.getLocalX() + ", " + chunk.getLocalZ() + ")";
        }
        return "";
    }
}
