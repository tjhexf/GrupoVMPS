package org.psz80.assembler.ast;

public class MemoryOperand extends Operand {
    public Operand inner;

    public MemoryOperand(Operand inner) {
        this.inner = inner;
    }

    @Override
    public String toString() {
        return "(" + inner + ")";
    }
}