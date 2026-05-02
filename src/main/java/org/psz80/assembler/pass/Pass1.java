package org.psz80.assembler.pass;

import org.psz80.assembler.model.*;
import org.psz80.assembler.encoder.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pass1 {

    private final Map<String, Integer> symbols = new HashMap<>();
    private final InstructionTable table = new InstructionTable();

    private int pc = 0;

    public Map<String, Integer> run(List<Node> program) {

        for (Node node : program) {

            if (node instanceof Label label) {
                symbols.put(label.getName(), pc);
            }

            else if (node instanceof Instruction inst) {

                InstructionPattern pattern = table.find(inst);

                Operand[] ops = inst.getOperands().toArray(new Operand[0]);

                pc += pattern.size(ops);
            }
        }

        return symbols;
    }
}