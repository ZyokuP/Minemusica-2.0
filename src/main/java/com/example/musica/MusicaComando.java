package com.example.musica;

import com.example.musica.network.ModNetwork;
import com.example.musica.network.OfertaPaquete;
import com.example.musica.network.StopPaquete;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * /musica <archivo> [jugadores]   -> reproduce una canción (sin jugadores = todos)
 * /musica stop [jugadores]        -> detiene (sin jugadores = todos)
 * /musica lista                   -> muestra los .mp3 de la carpeta musica/
 * Solo operadores.
 */
@Mod.EventBusSubscriber(modid = MusicaMod.MODID)
public class MusicaComando {

    private static final long MAX_BYTES = 300L * 1024 * 1024;

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("musica")
                .requires(src -> src.hasPermission(2))
                .executes(c -> ayuda(c.getSource()));

        raiz.then(Commands.literal("lista").executes(c -> lista(c.getSource())));

        raiz.then(Commands.literal("stop")
                .executes(c -> detener(c.getSource(), todos(c.getSource())))
                .then(Commands.argument("jugadores", EntityArgument.players())
                        .executes(c -> detener(c.getSource(), EntityArgument.getPlayers(c, "jugadores")))));

        raiz.then(Commands.argument("archivo", StringArgumentType.string())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Carpeta.sugerencias(), b))
                .executes(c -> reproducir(c.getSource(),
                        StringArgumentType.getString(c, "archivo"), todos(c.getSource())))
                .then(Commands.argument("jugadores", EntityArgument.players())
                        .executes(c -> reproducir(c.getSource(),
                                StringArgumentType.getString(c, "archivo"),
                                EntityArgument.getPlayers(c, "jugadores")))));

        e.getDispatcher().register(raiz);
    }

    private static Collection<ServerPlayer> todos(CommandSourceStack src) {
        return src.getServer().getPlayerList().getPlayers();
    }

    // ------------------------------------------------------------------ comandos

    private static int ayuda(CommandSourceStack src) {
        String t = """
                === Música ===
                /musica <archivo> [jugadores]  -> reproduce (sin jugadores = todos)
                /musica stop [jugadores]  -> detiene
                /musica lista  -> canciones disponibles
                Pon los .mp3 en la carpeta musica/ del servidor.
                Ejemplos: /musica cancion   |   /musica cancion Steve   |   /musica cancion @a""";
        src.sendSuccess(() -> Component.literal(t), false);
        return 1;
    }

    private static int lista(CommandSourceStack src) {
        Carpeta.crear();
        List<String> archivos = Carpeta.archivos();
        String texto = "Carpeta: " + Carpeta.dir().toAbsolutePath()
                + "\n.mp3 (" + archivos.size() + "): "
                + (archivos.isEmpty() ? "-" : String.join(", ", archivos));
        src.sendSuccess(() -> Component.literal(texto), false);
        return 1;
    }

    private static int reproducir(CommandSourceStack src, String nombre, Collection<ServerPlayer> jugadores) {
        Path ruta = Carpeta.resolver(nombre);
        if (ruta == null) {
            src.sendFailure(Component.literal("No encuentro '" + nombre + "' en la carpeta musica/. Usa /musica lista."));
            return 0;
        }
        long[] datos = tamanoYFecha(ruta);
        if (datos == null) {
            src.sendFailure(Component.literal("No se pudo leer el archivo."));
            return 0;
        }
        long tam = datos[0];
        if (tam <= 0 || tam > MAX_BYTES) {
            src.sendFailure(Component.literal("El archivo está vacío o pesa más de 300 MB."));
            return 0;
        }
        if (jugadores.isEmpty()) {
            src.sendFailure(Component.literal("No hay jugadores a quienes enviarla."));
            return 0;
        }

        String nombreArchivo = ruta.getFileName().toString();
        String clave = clave(nombreArchivo, tam, datos[1]);
        Set<UUID> destinatarios = new HashSet<>();
        for (ServerPlayer j : jugadores) destinatarios.add(j.getUUID());

        Ofertas.Oferta oferta = Ofertas.crear(ruta, nombreArchivo, tam, clave, destinatarios);
        OfertaPaquete paquete = new OfertaPaquete(oferta.id(), nombreArchivo, tam, clave);
        for (ServerPlayer j : jugadores) {
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), paquete);
        }

        int n = jugadores.size();
        String mb = String.format(Locale.ROOT, "%.1f", tam / 1048576.0);
        src.sendSuccess(() -> Component.literal("Enviando '" + nombreArchivo + "' (" + mb + " MB) a "
                + n + " jugador(es). Suena al terminar de descargarse en cada uno."), true);
        return n;
    }

    private static int detener(CommandSourceStack src, Collection<ServerPlayer> jugadores) {
        StopPaquete stop = new StopPaquete();
        for (ServerPlayer j : jugadores) {
            Transferencias.cancelar(j.getUUID());
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), stop);
        }
        int n = jugadores.size();
        src.sendSuccess(() -> Component.literal("Música detenida para " + n + " jugador(es)."), true);
        return n;
    }

    // ------------------------------------------------------------------ utilidades

    private static long[] tamanoYFecha(Path ruta) {
        try {
            return new long[]{Files.size(ruta), Files.getLastModifiedTime(ruta).toMillis()};
        } catch (IOException ex) {
            return null;
        }
    }

    /** Identifica la versión exacta del archivo; sirve para la caché de los clientes. */
    private static String clave(String nombre, long tam, long fecha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest((nombre + "|" + tam + "|" + fecha).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException ex) {
            return Long.toHexString(tam) + Long.toHexString(fecha);
        }
    }
}
