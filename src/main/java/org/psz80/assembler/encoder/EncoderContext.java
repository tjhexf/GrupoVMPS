package org.psz80.assembler.encoder;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class EncoderContext {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    public final Map<String, Integer> symbols;

    private int pc = 0;

    public EncoderContext(Map<String, Integer> symbols) {
        this.symbols = symbols;
    }

    public void writeByte(int b) {
        out.write(b & 0xFF);
        pc++;
    }

    public void writeWord(int w) {
        writeByte(w & 0xFF);
        writeByte((w >> 8) & 0xFF);
    }

    public int getPC() { // ✅ ADD THIS
        return pc;
    }

    public byte[] getBytes() {
        return out.toByteArray();
    }
}