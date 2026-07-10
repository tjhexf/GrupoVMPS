package org.psz80.linker;

public class RelocationEntry {

    private final int offset;
    private final String symbolName;
    private final RelocationType type;

    public RelocationEntry(int offset, String symbolName, RelocationType type) {
        this.offset = offset;
        this.symbolName = symbolName;
        this.type = type;
    }

    public int getOffset() {
        return offset;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public RelocationType getType() {
        return type;
    }

}
