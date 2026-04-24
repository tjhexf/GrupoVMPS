package org.psz80.assembler.ast;

public class ImmediateOperand extends Operand {
    public int value;

    public ImmediateOperand(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}