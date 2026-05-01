package org.psz80.assembler.encoder;

import org.psz80.assembler.model.Operand;

public class InstructionPattern {

    public final String mnemonic;
    public final OperandType[] types;
    private final EncoderFn fn;

    public interface EncoderFn {
        void apply(Operand[] ops, EncoderContext ctx);
    }

    public InstructionPattern(String mnemonic, OperandType[] types, EncoderFn fn) {
        this.mnemonic = mnemonic;
        this.types = types;
        this.fn = fn;
    }

    public void encode(Operand[] ops, EncoderContext ctx) {
        fn.apply(ops, ctx);
    }
}