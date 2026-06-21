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

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import org.glavo.nbt.NBTElement;
import org.glavo.nbt.chunk.Chunk;
import org.glavo.nbt.chunk.ChunkRegion;
import org.glavo.nbt.tag.*;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public final class NBTTreeCell extends TreeCell<@Nullable NBTElement> {

    // ── Icons ──────────────────────────────────────────────────────────────

    private static @Nullable Image getIcon(NBTElement element) {
        if (element instanceof Tag tag) {
            TagType<?> type = tag.getType();
            if (type == TagType.BYTE)        return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Byte.png");
            if (type == TagType.SHORT)       return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Short.png");
            if (type == TagType.INT)         return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Int.png");
            if (type == TagType.LONG)        return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Long.png");
            if (type == TagType.FLOAT)       return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Float.png");
            if (type == TagType.DOUBLE)      return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Double.png");
            if (type == TagType.STRING)      return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_String.png");
            if (type == TagType.BYTE_ARRAY)  return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Byte_Array.png");
            if (type == TagType.INT_ARRAY)   return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Int_Array.png");
            if (type == TagType.LONG_ARRAY)  return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Long_Array.png");
            if (type == TagType.LIST)        return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_List.png");
            if (type == TagType.COMPOUND)    return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Compound.png");
        }
        if (element instanceof ChunkRegion) return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_List.png");
        if (element instanceof Chunk)       return FXUtils.newBuiltinImage("/assets/img/nbt/TAG_Compound.png");
        return null;
    }

    // ── Fields ─────────────────────────────────────────────────────────────

    private final ImageView imageView;
    private TextField textField;
    private boolean editing;
    private ContextMenu contextMenu;

    // ── Constructor ────────────────────────────────────────────────────────

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

        buildContextMenu();
    }

    // ── Context Menu ───────────────────────────────────────────────────────

    private void buildContextMenu() {
        contextMenu = new ContextMenu();

        MenuItem addByte = new MenuItem("Add Byte");      addByte.setOnAction(e -> addChild(TagType.BYTE, "0"));
        MenuItem addShort = new MenuItem("Add Short");    addShort.setOnAction(e -> addChild(TagType.SHORT, "0"));
        MenuItem addInt = new MenuItem("Add Int");        addInt.setOnAction(e -> addChild(TagType.INT, "0"));
        MenuItem addLong = new MenuItem("Add Long");      addLong.setOnAction(e -> addChild(TagType.LONG, "0"));
        MenuItem addFloat = new MenuItem("Add Float");    addFloat.setOnAction(e -> addChild(TagType.FLOAT, "0"));
        MenuItem addDouble = new MenuItem("Add Double");  addDouble.setOnAction(e -> addChild(TagType.DOUBLE, "0"));
        MenuItem addString = new MenuItem("Add String");  addString.setOnAction(e -> addChild(TagType.STRING, "\"\""));
        MenuItem addCompound = new MenuItem("Add Compound"); addCompound.setOnAction(e -> addCompound());
        MenuItem addList = new MenuItem("Add List");      addList.setOnAction(e -> addList());

        Menu addMenu = new Menu("Add Child");
        addMenu.getItems().addAll(addByte, addShort, addInt, addLong, addFloat, addDouble, addString,
                new SeparatorMenuItem(), addCompound, addList);

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> deleteNode());

        MenuItem copyValue = new MenuItem("Copy Value");
        copyValue.setOnAction(e -> copyNodeValue());

        contextMenu.getItems().addAll(addMenu, deleteItem, new SeparatorMenuItem(), copyValue);

        setOnContextMenuRequested(e -> {
            NBTElement item = getItem();
            if (item != null) {
                addMenu.setVisible(item instanceof ParentTag<?>);
                deleteItem.setVisible(getTreeItem().getParent() != null); // not root
                copyValue.setVisible(item instanceof ValueTag<?>);
                contextMenu.show(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    /// Adds a new child tag to a Compound or List parent.
    @SuppressWarnings("unchecked")
    private void addChild(TagType<?> type, String defaultValue) {
        NBTElement item = getItem();
        if (!(item instanceof ParentTag<?>)) return;
        ParentTag<Tag> parent = (ParentTag<Tag>) (ParentTag<?>) item;

        Tag newTag;
        if (type == TagType.BYTE)      newTag = new ByteTag(Byte.parseByte(defaultValue));
        else if (type == TagType.SHORT) newTag = new ShortTag(Short.parseShort(defaultValue));
        else if (type == TagType.INT)  newTag = new IntTag(Integer.parseInt(defaultValue));
        else if (type == TagType.LONG) newTag = new LongTag(Long.parseLong(defaultValue));
        else if (type == TagType.FLOAT) newTag = new FloatTag(Float.parseFloat(defaultValue));
        else if (type == TagType.DOUBLE) newTag = new DoubleTag(Double.parseDouble(defaultValue));
        else if (type == TagType.STRING) newTag = new StringTag(defaultValue);
        else return;

        if (parent instanceof CompoundTag) {
            TextInputDialog dialog = new TextInputDialog("newKey");
            dialog.setTitle("New Tag");
            dialog.setHeaderText("Enter tag name:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                newTag.setName(name);
                parent.addTag(newTag);
                refreshTree();
            });
        } else {
            newTag.setName("");
            parent.addTag(newTag);
            refreshTree();
        }
    }

    @SuppressWarnings("unchecked")
    private void addCompound() {
        NBTElement item = getItem();
        if (!(item instanceof ParentTag<?>)) return;
        ParentTag<Tag> parent = (ParentTag<Tag>) (ParentTag<?>) item;

        if (parent instanceof CompoundTag) {
            TextInputDialog dialog = new TextInputDialog("newCompound");
            dialog.setTitle("New Compound");
            dialog.setHeaderText("Enter name:");
            dialog.showAndWait().ifPresent(name -> {
                parent.addTag(new CompoundTag().setName(name));
                refreshTree();
            });
        } else {
            parent.addTag(new CompoundTag());
            refreshTree();
        }
    }

    @SuppressWarnings("unchecked")
    private void addList() {
        NBTElement item = getItem();
        if (!(item instanceof ParentTag<?>)) return;
        ParentTag<Tag> parent = (ParentTag<Tag>) (ParentTag<?>) item;

        if (parent instanceof CompoundTag) {
            TextInputDialog dialog = new TextInputDialog("newList");
            dialog.setTitle("New List");
            dialog.setHeaderText("Enter name:");
            dialog.showAndWait().ifPresent(name -> {
                parent.addTag(new ListTag<Tag>().setName(name));
                refreshTree();
            });
        } else {
            parent.addTag(new ListTag<Tag>());
            refreshTree();
        }
    }

    private void deleteNode() {
        TreeItem<NBTElement> treeItem = getTreeItem();
        if (treeItem == null || treeItem.getParent() == null) return;
        NBTElement item = getItem();
        if (!(item instanceof Tag tag)) return;

        ParentTag<?> parent = tag.getParentTag();
        if (parent == null) return;
        parent.removeElement(tag);

        // Remove from tree
        treeItem.getParent().getChildren().remove(treeItem);
    }

    private void copyNodeValue() {
        NBTElement item = getItem();
        if (item instanceof ValueTag<?> vt) {
            javafx.scene.input.Clipboard.getSystemClipboard()
                    .setContent(java.util.Collections.singletonMap(
                            javafx.scene.input.DataFormat.PLAIN_TEXT, vt.getAsString()));
        }
    }

    /// Re-expands the current parent to reflect added/removed children.
    private void refreshTree() {
        TreeItem<NBTElement> parent = getTreeItem();
        if (parent instanceof NBTTreeItem nbtParent) {
            nbtParent.resetChildren();
            parent.setExpanded(false);
            parent.setExpanded(true);
        }
    }

    // ── Inline editing ─────────────────────────────────────────────────────

    private NBTTreeItem getNBTTreeItem() {
        return (NBTTreeItem) getTreeItem();
    }

    private void beginEdit() {
        if (editing) return;
        NBTElement item = getItem();
        if (!(item instanceof ValueTag<?> tag)) return;
        editing = true;

        String name = getDisplayName();
        String value = tag.getAsString();

        textField = new TextField(value);
        textField.setPrefWidth(150);
        textField.selectAll();
        textField.setOnAction(ev -> commitEdit());
        textField.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ESCAPE) abortEdit(); });
        textField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && editing) commitEdit();
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

    private void commitEdit() {
        if (!editing) return;
        editing = false;

        String newText = textField.getText().trim();
        NBTElement item = getItem();
        if (!(item instanceof Tag oldTag)) { restoreDisplay(); return; }

        Tag newTag;
        try {
            newTag = parseTag(oldTag, newText);
        } catch (Exception ex) {
            Logger.LOG.warning("Failed to parse NBT value: " + newText);
            restoreDisplay();
            return;
        }
        if (newTag == null) { restoreDisplay(); return; }

        ParentTag<?> parent = oldTag.getParentTag();
        if (parent instanceof CompoundTag compound) {
            compound.addTag(newTag);
        } else if (parent instanceof ListTag<?>) {
            @SuppressWarnings("unchecked")
            ListTag<Tag> listTag = (ListTag<Tag>) parent;
            int idx = oldTag.getIndex();
            if (idx >= 0 && idx < listTag.size()) {
                List<Tag> tags = new ArrayList<>();
                listTag.forEach(tags::add);
                tags.set(idx, newTag);
                listTag.clear();
                tags.forEach(listTag::addTag);
            }
        }

        NBTTreeItem treeItem = getNBTTreeItem();
        treeItem.setValue(newTag);
        updateItem(newTag, false);
        setGraphic(imageView);
    }

    private void abortEdit() {
        if (!editing) return;
        editing = false;
        NBTElement item = getItem();
        if (item != null) updateItem(item, false);
        setGraphic(imageView);
    }

    private void restoreDisplay() {
        NBTElement item = getItem();
        if (item != null) updateItem(item, false);
        setGraphic(imageView);
    }

    // ── Tag parsing ────────────────────────────────────────────────────────

    private static @Nullable Tag parseTag(Tag oldTag, String text) {
        String name = oldTag.getName();
        TagType<?> type = oldTag.getType();
        Tag tag;

        if (type == TagType.BYTE) {
            text = text.trim().toLowerCase();
            if ("true".equals(text))  tag = new ByteTag((byte) 1);
            else if ("false".equals(text)) tag = new ByteTag((byte) 0);
            else tag = new ByteTag(Byte.parseByte(text));
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

    private static long parseLong(String text) {
        text = text.trim();
        if (text.startsWith("0x") || text.startsWith("0X"))
            return Long.parseLong(text.substring(2), 16);
        return Long.parseLong(text);
    }

    // ── Display ────────────────────────────────────────────────────────────

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
        } else {
            text = formatValue(item);
        }

        setText(text == null ? name : name + ": " + text);
    }

    /// Smart formatting: booleans for ByteTag 0/1, short array preview, etc.
    private static String formatValue(NBTElement element) {
        if (element instanceof ByteTag) {
            byte v = ((ByteTag) element).getValue();
            if (v == 0) return "false";
            if (v == 1) return "true";
        }
        if (element instanceof ByteArrayTag)
            return "[" + ((ByteArrayTag) element).size() + " bytes]";
        if (element instanceof IntArrayTag) {
            IntArrayTag ia = (IntArrayTag) element;
            if (ia.isUUID()) return "UUID(" + ia.getUUID() + ")";
            return "[" + ia.size() + " ints]";
        }
        if (element instanceof LongArrayTag)
            return "[" + ((LongArrayTag) element).size() + " longs]";
        if (element instanceof ValueTag<?> vt)
            return vt.getAsString();
        return "";
    }

    private String getDisplayName() {
        String overrideName = getNBTTreeItem().getOverrideName();
        if (overrideName != null) return overrideName;

        NBTElement value = getNBTTreeItem().getValue();
        if (value instanceof Tag tag) {
            if (tag.getParentTag() instanceof ListTag<?>)
                return Integer.toString(tag.getIndex());
            return tag.getName();
        }
        if (value instanceof Chunk chunk)
            return "Chunk (" + chunk.getLocalX() + ", " + chunk.getLocalZ() + ")";
        return "";
    }
}
