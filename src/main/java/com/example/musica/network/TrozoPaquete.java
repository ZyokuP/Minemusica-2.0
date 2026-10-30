package com.example.musica.network;

import com.example.musica.client.ClienteMusica;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: un trozo del archivo. */
public class TrozoPaquete {
    private final int id;
    private final byte[] datos;
    private final boolean fin;

    public TrozoPaquete(int id, byte[] datos, boolean fin) {
        this.id = id;
        this.datos = datos;
        this.fin = fin;
    }

    public static void codificar(TrozoPaquete m, FriendlyByteBuf b) {
        b.writeInt(m.id);
        b.writeBoolean(m.fin);
        b.writeByteArray(m.datos);
    }

    public static TrozoPaquete decodificar(FriendlyByteBuf b) {
        int id = b.readInt();
        boolean fin = b.readBoolean();
        byte[] datos = b.readByteArray();
        return new TrozoPaquete(id, datos, fin);
    }

    public static void manejar(TrozoPaquete m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClienteMusica.alRecibirTrozo(m.id, m.datos, m.fin)));
        ctx.get().setPacketHandled(true);
    }
}
