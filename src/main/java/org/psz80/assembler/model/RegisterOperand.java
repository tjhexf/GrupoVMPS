package org.psz80.assembler.model;

public class RegisterOperand extends Operand {
    public String name;

    public RegisterOperand(String name) {
        this.name = name.toUpperCase();
    }

    @Override
    public String toString() {
        return name;
    }
}