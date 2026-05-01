package org.psz80.assembler.encoder;

import org.psz80.assembler.model.Instruction;
import org.psz80.assembler.model.Operand;

import java.util.Map;

public class Encoder {

    private final EncoderContext ctx;
    private final InstructionTable table;

    public Encoder(Map<String, Integer> symbols) {
        this.ctx = new EncoderContext(symbols);
        this.table = new InstructionTable();
    }

    public void encode(Instruction inst) {
        InstructionPattern pattern = table.find(inst);
        Operand[] ops = inst.getOperands().toArray(new Operand[0]);
        pattern.encode(ops, ctx);
    }

    public byte[] getBytes() {
        return ctx.getBytes();
    }
}