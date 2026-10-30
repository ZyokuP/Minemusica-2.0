package com.example.musica.network;

import com.example.musica.MusicaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MusicaMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void registrar() {
        CANAL.registerMessage(0, OfertaPaquete.class, OfertaPaquete::codificar, OfertaPaquete::decodificar,
                OfertaPaquete::manejar, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(1, TrozoPaquete.class, TrozoPaquete::codificar, TrozoPaquete::decodificar,
                TrozoPaquete::manejar, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(2, StopPaquete.class, StopPaquete::codificar, StopPaquete::decodificar,
                StopPaquete::manejar, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(3, PedirPaquete.class, PedirPaquete::codificar, PedirPaquete::decodificar,
                PedirPaquete::manejar, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
