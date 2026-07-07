package org.psz80.encoder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.psz80.linker.RelocationEntry;
import org.psz80.linker.RelocationType;

// ALTERADO: agora o contexto também pode registrar relocações.
public class EncoderContext {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public final Map<String, Integer> symbols;

    private final boolean objectMode;

    // lista de pontos que o ligador precisará corrigir
    private final List<RelocationEntry> relocations = new ArrayList<>();

    private int pc = 0;

    public EncoderContext(Map<String, Integer> symbols) {
        this(symbols, false);
    }

    // objectMode=true gera objeto relocável
    public EncoderContext(Map<String, Integer> symbols, boolean objectMode) {
        this.symbols = symbols;
        this.objectMode = objectMode;
    }

    public void writeByte(int b) {
        out.write(b & 0xFF);
        pc++;
    }

    public void writeWord(int w) {
        writeByte(w & 0xFF);
        writeByte((w >> 8) & 0xFF);
    }

    public int getPC() {
        return pc;
    }

    public byte[] getBytes() {
        return out.toByteArray();
    }

    public boolean isObjectMode() {
        return objectMode;
    }

    public List<RelocationEntry> getRelocations() {
        return Collections.unmodifiableList(relocations);
    }

    // escreve endereço absoluto de 16 bits a partir de símbolo
    // Em modo absoluto: resolve agora
    // Em modo objeto: deixa para o ligador
    public void writeAddress16Symbol(String symbolName) {
        if (objectMode) {
            relocations.add(
                new RelocationEntry(pc, symbolName, RelocationType.ABSOLUTE_16)
            );

            writeWord(0x0000);
            return;
        }

        Integer value = symbols.get(symbolName);

        if (value == null) {
            throw new RuntimeException("Unknown label: " + symbolName);
        }

        writeWord(value);
    }

    // imediato numérico não é símbolo, então não entra na tabela de relocação
    public void writeAddress16Immediate(int value) {
        writeWord(value);
    }

    // JR label
    public void writeRelative8Symbol(String symbolName) {
        if (objectMode) {
            relocations.add(
                new RelocationEntry(
                    pc,
                    symbolName,
                    RelocationType.PC_RELATIVE_8
                )
            );

            writeByte(0x00);
            return;
        }

        Integer target = symbols.get(symbolName);

        if (target == null) {
            throw new RuntimeException("Unknown label: " + symbolName);
        }

        writeRelative8Immediate(target);
    }

    // JR número
    public void writeRelative8Immediate(int target) {
        int offset = target - (pc + 1);

        if (offset < -128 || offset > 127) {
            throw new RuntimeException("JR target out of range: " + offset);
        }

        writeByte(offset & 0xFF);
    }
}
