package com.example.musica;

import com.example.musica.network.ModNetwork;
import com.example.musica.network.TrozoPaquete;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Envía el archivo al jugador poco a poco (unos pocos trozos por tick) para no saturar el servidor. */
@Mod.EventBusSubscriber(modid = MusicaMod.MODID)
public class Transferencias {

    private static final int TROZO = 30_000;
    private static final int TROZOS_POR_TICK = 6;

    private static final class Envio {
        final Ofertas.Oferta oferta;
        final InputStream in;
        long enviado = 0;

        Envio(Ofertas.Oferta oferta, InputStream in) {
            this.oferta = oferta;
            this.in = in;
        }
    }

    private static final Map<UUID, Envio> ENVIOS = new HashMap<>();

    public static void iniciar(ServerPlayer jugador, Ofertas.Oferta oferta) {
        cancelar(jugador.getUUID());
        try {
            InputStream in = new BufferedInputStream(Files.newInputStream(oferta.ruta()), 1 << 16);
            ENVIOS.put(jugador.getUUID(), new Envio(oferta, in));
        } catch (IOException ex) {
            // no se pudo abrir: el cliente simplemente no recibirá nada
        }
    }

    public static void cancelar(UUID id) {
        Envio e = ENVIOS.remove(id);
        if (e != null) cerrar(e);
    }

    public static void cancelarTodo() {
        for (Envio e : ENVIOS.values()) cerrar(e);
        ENVIOS.clear();
    }

    private static void cerrar(Envio e) {
        try {
            e.in.close();
        } catch (IOException ignored) {
        }
    }

    @SubscribeEvent
    public static void alTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ENVIOS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, Envio>> it = ENVIOS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Envio> entrada = it.next();
            Envio envio = entrada.getValue();
            ServerPlayer jugador = server.getPlayerList().getPlayer(entrada.getKey());
            if (jugador == null) {
                cerrar(envio);
                it.remove();
                continue;
            }
            try {
                for (int i = 0; i < TROZOS_POR_TICK; i++) {
                    byte[] buf = new byte[TROZO];
                    int n = envio.in.readNBytes(buf, 0, TROZO);
                    envio.enviado += n;
                    boolean fin = n < TROZO || envio.enviado >= envio.oferta.tam();
                    byte[] datos = (n == TROZO) ? buf : Arrays.copyOf(buf, n);
                    ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador),
                            new TrozoPaquete(envio.oferta.id(), datos, fin));
                    if (fin) {
                        cerrar(envio);
                        it.remove();
                        break;
                    }
                }
            } catch (IOException ex) {
                cerrar(envio);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent e) {
        cancelar(e.getEntity().getUUID());
    }
}
