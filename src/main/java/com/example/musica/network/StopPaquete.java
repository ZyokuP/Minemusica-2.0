package com.example.musica.network;

import com.example.musica.client.ClienteMusica;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: detener lo que se esté reproduciendo o descargando. */
public class StopPaquete {

    public static void codificar(StopPaquete m, FriendlyByteBuf b) {
    }

    public static StopPaquete decodificar(FriendlyByteBuf b) {
        return new StopPaquete();
    }

    public static void manejar(StopPaquete m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> ClienteMusica::detener));
        ctx.get().setPacketHandled(true);
    }
}
