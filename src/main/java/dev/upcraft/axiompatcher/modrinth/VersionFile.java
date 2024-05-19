package dev.upcraft.axiompatcher.modrinth;

public record VersionFile(VersionFileHashes hashes, String url, String filename, boolean primary, long size) {
}
