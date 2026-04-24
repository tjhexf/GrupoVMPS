package org.psz80.assembler.ast;

import java.util.List;

public class Instruction {
    public String mnemonic;
    public List<Operand> operands;

    public Instruction(String mnemonic, List<Operand> operands) {
        this.mnemonic = mnemonic.toUpperCase();
        this.operands = operands;
    }
}