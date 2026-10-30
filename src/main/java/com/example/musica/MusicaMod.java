package com.example.musica;

import com.example.musica.network.ModNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(MusicaMod.MODID)
public class MusicaMod {
    public static final String MODID = "musica";

    public MusicaMod() {
        ModNetwork.registrar();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigCliente.SPEC);
    }
}
