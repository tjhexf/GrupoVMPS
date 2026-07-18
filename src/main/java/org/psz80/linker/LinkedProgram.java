package org.psz80.linker;

import java.util.Collections;
import java.util.List;

public class LinkedProgram {

    private final byte[] bytes;
    private final List<RelocationEntry> loaderRelocations;

    public LinkedProgram(
        byte[] bytes,
        List<RelocationEntry> loaderRelocations
    ) {
        this.bytes = bytes.clone();
        this.loaderRelocations = List.copyOf(loaderRelocations);
    }

    public byte[] getBytes() {
        return bytes.clone();
    }

    public List<RelocationEntry> getLoaderRelocations() {
        return Collections.unmodifiableList(loaderRelocations);
    }

    public int[] toIntArray() {
        return toIntArray(bytes);
    }

    public static int[] toIntArray(byte[] data) {
        int[] result = new int[data.length];

        for (int i = 0; i < data.length; i++) {
            result[i] = data[i] & 0xFF;
        }

        return result;
    }

    // usado quando o ligador rodou em modo LINK_ONLY , carregador relocador aplica o endereço de carga depois.
    public byte[] relocateAt(int loadAddress) {
        byte[] relocated = bytes.clone();

        for (RelocationEntry relocation : loaderRelocations) {
            if (relocation.getType() != RelocationType.ABSOLUTE_16) {
                throw new RuntimeException(
                    "Loader relocation unsupported: " + relocation.getType()
                );
            }

            int offset = relocation.getOffset();
            int oldValue = readWord(relocated, offset);
            int newValue = (oldValue + loadAddress) & 0xFFFF;

            writeWord(relocated, offset, newValue);
        }

        return relocated;
    }

    private static int readWord(byte[] data, int offset) {
        int low = data[offset] & 0xFF;
        int high = data[offset + 1] & 0xFF;

        return ((high << 8) | low) & 0xFFFF;
    }

    private static void writeWord(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
}
