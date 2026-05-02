package org.psz80.assembler.encoder;

import org.psz80.assembler.model.*;

public class OperandClassifier {

    public OperandType classify(Operand op) {

        if (op instanceof RegisterOperand r) {
            String name = r.getName().toUpperCase();

            if (name.equals("BC") || name.equals("DE") ||
                    name.equals("HL") || name.equals("AF")) {
                return OperandType.REG_PAIR;
            }

            return OperandType.REG;
        }

        if (op instanceof ImmediateOperand) return OperandType.IMM;

        if (op instanceof IdentifierOperand id) {
            String name = id.getName().toUpperCase();

            if (name.equals("BC") || name.equals("DE") ||
                    name.equals("HL") || name.equals("AF")) {
                return OperandType.REG_PAIR;
            }

            return OperandType.ADDR;
        }

        if (op instanceof MemoryOperand mem) {

            Operand inner = mem.getAddress();

            if (inner instanceof RegisterOperand r &&
                    r.getName().equalsIgnoreCase("HL")) {
                return OperandType.MEM_HL;
            }

            if (inner instanceof IndexedOperand idx) {
                if (idx.getRegister().equalsIgnoreCase("IX")) return OperandType.MEM_IX;
                if (idx.getRegister().equalsIgnoreCase("IY")) return OperandType.MEM_IY;
            }

            if (inner instanceof ImmediateOperand || inner instanceof IdentifierOperand) {
                return OperandType.MEM_ADDR;
            }
        }



        throw new RuntimeException("Unknown operand: " + op);
    }
}