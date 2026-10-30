package com.example.musica.network;

import com.example.musica.Ofertas;
import com.example.musica.Transferencias;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "no tengo esa canción, envíamela". Solo se atiende si el jugador era destinatario. */
public class PedirPaquete {
    private final int id;

    public PedirPaquete(int id) {
        this.id = id;
    }

    public static void codificar(PedirPaquete m, FriendlyByteBuf b) {
        b.writeInt(m.id);
    }

    public static PedirPaquete decodificar(FriendlyByteBuf b) {
        return new PedirPaquete(b.readInt());
    }

    public static void manejar(PedirPaquete m, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer jugador = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (jugador == null) return;
            Ofertas.Oferta oferta = Ofertas.obtener(m.id);
            if (oferta == null || !oferta.destinatarios().contains(jugador.getUUID())) return;
            Transferencias.iniciar(jugador, oferta);
        });
        ctx.get().setPacketHandled(true);
    }
}
