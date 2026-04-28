package org.psz80.assembler.model;

// jolene: label que é um operando (pra jump)
public class IdentifierOperand extends Operand {

    private final String name;

    public IdentifierOperand(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}