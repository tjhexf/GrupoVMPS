package org.psz80.assembler.model;

import java.util.List;

public class Instruction implements Node {
    public String mnemonic;
    public List<Operand> operands;

    public Instruction(String mnemonic, List<Operand> operands) {
        this.mnemonic = mnemonic.toUpperCase();
        this.operands = operands;
    }

    @Override
    public String toString() {
        return "Instruction(" + mnemonic + ")";
    }
}