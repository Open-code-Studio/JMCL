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
package org.Open_code_Studio.jmcl.ui.decorator;

import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DecoratorAnimatedPage extends Control {

    protected final VBox left = new VBox();
    protected final StackPane center = new StackPane();
    protected final StackPane leftCenter = new StackPane();

    {
        getStyleClass().add("gray-background");
    }

    protected void setLeft(Node... children) {
        left.getChildren().setAll(children);
    }

    protected void setCenter(Node... children) {
        center.getChildren().setAll(children);
    }

    protected void setLeftCenter(Node... children) {
        leftCenter.getChildren().setAll(children);
    }

    public VBox getLeft() {
        return left;
    }

    public StackPane getCenter() {
        return center;
    }

    public StackPane getLeftCenter() {
        return leftCenter;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DecoratorAnimatedPageSkin<>(this);
    }

    public static class DecoratorAnimatedPageSkin<T extends DecoratorAnimatedPage> extends SkinBase<T> {

        protected DecoratorAnimatedPageSkin(T control) {
            super(control);

            BorderPane pane = new BorderPane();
            
            control.left.setPrefWidth(200);
            control.left.setMinWidth(Region.USE_PREF_SIZE);
            control.left.setMaxWidth(Region.USE_PREF_SIZE);
            pane.setLeft(control.left);
            
            HBox centerContainer = new HBox();
            centerContainer.setFillHeight(true);
            
            control.leftCenter.setPrefWidth(260);
            control.leftCenter.setMinWidth(260);
            control.leftCenter.setMaxWidth(400);
            HBox.setHgrow(control.leftCenter, Priority.SOMETIMES);
            // Hide leftCenter when it has no children to avoid empty space on pages that don't use it
            control.leftCenter.visibleProperty().bind(Bindings.isNotEmpty(control.leftCenter.getChildren()));
            control.leftCenter.managedProperty().bind(control.leftCenter.visibleProperty());
            centerContainer.getChildren().add(control.leftCenter);
            
            HBox.setHgrow(control.center, Priority.ALWAYS);
            centerContainer.getChildren().add(control.center);
            
            pane.setCenter(centerContainer);
            getChildren().setAll(pane);
        }

        protected void setLeft(Node... children) {
            getSkinnable().setLeft(children);
        }

        protected void setCenter(Node... children) {
            getSkinnable().setCenter(children);
        }

        protected void setLeftCenter(Node... children) {
            getSkinnable().setLeftCenter(children);
        }
    }

}