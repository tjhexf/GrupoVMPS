package org.psz80.assembler.pass;

import org.psz80.assembler.model.Node;
import org.psz80.assembler.model.Instruction;
import org.psz80.assembler.encoder.Encoder;

import java.util.List;
import java.util.Map;

public class Pass2 {

    private final Encoder encoder;

    public Pass2(Map<String, Integer> symbols) {
        this.encoder = new Encoder(symbols);
    }

    public byte[] run(List<Node> program) {

        for (Node node : program) {

            if (node instanceof Instruction inst) {
                encoder.encode(inst);
            }

        }

        return encoder.getBytes();
    }
}