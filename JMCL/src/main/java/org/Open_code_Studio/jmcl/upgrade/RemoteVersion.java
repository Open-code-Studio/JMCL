/*
 * JMCL
 * Copyright (C) 2020  Open Code Studio and contributors
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
package org.Open_code_Studio.jmcl.upgrade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.task.FileDownloadTask.IntegrityCheck;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.Open_code_Studio.jmcl.util.io.NetworkUtils;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.util.Optional;

public record RemoteVersion(UpdateChannel channel, String version, String tag, String url, Type type, IntegrityCheck integrityCheck,
                            boolean preview, boolean force) {

    private static final String GITHUB_API_RELEASES = "https://api.github.com/repos/Open-code-Studio/JMCL/releases";

    public static RemoteVersion fetch(UpdateChannel channel, boolean preview, String url) throws IOException {
        try {
            JsonObject response = JsonUtils.fromNonNullJson(NetworkUtils.doGet(url), JsonObject.class);
            String version = Optional.ofNullable(response.get("version")).map(JsonElement::getAsString).orElseThrow(() -> new IOException("version is missing"));
            String jarUrl = Optional.ofNullable(response.get("jar")).map(JsonElement::getAsString).orElse(null);
            String jarHash = Optional.ofNullable(response.get("jarsha1")).map(JsonElement::getAsString).orElse(null);
            boolean force = Optional.ofNullable(response.get("force")).map(JsonElement::getAsBoolean).orElse(false);
            if (jarUrl != null && jarHash != null) {
                return new RemoteVersion(channel, version, version, jarUrl, Type.JAR, new IntegrityCheck("SHA-1", jarHash), preview, force);
            } else {
                throw new IOException("No download url is available");
            }
        } catch (JsonParseException e) {
            throw new IOException("Malformed response", e);
        }
    }

    public static RemoteVersion fetchFromGitHub(UpdateChannel channel, boolean preview) throws IOException {
        try {
            URLConnection connection = NetworkUtils.createConnection(URI.create(GITHUB_API_RELEASES));
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            String responseText = NetworkUtils.readFullyAsString(connection);

            JsonArray releases;
            try {
                releases = JsonUtils.fromNonNullJson(responseText, JsonArray.class);
            } catch (JsonParseException e) {
                String message = Optional.ofNullable(JsonUtils.fromNonNullJson(responseText, JsonObject.class))
                        .map(obj -> Optional.ofNullable(obj.get("message")).map(JsonElement::getAsString).orElse(null))
                        .orElse(null);
                if (message != null && message.contains("rate limit")) {
                    throw new IOException("GitHub API rate limit exceeded. Please try again later.");
                }
                if (message != null && message.contains("Not Found")) {
                    throw new IOException("Repository not found");
                }
                throw new IOException("Malformed response: " + responseText);
            }

            JsonObject latestRelease = null;
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                // Skip draft releases
                JsonElement draftElement = release.get("draft");
                if (draftElement != null && draftElement.getAsBoolean()) {
                    continue;
                }
                // If preview is not enabled, skip pre-releases too
                if (!preview) {
                    JsonElement prereleaseElement = release.get("prerelease");
                    if (prereleaseElement != null && prereleaseElement.getAsBoolean()) {
                        continue;
                    }
                }
                latestRelease = release;
                break;
            }

            if (latestRelease == null) {
                throw new IOException("No published release found");
            }

            // Preserve the original tag name as-is (e.g. "v2026.1.0" or "DEV2026.2.0")
            String tagName = Optional.ofNullable(latestRelease.get("tag_name")).map(JsonElement::getAsString)
                    .orElseThrow(() -> new IOException("tag_name is missing"));

            // Strip leading "v" for the semantic version string only
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;

            JsonArray assets = Optional.ofNullable(latestRelease.get("assets")).map(JsonElement::getAsJsonArray)
                    .orElseThrow(() -> new IOException("assets is missing"));

            String jarUrl = null;

            // On macOS, prefer the dedicated "macOS.jar" asset
            if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS) {
                for (JsonElement assetElement : assets) {
                    JsonObject asset = assetElement.getAsJsonObject();
                    String name = Optional.ofNullable(asset.get("name")).map(JsonElement::getAsString).orElse("");
                    if ("macOS.jar".equals(name)) {
                        jarUrl = Optional.ofNullable(asset.get("browser_download_url")).map(JsonElement::getAsString).orElse(null);
                        break;
                    }
                }
            }

            // Fallback: match JAR files that contain the project name (JVM-MCL)
            // This matches: JVM-MCL-DEV2026.2.1.jar, JVM-MCL-2026.1.0.jar, etc.
            if (jarUrl == null) {
                for (JsonElement assetElement : assets) {
                    JsonObject asset = assetElement.getAsJsonObject();
                    String name = Optional.ofNullable(asset.get("name")).map(JsonElement::getAsString).orElse("");
                    if (name.endsWith(".jar") && name.startsWith(Metadata.NAME)) {
                        jarUrl = Optional.ofNullable(asset.get("browser_download_url")).map(JsonElement::getAsString).orElse(null);
                        break;
                    }
                }
            }

            if (jarUrl == null) {
                // Fallback: try any JAR file in the release
                for (JsonElement assetElement : assets) {
                    JsonObject asset = assetElement.getAsJsonObject();
                    String name = Optional.ofNullable(asset.get("name")).map(JsonElement::getAsString).orElse("");
                    if (name.endsWith(".jar")) {
                        jarUrl = Optional.ofNullable(asset.get("browser_download_url")).map(JsonElement::getAsString).orElse(null);
                        break;
                    }
                }
            }

            if (jarUrl == null) {
                throw new IOException("No JAR asset found in the latest release");
            }

            // When fetching from GitHub, we don't have a pre-computed SHA-1 hash,
            // so integrityCheck is null. The FileDownloadTask will handle this gracefully.
            return new RemoteVersion(channel, version, tagName, jarUrl, Type.JAR, null, preview, false);
        } catch (JsonParseException e) {
            throw new IOException("Malformed response", e);
        }
    }

    @Override
    public @NotNull String toString() {
        return "[" + version + " from " + url + "]";
    }

    public enum Type {
        JAR
    }
}