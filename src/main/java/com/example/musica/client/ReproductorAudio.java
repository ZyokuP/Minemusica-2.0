package com.example.musica.client;

import com.example.musica.ConfigCliente;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decodifica el MP3 por streaming (JLayer) y lo reproduce por su cuenta, fuera del motor de sonido de Minecraft.
 * El volumen NO depende del control "Música": usa el ajuste propio (/musicaconfig volumen) y,
 * si está activado, el volumen general (Maestro).
 */
public class ReproductorAudio {
    private static volatile Hilo actual;
    private static volatile float volumen = 1f;

    public static boolean sonando() {
        return actual != null;
    }

    /** Recalcula el volumen final. Llamar desde el hilo principal del juego. */
    public static void recalcularVolumen() {
        try {
            float v = ConfigCliente.VOLUMEN.get() / 100f;
            if (ConfigCliente.USAR_MAESTRO.get()) {
                v *= Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            }
            volumen = Math.max(0f, v);
        } catch (Exception ignored) {
        }
    }

    public static void reproducir(Path archivo) {
        detener();
        recalcularVolumen();
        Hilo h = new Hilo(archivo);
        actual = h;
        h.start();
    }

    public static void detener() {
        Hilo h = actual;
        actual = null;
        if (h != null) h.parar();
    }

    private static final class Hilo extends Thread {
        private final Path archivo;
        private volatile boolean parar = false;

        Hilo(Path archivo) {
            super("musica-audio");
            setDaemon(true);
            this.archivo = archivo;
        }

        void parar() {
            parar = true;
        }

        @Override
        public void run() {
            SourceDataLine linea = null;
            try (InputStream in = new BufferedInputStream(Files.newInputStream(archivo), 1 << 16)) {
                Bitstream bs = new Bitstream(in);
                Decoder dec = new Decoder();
                Header h = bs.readFrame();
                if (h == null) return;
                SampleBuffer sb = (SampleBuffer) dec.decodeFrame(h, bs);

                AudioFormat fmt = new AudioFormat(dec.getOutputFrequency(), 16, dec.getOutputChannels(), true, false);
                linea = AudioSystem.getSourceDataLine(fmt);
                linea.open(fmt);
                aplicarVolumen(linea);
                linea.start();

                int tramas = 0;
                while (h != null && !parar) {
                    short[] pcm = sb.getBuffer();
                    int len = sb.getBufferLength();
                    byte[] bytes = new byte[len * 2];
                    for (int i = 0; i < len; i++) {
                        bytes[2 * i] = (byte) (pcm[i] & 0xFF);
                        bytes[2 * i + 1] = (byte) ((pcm[i] >> 8) & 0xFF);
                    }
                    linea.write(bytes, 0, bytes.length);
                    bs.closeFrame();
                    if (++tramas % 10 == 0) aplicarVolumen(linea);
                    h = bs.readFrame();
                    if (h != null) sb = (SampleBuffer) dec.decodeFrame(h, bs);
                }
                if (!parar) linea.drain();
            } catch (Exception ex) {
                if (!parar) ClienteMusica.aviso("No se pudo reproducir el audio (¿archivo MP3 dañado?).");
            } finally {
                if (linea != null) {
                    linea.stop();
                    linea.close();
                }
                if (actual == this) actual = null; // terminó la canción: ya no "suena"
            }
        }
    }

    private static void aplicarVolumen(SourceDataLine linea) {
        try {
            if (linea.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl c = (FloatControl) linea.getControl(FloatControl.Type.MASTER_GAIN);
                float v = volumen;
                float db = v <= 0.0001f ? c.getMinimum() : (float) (20.0 * Math.log10(v));
                c.setValue(Math.max(c.getMinimum(), Math.min(c.getMaximum(), db)));
            }
        } catch (Exception ignored) {
        }
    }
}
