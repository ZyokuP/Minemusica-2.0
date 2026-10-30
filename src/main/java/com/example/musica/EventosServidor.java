package com.example.musica;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MusicaMod.MODID)
public class EventosServidor {

    @SubscribeEvent
    public static void alIniciar(ServerStartingEvent e) {
        Carpeta.crear();
    }

    @SubscribeEvent
    public static void alCerrar(ServerStoppedEvent e) {
        Transferencias.cancelarTodo();
        Ofertas.limpiar();
    }
}
