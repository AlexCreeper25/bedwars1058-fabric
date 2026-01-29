package com.alexcreeper.bedwars1058.api.utils;

import com.alexcreeper.bedwars1058.BedWars;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class WorldUtils {

    // Copies a source directory to a target directory
    public static void copyDirectory(Path source, Path target) {
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = target.resolve(source.relativize(dir));
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Ignore session.lock to avoid crashes
                    if (file.getFileName().toString().equals("session.lock")) return FileVisitResult.CONTINUE;
                    if (file.getFileName().toString().equals("uid.dat")) return FileVisitResult.CONTINUE;

                    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            BedWars.LOGGER.error("Error copying world from " + source + " to " + target, e);
        }
    }

    // Recursively deletes a directory
    public static void deleteDirectory(Path path) {
        if (!Files.exists(path)) return;
        
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted((p1, p2) -> -p1.compareTo(p2)) // Reverse order (files first, then dirs)
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        BedWars.LOGGER.error("Unable to delete: " + p, e);
                    }
                });
        } catch (IOException e) {
            BedWars.LOGGER.error("Error deleting directory: " + path, e);
        }
    }
}