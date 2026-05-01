package org.psz80.assembler.encoder;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class EncoderContext {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    public final Map<String, Integer> symbols;

    public EncoderContext(Map<String, Integer> symbols) {
        this.symbols = symbols;
    }

    public void writeByte(int b) {
        out.write(b & 0xFF);
    }

    public void writeWord(int w) {
        writeByte(w & 0xFF);
        writeByte((w >> 8) & 0xFF);
    }

    public byte[] getBytes() {
        return out.toByteArray();
    }
}