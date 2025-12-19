package dev.sixdev.unrrifts.core;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;

public final class FileUtil {
    private FileUtil(){}

    public static void copyWorldFolder(File src, File dst) throws IOException {
        if (dst.exists()) deleteWorldFolder(dst);
        Files.walk(src.toPath()).forEach(from -> {
            try {
                Path rel = src.toPath().relativize(from);
                Path to = dst.toPath().resolve(rel);
                if (Files.isDirectory(from)) {
                    Files.createDirectories(to);
                } else {
                    // skip uid/session locks if present
                    String n = from.getFileName().toString();
                    if (n.equalsIgnoreCase("uid.dat") || n.equalsIgnoreCase("session.lock")) return;
                    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored){}
        });
    }

    public static void deleteWorldFolder(File folder) throws IOException {
        if (folder == null || !folder.exists()) return;
        Files.walk(folder.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored){}
                });
    }
}
