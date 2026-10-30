package com.example.musica;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Carpeta "musica" en la raíz del servidor: ahí se ponen los .mp3. */
public final class Carpeta {
    private static final String EXT = ".mp3";

    private Carpeta() {}

    public static Path dir() {
        return FMLPaths.GAMEDIR.get().resolve("musica");
    }

    public static void crear() {
        try {
            Files.createDirectories(dir());
        } catch (IOException ignored) {
        }
    }

    public static List<String> archivos() {
        List<String> res = new ArrayList<>();
        Path d = dir();
        if (!Files.isDirectory(d)) return res;
        try (Stream<Path> s = Files.list(d)) {
            s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(EXT))
                    .sorted()
                    .forEach(res::add);
        } catch (IOException ignored) {
        }
        return res;
    }

    /** Nombres para autocompletar (entre comillas si tienen espacios). */
    public static List<String> sugerencias() {
        List<String> res = new ArrayList<>();
        for (String n : archivos()) {
            res.add(n.contains(" ") ? "\"" + n + "\"" : n);
        }
        return res;
    }

    /** Busca el archivo (con o sin ".mp3"). Solo dentro de la carpeta musica. */
    public static Path resolver(String nombre) {
        String n = nombre.trim();
        if (n.isEmpty() || n.contains("/") || n.contains("\\") || n.contains("..")) return null;
        try {
            Path base = dir().toAbsolutePath().normalize();
            Path p = base.resolve(n).normalize();
            if (!Files.isRegularFile(p) && !n.toLowerCase(Locale.ROOT).endsWith(EXT)) {
                p = base.resolve(n + EXT).normalize();
            }
            if (!p.startsWith(base)) return null;
            if (!Files.isRegularFile(p)) return null;
            if (!p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(EXT)) return null;
            return p;
        } catch (InvalidPathException ex) {
            return null;
        }
    }
}
