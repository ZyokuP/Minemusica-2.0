package com.example.musica.client;

import com.example.musica.ConfigCliente;
import com.example.musica.MusicaMod;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MusicaMod.MODID, value = Dist.CLIENT)
public class EventosMusica {
    private static int contador = 0;

    @SubscribeEvent
    public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
        ClienteMusica.detener();
    }

    /** Mientras suena una canción, mantiene el volumen al día (por si cambian los controles del juego). */
    @SubscribeEvent
    public static void alTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !ReproductorAudio.sonando()) return;
        if (++contador % 10 == 0) ReproductorAudio.recalcularVolumen();
    }

    /** Mientras suena una canción del servidor, la música de fondo de Minecraft no empieza. */
    @SubscribeEvent
    public static void alSonar(PlaySoundEvent e) {
        if (!ReproductorAudio.sonando() || !ConfigCliente.SILENCIAR_MC.get()) return;
        SoundInstance s = e.getSound();
        if (s != null && s.getSource() == SoundSource.MUSIC) e.setSound(null);
    }
}
