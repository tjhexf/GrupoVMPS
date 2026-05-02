package org.psz80.assembler.encoder;

import org.psz80.assembler.model.Operand;

public class InstructionPattern {

    public final String mnemonic;
    public final OperandType[] types;
    private final EncoderFn fn;
    private final SizeFn sizeFn;

    public interface EncoderFn {
        void apply(Operand[] ops, EncoderContext ctx);
    }

    public interface SizeFn {
        int size(Operand[] ops);
    }

    public InstructionPattern(String mnemonic, OperandType[] types,
                              EncoderFn fn,
                              SizeFn sizeFn) {
        this.mnemonic = mnemonic;
        this.types = types;
        this.fn = fn;
        this.sizeFn = sizeFn;
    }

    public void encode(Operand[] ops, EncoderContext ctx) {
        fn.apply(ops, ctx);
    }

    public int size(Operand[] ops) {
        return sizeFn.size(ops);
    }
}