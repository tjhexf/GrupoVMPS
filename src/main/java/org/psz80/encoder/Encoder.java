package org.psz80.encoder;

import java.util.List;
import java.util.Map;
import org.psz80.assembler.model.Instruction;
import org.psz80.assembler.model.Operand;
import org.psz80.linker.RelocationEntry;

public class Encoder {

    private final EncoderContext ctx;
    private final InstructionTable table;

    public Encoder(Map<String, Integer> symbols) {
        this(symbols, false);
    }

    // objectMode=true, gera relocações
    public Encoder(Map<String, Integer> symbols, boolean objectMode) {
        this.ctx = new EncoderContext(symbols, objectMode);
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

    public List<RelocationEntry> getRelocations() {
        return ctx.getRelocations();
    }
}
