package org.psz80.assembler.model;

public class IndexedOperand extends Operand {

    private final String register; // IX or IY
    private final int offset;

    public IndexedOperand(String register, int offset) {
        this.register = register;
        this.offset = offset;
    }

    public String getRegister() {
        return register;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return register + (offset >= 0 ? "+" : "") + offset;
    }
}