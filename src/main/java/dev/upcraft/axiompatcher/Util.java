package dev.upcraft.axiompatcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Util {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String getVersion() {
        return Util.class.getPackage().getImplementationVersion();
    }

    public static void patchFile(Path source, Path target, DoubleConsumer progressCallback) throws IOException {
        System.out.println("Patching file " + target);

        var tmpOut = Files.createTempFile("axiom-patcher", ".tmp");

        var removeFiles = List.of(
                "org/lwjgl/BufferUtils.class",
                "org/lwjgl/CLongBuffer.class",
                "org/lwjgl/package-info.class",
                "org/lwjgl/PointerBuffer.class",
                "org/lwjgl/Version.class",
                "org/lwjgl/Version$BuildType.class",
                "org/lwjgl/VersionImpl.class"
                );
        var removePrefix = "org/lwjgl/system/";

        int size;
        try (var zip = new ZipFile(source.toFile())) {
            size = zip.size();
        }

        try (var in = new ZipInputStream(Files.newInputStream(source)); var out = new ZipOutputStream(Files.newOutputStream(tmpOut))) {
            int count = 1;
            for (var entry = in.getNextEntry(); entry != null; entry = in.getNextEntry(), count++) {
                progressCallback.accept(count / (double) size);
                if (removeFiles.contains(entry.getName()) || entry.getName().startsWith(removePrefix)) {
                    continue;
                }

                out.putNextEntry(entry);
                in.transferTo(out);
                out.closeEntry();
            }
        }
        Files.move(tmpOut, target, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Patched file " + target);
    }
}
