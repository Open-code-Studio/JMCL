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
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

/// Pre-fetches Minecraft changelog data using plain HttpURLConnection
/// (runs on a simple daemon thread, independent of JMCL's task system).
public final class ChangelogPrefetcher {

    public record ChangelogData(GameRemoteVersionInfo version, @Nullable String imageUrl) {}

    private static final CompletableFuture<@Nullable ChangelogData> CACHE = new CompletableFuture<>();

    private ChangelogPrefetcher() {}

    /// Starts a background thread that fetches the latest MC release info.
    public static void startPrefetch() {
        Thread t = new Thread(() -> {
            try {
                String manifest = httpGet("https://piston-meta.mojang.com/mc/game/version_manifest.json");
                GameRemoteVersions versions = JsonUtils.fromNonNullJson(manifest, GameRemoteVersions.class);
                GameRemoteVersionInfo latest = null;
                for (GameRemoteVersionInfo v : versions.versions()) {
                    if (v.type() == ReleaseType.RELEASE) {
                        if (latest == null || v.releaseTime().isAfter(latest.releaseTime())) {
                            latest = v;
                        }
                    }
                }
                if (latest == null) { CACHE.complete(null); return; }

                String wikiUrl = "https://minecraft.wiki/api.php?action=query"
                        + "&titles=Java_Edition_" + latest.gameVersion()
                        + "&prop=pageimages&format=json&pithumbsize=960";
                String wikiJson = httpGet(wikiUrl);
                String imageUrl = parseWikiImage(wikiJson);
                CACHE.complete(new ChangelogData(latest, imageUrl));
            } catch (Exception e) {
                LOG.warning("Changelog prefetch failed", e);
                CACHE.complete(null);
            }
        }, "ChangelogPrefetch");
        t.setDaemon(true);
        t.start();
    }

    public static CompletableFuture<@Nullable ChangelogData> getCachedData() {
        return CACHE;
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "JMCL");
        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static @Nullable String parseWikiImage(String json) {
        if (json == null) return null;
        try {
            JsonObject root = JsonUtils.fromNonNullJson(json, JsonObject.class);
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
        } catch (Exception ignored) {}
        return null;
    }
}
