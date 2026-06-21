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

import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.event.EventBus;
import org.Open_code_Studio.jmcl.event.RefreshedVersionsEvent;
import org.Open_code_Studio.jmcl.game.JMCLGameRepository;
import org.Open_code_Studio.jmcl.game.ModpackHelper;
import org.Open_code_Studio.jmcl.game.Version;
import org.Open_code_Studio.jmcl.setting.Accounts;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.Profiles;
import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.terracotta.TerracottaMetadata;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.account.AccountAdvancedListItem;
import org.Open_code_Studio.jmcl.ui.account.AccountListPopupMenu;
import org.Open_code_Studio.jmcl.ui.animation.AnimationUtils;
import org.Open_code_Studio.jmcl.ui.construct.AdvancedListBox;
import org.Open_code_Studio.jmcl.ui.construct.AdvancedListItem;
import org.Open_code_Studio.jmcl.ui.construct.MessageDialogPane;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorAnimatedPage;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorPage;
import org.Open_code_Studio.jmcl.ui.download.ModpackInstallWizardProvider;
import org.Open_code_Studio.jmcl.ui.nbt.NBTEditorPage;
import org.Open_code_Studio.jmcl.ui.nbt.NBTFileType;
import org.Open_code_Studio.jmcl.ui.versions.GameAdvancedListItem;
import org.Open_code_Studio.jmcl.ui.versions.GameListPopupMenu;
import org.Open_code_Studio.jmcl.ui.versions.Versions;
import org.Open_code_Studio.jmcl.upgrade.UpdateChecker;
import org.Open_code_Studio.jmcl.util.Lang;
import org.Open_code_Studio.jmcl.util.StringUtils;
import org.Open_code_Studio.jmcl.util.TaskCancellationAction;
import org.Open_code_Studio.jmcl.util.io.CompressingUtils;
import org.Open_code_Studio.jmcl.util.io.FileUtils;
import org.Open_code_Studio.jmcl.util.platform.*;
import org.Open_code_Studio.jmcl.util.versioning.VersionNumber;

import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.Open_code_Studio.jmcl.ui.FXUtils.runInFX;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public class RootPage extends DecoratorAnimatedPage implements DecoratorPage {
    private MainPage mainPage = null;

    public RootPage() {
        EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class)
                .register(event -> onRefreshedVersions((JMCLGameRepository) event.getSource()));

        getStyleClass().add("md3-root-page");

        Profile profile = Profiles.getSelectedProfile();
        if (profile != null && profile.getRepository().isLoaded())
            onRefreshedVersions(profile.getRepository());

        getStyleClass().remove("gray-background");
        getLeft().getStyleClass().add("gray-background");
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return getMainPage().stateProperty();
    }

    @Override
    protected Skin createDefaultSkin() {
        return new Skin(this);
    }

    public MainPage getMainPage() {
        if (mainPage == null) {
            MainPage mainPage = new MainPage();
            FXUtils.applyDragListener(mainPage,
                    file -> ModpackHelper.isFileModpackByExtension(file) || NBTFileType.isNBTFileByExtension(file) || "json".equalsIgnoreCase(FileUtils.getExtension(file)),
                    modpacks -> {
                        Path file = modpacks.get(0);
                        if (ModpackHelper.isFileModpackByExtension(file)) {
                            Controllers.getDecorator().startWizard(
                                    new ModpackInstallWizardProvider(Profiles.getSelectedProfile(), file),
                                    i18n("install.modpack"));
                        } else if (NBTFileType.isNBTFileByExtension(file)) {
                            try {
                                Controllers.navigate(new NBTEditorPage(file));
                            } catch (Throwable e) {
                                LOG.warning("Fail to open nbt file", e);
                                Controllers.dialog(i18n("nbt.open.failed") + "\n\n" + StringUtils.getStackTrace(e),
                                        i18n("message.error"), MessageDialogPane.MessageType.ERROR);
                            }
                        } else if ("json".equalsIgnoreCase(FileUtils.getExtension(file))) {
                            Versions.installFromJson(Profiles.getSelectedProfile(), file);
                        }
                    });

            FXUtils.onChangeAndOperate(Profiles.selectedVersionProperty(), mainPage::setCurrentGame);
            mainPage.showUpdateProperty().bind(UpdateChecker.outdatedProperty());
            mainPage.latestVersionProperty().bind(UpdateChecker.latestVersionProperty());

            Profiles.registerVersionsListener(profile -> {
                JMCLGameRepository repository = profile.getRepository();
                List<Version> children = repository.getVersions().parallelStream()
                        .filter(version -> !version.isHidden())
                        .sorted(Comparator
                                .comparing((Version version) -> Lang.requireNonNullElse(version.getReleaseTime(), Instant.EPOCH))
                                .thenComparing(version -> VersionNumber.asVersion(repository.getGameVersion(version).orElse(version.getId()))))
                        .collect(Collectors.toList());
                runInFX(() -> {
                    if (profile == Profiles.getSelectedProfile())
                        mainPage.initVersions(profile, children);
                });
            });
            this.mainPage = mainPage;
        }
        return mainPage;
    }

    private static class Skin extends DecoratorAnimatedPageSkin<RootPage> {

        protected Skin(RootPage control) {
            super(control);

            AccountAdvancedListItem accountListItem = new AccountAdvancedListItem();
            accountListItem.setOnAction(e -> Controllers.navigate(Controllers.getAccountListPage()));
            FXUtils.onSecondaryButtonClicked(accountListItem, () -> AccountListPopupMenu.show(accountListItem, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, accountListItem.getWidth(), 0));
            accountListItem.accountProperty().bind(Accounts.selectedAccountProperty());
            accountListItem.getStyleClass().add("account-header-item");

            GameAdvancedListItem gameListItem = new GameAdvancedListItem();
            gameListItem.setOnAction(e -> {
                Profile profile = Profiles.getSelectedProfile();
                String version = Profiles.getSelectedVersion();
                if (version == null) {
                    Controllers.navigate(Controllers.getGameListPage());
                } else {
                    Versions.modifyGameSettings(profile, version);
                }
            });
            FXUtils.onScroll(gameListItem, getSkinnable().getMainPage().getVersions(), list -> {
                String currentId = getSkinnable().getMainPage().getCurrentGame();
                return Lang.indexWhere(list, instance -> instance.getId().equals(currentId));
            }, it -> getSkinnable().getMainPage().getProfile().setSelectedVersion(it.getId()));
            if (AnimationUtils.isAnimationEnabled()) {
                FXUtils.prepareOnMouseEnter(gameListItem, Controllers::prepareVersionPage);
            }
            FXUtils.onSecondaryButtonClicked(gameListItem, () -> showGameListPopupMenu(gameListItem));

            AdvancedListItem gameItem = new AdvancedListItem();
            gameItem.setLeftIcon(SVG.FORMAT_LIST_BULLETED);
            gameItem.setTitle(i18n("version.manage"));
            gameItem.setOnAction(e -> Controllers.navigate(Controllers.getGameListPage()));
            FXUtils.onSecondaryButtonClicked(gameItem, () -> showGameListPopupMenu(gameItem));

            AdvancedListItem downloadItem = new AdvancedListItem();
            downloadItem.setLeftIcon(SVG.DOWNLOAD);
            downloadItem.setTitle(i18n("download"));
            downloadItem.setOnAction(e -> {
                Controllers.getDownloadPage().showGameDownloads();
                Controllers.navigate(Controllers.getDownloadPage());
            });
            FXUtils.installFastTooltip(downloadItem, i18n("download.hint"));
            if (AnimationUtils.isAnimationEnabled()) {
                FXUtils.prepareOnMouseEnter(downloadItem, Controllers::prepareDownloadPage);
            }

            AdvancedListItem launcherSettingsItem = new AdvancedListItem();
            launcherSettingsItem.setLeftIcon(SVG.SETTINGS);
            launcherSettingsItem.setTitle(i18n("settings"));
            launcherSettingsItem.setOnAction(e -> {
                Controllers.getSettingsPage().showGameSettings(Profiles.getSelectedProfile());
                Controllers.navigate(Controllers.getSettingsPage());
            });
            if (AnimationUtils.isAnimationEnabled()) {
                FXUtils.prepareOnMouseEnter(launcherSettingsItem, Controllers::prepareSettingsPage);
            }

            AdvancedListItem terracottaItem = new AdvancedListItem();
            terracottaItem.setLeftIcon(SVG.GRAPH2);
            terracottaItem.setTitle(i18n("terracotta"));
            terracottaItem.setOnAction(e -> {
                if (TerracottaMetadata.PROVIDER != null) {
                    Controllers.navigate(Controllers.getTerracottaPage());
                } else {
                    String message;
                    if (Architecture.SYSTEM_ARCH.getBits() == Bits.BIT_32)
                        message = i18n("terracotta.unsupported.arch.32bit");
                    else if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS
                            && !OperatingSystem.SYSTEM_VERSION.isAtLeast(OSVersion.WINDOWS_10))
                        message = i18n("terracotta.unsupported.os.windows.old");
                    else if (Platform.SYSTEM_PLATFORM.equals(OperatingSystem.LINUX, Architecture.LOONGARCH64_OW))
                        message = i18n("terracotta.unsupported.arch.loongarch64_ow");
                    else
                        message = i18n("terracotta.unsupported");

                    Controllers.dialog(message, null, MessageDialogPane.MessageType.WARNING);
                }
            });

            AdvancedListBox sideBar = new AdvancedListBox()
                    .startCategory(i18n("version").toUpperCase(Locale.ROOT))
                    .add(gameListItem)
                    .add(gameItem)
                    .add(downloadItem)
                    .startCategory(i18n("settings.launcher.general").toUpperCase(Locale.ROOT))
                    .add(launcherSettingsItem)
                    .add(terracottaItem)
                    .startCategory(i18n("tools").toUpperCase(Locale.ROOT))
                    .add(createNBTEditorItem())
                    .addNavigationDrawerItem(i18n("contact.chat"), SVG.CHAT, () -> {
                        Controllers.getSettingsPage().showFeedback();
                        Controllers.navigate(Controllers.getSettingsPage());
                    });

            SkinViewPane skinViewPane = new SkinViewPane();

            // Account header above skin preview
            VBox leftCenterContent = new VBox();
            leftCenterContent.getStyleClass().add("left-center-content");
            leftCenterContent.getChildren().addAll(accountListItem, skinViewPane);
            VBox.setVgrow(skinViewPane, Priority.ALWAYS);

            // Hide skin preview when not on the main page (RootPage removed from Navigator)
            skinViewPane.visibleProperty().bind(Bindings.isNotNull(control.parentProperty()));
            skinViewPane.managedProperty().bind(skinViewPane.visibleProperty());

            sideBar.getStyleClass().addAll("card", "elev-2");
            skinViewPane.getStyleClass().addAll("skin-view-pane");
            getSkinnable().getMainPage().getStyleClass().add("md3-content-area");

            setLeft(sideBar);
            setLeftCenter(leftCenterContent);
            setCenter(getSkinnable().getMainPage());

            // Prevent overflow from center (announcement) from overlapping sibling regions
            FXUtils.setOverflowHidden(control.center);
            FXUtils.setOverflowHidden(control.left);
        }

        public void showGameListPopupMenu(Region gameListItem) {
            GameListPopupMenu.show(gameListItem,
                    JFXPopup.PopupVPosition.TOP,
                    JFXPopup.PopupHPosition.LEFT,
                    gameListItem.getWidth(),
                    0,
                    getSkinnable().getMainPage().getProfile(),
                    getSkinnable().getMainPage().getVersions());
        }
    }

    private static AdvancedListItem createNBTEditorItem() {
        AdvancedListItem item = new AdvancedListItem();
        item.setLeftIcon(SVG.EDIT);
        item.setTitle(i18n("tools.nbt_editor"));
        item.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(i18n("tools.nbt_editor"));
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("NBT Files", "*.dat", "*.dat_old", "*.mca", "*.mcr"));
            File file = chooser.showOpenDialog(Controllers.getStage());
            if (file != null) {
                try {
                    Controllers.navigate(new NBTEditorPage(file.toPath()));
                } catch (Throwable ex) {
                    Controllers.dialog(i18n("nbt.open.failed") + "\n\n" + StringUtils.getStackTrace(ex),
                            i18n("message.error"), MessageDialogPane.MessageType.ERROR);
                }
            }
        });
        return item;
    }

    private boolean checkedModpack = false;

    private void onRefreshedVersions(JMCLGameRepository repository) {
        runInFX(() -> {
            if (!checkedModpack) {
                checkedModpack = true;

                if (repository.getVersionCount() == 0) {
                    Path zipModpack = Metadata.CURRENT_DIRECTORY.resolve("modpack.zip");
                    Path mrpackModpack = Metadata.CURRENT_DIRECTORY.resolve("modpack.mrpack");

                    Path modpackFile;
                    if (Files.exists(zipModpack)) {
                        modpackFile = zipModpack;
                    } else if (Files.exists(mrpackModpack)) {
                        modpackFile = mrpackModpack;
                    } else {
                        modpackFile = null;
                    }

                    if (modpackFile != null) {
                        Task.supplyAsync(() -> CompressingUtils.findSuitableEncoding(modpackFile))
                                .thenApplyAsync(encoding -> ModpackHelper.readModpackManifest(modpackFile, encoding))
                                .thenApplyAsync(modpack -> ModpackHelper
                                        .getInstallTask(repository.getProfile(), modpackFile, modpack.getName(), modpack, null)
                                        .executor())
                                .thenAcceptAsync(Schedulers.javafx(), executor -> {
                                    Controllers.taskDialog(executor, i18n("modpack.installing"), TaskCancellationAction.NO_CANCEL);
                                    executor.start();
                                }).start();
                    }
                }
            }
        });
    }
}