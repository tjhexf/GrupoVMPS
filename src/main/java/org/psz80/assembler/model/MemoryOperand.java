package org.psz80.assembler.model;

public class MemoryOperand extends Operand {

    private final Operand inner;

    public MemoryOperand(Operand inner) {
        this.inner = inner;
    }

    public Operand getInner() {
        return inner;
    }

    @Override
    public String toString() {
        return "(" + inner + ")";
    }
}