package dev.upcraft.axiompatcher.modrinth;

import com.unascribed.flexver.FlexVerComparator;

import java.util.Arrays;
import java.util.Comparator;

public record Version(String name, String version_number, String[] game_versions, String[] loaders, boolean featured, String id, String project_id, String date_published, VersionFile[] files) {

    @Override
    public String toString() {
        var versions = Arrays.asList(game_versions());
        versions.sort(((Comparator<? super String>) FlexVerComparator::compare).reversed());
        return "%s (%s)".formatted(name(), String.join(", ", versions));
    }
}
