package org.psz80.assembler.pass;

import java.util.List;
import java.util.Map;
import org.psz80.assembler.model.Instruction;
import org.psz80.assembler.model.Node;
import org.psz80.encoder.Encoder;
import org.psz80.linker.RelocationEntry;

// fer: aqui mudou pra gerar relocações
public class Pass2 {

    private final Encoder encoder;

    public Pass2(Map<String, Integer> symbols) {
        this(symbols, false);
    }

    public Pass2(Map<String, Integer> symbols, boolean objectMode) {
        this.encoder = new Encoder(symbols, objectMode);
    }

    public byte[] run(List<Node> program) {
        for (Node node : program) {
            if (node instanceof Instruction inst) {
                encoder.encode(inst);
            }
        }

        return encoder.getBytes();
    }

    public List<RelocationEntry> getRelocations() {
        return encoder.getRelocations();
    }
}
