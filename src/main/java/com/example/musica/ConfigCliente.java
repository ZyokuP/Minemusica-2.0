package com.example.musica;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Ajustes de cada jugador (archivo config/musica-client.toml). Se cambian en el juego con /musicaconfig.
 * Solo se leen en el cliente.
 */
public final class ConfigCliente {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue VOLUMEN;
    public static final ForgeConfigSpec.BooleanValue SILENCIAR_MC;
    public static final ForgeConfigSpec.BooleanValue USAR_MAESTRO;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        VOLUMEN = b.comment("Volumen de las canciones del servidor, de 0 a 200 (100 = normal).",
                        "Es independiente del control 'Música' de Minecraft.")
                .defineInRange("volumen", 100, 0, 200);
        SILENCIAR_MC = b.comment("Si es true, la música de fondo de Minecraft se calla mientras suena una canción del servidor.")
                .define("silenciarMusicaMinecraft", true);
        USAR_MAESTRO = b.comment("Si es true, el control 'Volumen general' (Maestro) de Minecraft también afecta a las canciones.",
                        "Ponlo en false para que no dependan de ningún control del juego.")
                .define("usarVolumenMaestro", true);
        SPEC = b.build();
    }

    private ConfigCliente() {}
}
