package org.psz80.assembler.macro;

public class Macro {
    private final String name;
    private final int mdtStartIndex;

    public Macro(String name, int mdtStartIndex) {
        this.name = name;
        this.mdtStartIndex = mdtStartIndex;
    }

    public String getName() {
        return name;
    }

    public int getMdtStartIndex() {
        return mdtStartIndex;
    }
}