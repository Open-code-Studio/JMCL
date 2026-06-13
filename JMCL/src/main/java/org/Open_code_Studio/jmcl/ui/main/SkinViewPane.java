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
package org.Open_code_Studio.jmcl.ui.main;

import javafx.beans.InvalidationListener;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import org.Open_code_Studio.jmcl.auth.Account;
import org.Open_code_Studio.jmcl.auth.yggdrasil.TextureModel;
import org.Open_code_Studio.jmcl.game.TexturesLoader;
import org.Open_code_Studio.jmcl.setting.Accounts;
import org.Open_code_Studio.jmcl.ui.skin.SkinCanvas;
import org.Open_code_Studio.jmcl.ui.skin.animation.SkinAniRunning;
import org.Open_code_Studio.jmcl.ui.skin.animation.SkinAniWavingArms;

/**
 * A pane that displays a 3D preview of the currently selected account's skin.
 * Uses JavaFX 3D (SubScene) for rendering via {@link SkinCanvas}.
 */
public class SkinViewPane extends StackPane {
    private final SkinCanvas skinCanvas;

    public SkinViewPane() {
        getStyleClass().add("skin-view-pane");

        skinCanvas = new SkinCanvas(TexturesLoader.getDefaultSkinImage(), 300, 400, false);
        skinCanvas.enableRotation(0.3);
        skinCanvas.getAnimationPlayer().addSkinAnimation(
                new SkinAniRunning(1, 800, 30, skinCanvas),
                new SkinAniWavingArms(1, 3000, 45, skinCanvas)
        );
        getChildren().add(skinCanvas);

        // Listen for account changes
        InvalidationListener updateSkinListener = obs -> {
            Account selectedAccount = Accounts.selectedAccountProperty().get();
            if (selectedAccount != null) {
                updateSkinForAccount(selectedAccount);
            } else {
                updateSkinFromImage(TexturesLoader.getDefaultSkinImage(), false);
            }
        };

        Accounts.selectedAccountProperty().addListener(updateSkinListener);
        updateSkinListener.invalidated(null);
    }

    private void updateSkinForAccount(Account account) {
        TexturesLoader.skinBinding(account).addListener((obs, old, val) -> {
            if (val != null) {
                Image skin = val.getImage();
                boolean isSlim = TextureModel.SLIM.modelName.equals(val.getMetadata().get("model"));
                updateSkinFromImage(skin, isSlim);
            }
        });
    }

    private void updateSkinFromImage(Image skin, boolean isSlim) {
        skinCanvas.updateSkin(skin, isSlim, null);
    }
}