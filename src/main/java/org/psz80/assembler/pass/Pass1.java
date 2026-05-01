package org.psz80.assembler.pass;

import org.psz80.assembler.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pass1 {


    private final Map<String, Integer> symbols = new HashMap<>();
    private int pc = 0;

    private int estimateSize(Instruction inst) {

        String m = inst.getMnemonic().toUpperCase();

        switch (m) {

            case "NOP":
                return 1;

            case "INC":
            case "DEC":
            case "SUB":
                return 1;

            case "ADD":
                return 1; // ADD A, r

            case "LD":
                return estimateLD(inst);

            case "JP":
                return 3;

            default:
                throw new RuntimeException("Unknown instruction: " + m);
        }
    }

    private int estimateLD(Instruction inst) {
        var ops = inst.getOperands();

        if (ops.size() != 2) {
            throw new RuntimeException("LD requires 2 operands");
        }

        Operand a = ops.get(0);
        Operand b = ops.get(1);

        // LD r, n
        if (a instanceof RegisterOperand && b instanceof ImmediateOperand) {
            return 2;
        }

        // LD r, r'
        if (a instanceof RegisterOperand && b instanceof RegisterOperand) {
            return 1;
        }

        // LD r, (HL)
        if (a instanceof RegisterOperand && b instanceof MemoryOperand) {
            return 1;
        }

        // LD (HL), r
        if (a instanceof MemoryOperand && b instanceof RegisterOperand) {
            return 1;
        }

        // fallback
        return 2;
    }

    public Map<String, Integer> run(List<Node> program) {
        for (Node node : program) {

            if (node instanceof Label label) {
                symbols.put(label.getName(), pc);
            }

            else if (node instanceof Instruction inst) {
                pc += estimateSize(inst);
            }
        }

        return symbols;
    }
}