package com.example.musica.client;

import com.example.musica.ConfigCliente;
import com.example.musica.MusicaMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comandos de cada jugador (no necesitan ser operador):
 * /musicaconfig                          -> ver los ajustes
 * /musicaconfig volumen <0-200>          -> volumen de las canciones del servidor
 * /musicaconfig minecraft silenciar|mezclar -> callar o no la música de fondo de Minecraft mientras suenan
 */
@Mod.EventBusSubscriber(modid = MusicaMod.MODID, value = Dist.CLIENT)
public class ComandosCliente {

    @SubscribeEvent
    public static void registrar(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("musicaconfig")
                .executes(c -> estado())
                .then(Commands.literal("volumen")
                        .then(Commands.argument("porcentaje", IntegerArgumentType.integer(0, 200))
                                .executes(c -> volumen(IntegerArgumentType.getInteger(c, "porcentaje")))))
                .then(Commands.literal("minecraft")
                        .then(Commands.literal("silenciar").executes(c -> mezcla(true)))
                        .then(Commands.literal("mezclar").executes(c -> mezcla(false)))));
    }

    private static int estado() {
        msg("Música del servidor - volumen: " + ConfigCliente.VOLUMEN.get() + "%"
                + "\nMúsica de Minecraft durante las canciones: "
                + (ConfigCliente.SILENCIAR_MC.get() ? "se silencia" : "se mezcla")
                + "\nCambiar: /musicaconfig volumen <0-200>  |  /musicaconfig minecraft silenciar|mezclar");
        return 1;
    }

    private static int volumen(int v) {
        ConfigCliente.VOLUMEN.set(v);
        ConfigCliente.VOLUMEN.save();
        ReproductorAudio.recalcularVolumen();
        msg("Volumen de las canciones del servidor: " + v + "% (no depende del control 'Música' del juego).");
        return 1;
    }

    private static int mezcla(boolean silenciar) {
        ConfigCliente.SILENCIAR_MC.set(silenciar);
        ConfigCliente.SILENCIAR_MC.save();
        msg(silenciar
                ? "La música de fondo de Minecraft se callará mientras suene una canción del servidor."
                : "La música de fondo de Minecraft sonará junto con las canciones del servidor.");
        return 1;
    }

    private static void msg(String texto) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(texto), false);
    }
}
