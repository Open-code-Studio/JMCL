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
package org.Open_code_Studio.jmcl.ui.main;

import org.Open_code_Studio.jmcl.download.game.GameRemoteVersionInfo;
import org.Open_code_Studio.jmcl.download.game.GameRemoteVersions;
import org.Open_code_Studio.jmcl.game.ReleaseType;
import org.Open_code_Studio.jmcl.task.GetTask;
import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

/// Pre-fetches Minecraft changelog data during app splash-screen,
/// so it's available immediately when the main window appears.
public final class ChangelogPrefetcher {

    /// Holds pre-fetched changelog data: version info + optional wiki image URL.
    public record ChangelogData(GameRemoteVersionInfo version, @Nullable String imageUrl) {}

    private static final CompletableFuture<@Nullable ChangelogData> CACHE = new CompletableFuture<>();
    private static final AtomicBoolean started = new AtomicBoolean();

    private ChangelogPrefetcher() {}

    /// Starts a background task chain that fetches the latest MC release
    /// and its wiki banner. Idempotent — only runs once.
    public static void startPrefetch() {
        if (!started.compareAndSet(false, true)) return;

        new GetTask(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest.json"))
                .thenGetJsonAsync(GameRemoteVersions.class)
                .thenComposeAsync(versions -> {
                    GameRemoteVersionInfo latest = findLatestRelease(versions);
                    if (latest == null) {
                        CACHE.complete(null);
                        return Task.completed(null);
                    }

                    String wikiUrl = "https://minecraft.wiki/api.php?action=query"
                            + "&titles=Java_Edition_" + latest.gameVersion()
                            + "&prop=pageimages&format=json&pithumbsize=960";

                    return new GetTask(URI.create(wikiUrl)).thenApplyAsync(wikiJson -> {
                        CACHE.complete(new ChangelogData(latest, parseWikiImageUrl(wikiJson)));
                        return null;
                    });
                })
                .whenComplete(Schedulers.defaultScheduler(), (ignored, ex) -> {
                    if (ex != null) {
                        LOG.warning("Changelog prefetch failed", ex);
                        CACHE.complete(null);
                    }
                }).start();
    }

    /// Returns a future that completes when the changelog data is available.
    public static CompletableFuture<@Nullable ChangelogData> getCachedData() {
        return CACHE;
    }

    private static @Nullable GameRemoteVersionInfo findLatestRelease(GameRemoteVersions versions) {
        GameRemoteVersionInfo latest = null;
        for (GameRemoteVersionInfo v : versions.versions()) {
            if (v.type() == ReleaseType.RELEASE) {
                if (latest == null || v.releaseTime().isAfter(latest.releaseTime())) {
                    latest = v;
                }
            }
        }
        return latest;
    }

    private static @Nullable String parseWikiImageUrl(@Nullable String wikiJson) {
        if (wikiJson == null) return null;
        try {
            JsonObject root = JsonUtils.fromNonNullJson(wikiJson, JsonObject.class);
            JsonObject query = root.getAsJsonObject("query");
            if (query != null) {
                JsonObject pages = query.getAsJsonObject("pages");
                if (pages != null) {
                    for (var entry : pages.entrySet()) {
                        JsonObject page = entry.getValue().getAsJsonObject();
                        if (page != null && page.has("thumbnail")) {
                            return page.getAsJsonObject("thumbnail").get("source").getAsString();
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("Failed to parse wiki image", e);
        }
        return null;
    }
}
