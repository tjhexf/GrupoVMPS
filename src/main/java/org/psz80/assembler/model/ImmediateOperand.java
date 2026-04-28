package org.psz80.assembler.model;

public class ImmediateOperand extends Operand {

    private final int value;

    public ImmediateOperand(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}