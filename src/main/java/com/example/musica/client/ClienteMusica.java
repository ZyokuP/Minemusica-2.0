package com.example.musica.client;

import com.example.musica.ConfigCliente;
import com.example.musica.network.ModNetwork;
import com.example.musica.network.PedirPaquete;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Lado cliente: descarga la canción (si no está en la caché musica_cache/) y la reproduce.
 * Los archivos se guardan con un nombre único por versión, así que la próxima vez suena al instante.
 */
public class ClienteMusica {
    private static int idActual = -1;
    private static OutputStream salida;
    private static Path temporal;
    private static Path destino;
    private static long recibido;
    private static long total;
    private static String nombreActual;
    private static int ultimoPct = -1;

    public static void alRecibirOferta(int id, String nombre, long tam, String clave) {
        detener();
        Path cache = FMLPaths.GAMEDIR.get().resolve("musica_cache");
        Path archivo = cache.resolve(clave + ".mp3");
        try {
            Files.createDirectories(cache);
            if (Files.isRegularFile(archivo) && Files.size(archivo) == tam) {
                reproducir(archivo, nombre);
                return;
            }
            temporal = cache.resolve(clave + ".part");
            salida = new BufferedOutputStream(Files.newOutputStream(temporal));
        } catch (IOException ex) {
            aviso("No se pudo preparar la descarga de " + nombre + ".");
            return;
        }
        destino = archivo;
        idActual = id;
        recibido = 0;
        total = tam;
        nombreActual = nombre;
        ultimoPct = -1;
        ModNetwork.CANAL.sendToServer(new PedirPaquete(id));
        aviso("Descargando " + nombre + "...");
    }

    public static void alRecibirTrozo(int id, byte[] datos, boolean fin) {
        if (id != idActual || salida == null) return;
        try {
            salida.write(datos);
            recibido += datos.length;
            int pct = total > 0 ? (int) (recibido * 100 / total) : 0;
            if (pct >= ultimoPct + 10 && pct < 100) {
                ultimoPct = pct;
                aviso("Descargando " + nombreActual + "... " + pct + "%");
            }
            if (fin) {
                salida.close();
                salida = null;
                if (recibido != total) {
                    aviso("La descarga de " + nombreActual + " quedó incompleta.");
                    limpiarDescarga();
                    return;
                }
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
                Path ruta = destino;
                String nombre = nombreActual;
                idActual = -1;
                reproducir(ruta, nombre);
            }
        } catch (IOException ex) {
            aviso("Error al guardar " + nombreActual + ".");
            limpiarDescarga();
        }
    }

    private static void reproducir(Path archivo, String nombre) {
        if (ConfigCliente.SILENCIAR_MC.get()) {
            // corta la música de fondo de Minecraft que esté sonando; mientras suene la canción no empieza otra
            Minecraft.getInstance().getMusicManager().stopPlaying();
        }
        ReproductorAudio.reproducir(archivo);
        aviso("Reproduciendo: " + nombre);
    }

    public static void detener() {
        ReproductorAudio.detener();
        limpiarDescarga();
    }

    private static void limpiarDescarga() {
        idActual = -1;
        if (salida != null) {
            try {
                salida.close();
            } catch (IOException ignored) {
            }
            salida = null;
        }
        if (temporal != null) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException ignored) {
            }
            temporal = null;
        }
    }

    static void aviso(String texto) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal(texto), true);
        });
    }
}
