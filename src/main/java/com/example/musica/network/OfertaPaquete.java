package com.example.musica.network;

import com.example.musica.client.ClienteMusica;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: "voy a reproducir esta canción". El cliente la descarga si no la tiene en caché. */
public class OfertaPaquete {
    private final int id;
    private final String nombre;
    private final long tam;
    private final String clave;

    public OfertaPaquete(int id, String nombre, long tam, String clave) {
        this.id = id;
        this.nombre = nombre;
        this.tam = tam;
        this.clave = clave;
    }

    public static void codificar(OfertaPaquete m, FriendlyByteBuf b) {
        b.writeInt(m.id);
        b.writeUtf(m.nombre, 256);
        b.writeLong(m.tam);
        b.writeUtf(m.clave, 64);
    }

    public static OfertaPaquete decodificar(FriendlyByteBuf b) {
        return new OfertaPaquete(b.readInt(), b.readUtf(256), b.readLong(), b.readUtf(64));
    }

    public static void manejar(OfertaPaquete m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClienteMusica.alRecibirOferta(m.id, m.nombre, m.tam, m.clave)));
        ctx.get().setPacketHandled(true);
    }
}
