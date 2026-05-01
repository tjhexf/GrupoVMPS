package org.psz80.assembler.model;

public class MemoryOperand extends Operand {

    private final Operand address;

    public MemoryOperand(Operand address) {
        this.address = address;
    }

    public Operand getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "(" + address + ")";
    }
}