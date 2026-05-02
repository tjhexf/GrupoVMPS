package org.psz80.assembler.encoder;

import org.psz80.assembler.model.*;

import java.util.List;

public class InstructionTable {

    private final OperandClassifier classifier = new OperandClassifier();

    private final List<InstructionPattern> patterns = List.of(

            new InstructionPattern("NOP",
                    new OperandType[]{},
                    (ops, ctx) -> ctx.writeByte(0x00),
                    ops -> 1
            ),


            new InstructionPattern("INC",
                    new OperandType[]{OperandType.REG},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0x04 | (r << 3));
                    },
                    ops -> 1
            ),

            new InstructionPattern("DEC",
                    new OperandType[]{OperandType.REG},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0x05 | (r << 3));
                    },
                    ops -> 1
            ),

            new InstructionPattern("ADD",
                    new OperandType[]{OperandType.REG, OperandType.REG},
                    (ops, ctx) -> {
                        RegisterOperand a = (RegisterOperand) ops[0];
                        if (!a.getName().equalsIgnoreCase("A")) {
                            throw new RuntimeException("Only ADD A, r supported");
                        }

                        int r = regCode(((RegisterOperand) ops[1]).getName());
                        ctx.writeByte(0x80 | r);
                    },
                    ops -> 1
            ),

            new InstructionPattern("SUB",
                    new OperandType[]{OperandType.REG},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0x90 | r);
                    },
                    ops -> 1
            ),

            new InstructionPattern("LD",
                    new OperandType[]{OperandType.REG, OperandType.REG},
                    (ops, ctx) -> {
                        int r1 = regCode(((RegisterOperand) ops[0]).getName());
                        int r2 = regCode(((RegisterOperand) ops[1]).getName());
                        ctx.writeByte(0x40 | (r1 << 3) | r2);
                    },
                    ops -> 1
            ),

            new InstructionPattern("LD",
                    new OperandType[]{OperandType.REG, OperandType.IMM},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        int val = ((ImmediateOperand) ops[1]).getValue();

                        ctx.writeByte(0x06 | (r << 3));
                        ctx.writeByte(val);
                    },
                    ops -> 2
            ),

            new InstructionPattern("LD",
                    new OperandType[]{OperandType.REG, OperandType.MEM_HL},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0x46 | (r << 3));
                    },
                    ops -> 1
            ),

            new InstructionPattern("LD",
                    new OperandType[]{OperandType.REG, OperandType.MEM_IX},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());

                        MemoryOperand mem = (MemoryOperand) ops[1];
                        IndexedOperand idx = (IndexedOperand) mem.getAddress();

                        ctx.writeByte(0xDD);
                        ctx.writeByte(0x46 | (r << 3));
                        ctx.writeByte(idx.getOffset());
                    },
                    ops -> 3
            ),

            new InstructionPattern("LD",
                    new OperandType[]{OperandType.REG, OperandType.MEM_IY},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());

                        MemoryOperand mem = (MemoryOperand) ops[1];
                        IndexedOperand idx = (IndexedOperand) mem.getAddress();

                        ctx.writeByte(0xFD);
                        ctx.writeByte(0x46 | (r << 3));
                        ctx.writeByte(idx.getOffset());
                    },
                    ops -> 3
            ),

            new InstructionPattern("LD",
                    new OperandType[]{OperandType.REG, OperandType.MEM_ADDR},
                    (ops, ctx) -> {
                        RegisterOperand r = (RegisterOperand) ops[0];
                        if (!r.getName().equalsIgnoreCase("A")) {
                            throw new RuntimeException("Only LD A, (nn) supported");
                        }

                        MemoryOperand mem = (MemoryOperand) ops[1];
                        Operand inner = mem.getAddress();

                        int addr;

                        if (inner instanceof ImmediateOperand imm) {
                            addr = imm.getValue();
                        } else if (inner instanceof IdentifierOperand id) {
                            Integer v = ctx.symbols.get(id.getName());
                            if (v == null) throw new RuntimeException("Unknown label: " + id.getName());
                            addr = v;
                        } else {
                            throw new RuntimeException("Invalid memory address");
                        }

                        ctx.writeByte(0x3A);
                        ctx.writeWord(addr);
                    },
                    ops -> 3
            ),

            new InstructionPattern("JP",
                    new OperandType[]{OperandType.ADDR},
                    (ops, ctx) -> {
                        int addr;

                        if (ops[0] instanceof ImmediateOperand imm) {
                            addr = imm.getValue();
                        } else if (ops[0] instanceof IdentifierOperand id) {
                            Integer v = ctx.symbols.get(id.getName());
                            if (v == null) throw new RuntimeException("Unknown label: " + id.getName());
                            addr = v;
                        } else {
                            throw new RuntimeException("Invalid JP operand");
                        }

                        ctx.writeByte(0xC3);
                        ctx.writeWord(addr);
                    },
                    ops -> 3
            ),

            new InstructionPattern("AND",
                    new OperandType[]{OperandType.REG},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0xA0 | r);
                    },
                    ops -> 1
            ),

            new InstructionPattern("XOR",
                    new OperandType[]{OperandType.REG},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0xA8 | r);
                    },
                    ops -> 1
            ),

            new InstructionPattern("OR",
                    new OperandType[]{OperandType.REG},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0xB0 | r);
                    },
                    ops -> 1
            ),

            new InstructionPattern("CP",
                    new OperandType[]{OperandType.REG},
                    (ops, ctx) -> {
                        int r = regCode(((RegisterOperand) ops[0]).getName());
                        ctx.writeByte(0xB8 | r);
                    },
                    ops -> 1
            )


    );


    public InstructionPattern find(Instruction inst) {

        for (InstructionPattern p : patterns) {

            if (!p.mnemonic.equalsIgnoreCase(inst.getMnemonic())) continue;
            if (p.types.length != inst.getOperands().size()) continue;

            boolean match = true;

            for (int i = 0; i < p.types.length; i++) {
                OperandType actual = classifier.classify(inst.getOperands().get(i));
                if (actual != p.types[i]) {
                    match = false;
                    break;
                }
            }

            if (match) return p;
        }

        throw new RuntimeException("No matching pattern for: " + inst);
    }

    private static int regCode(String name) {
        return switch (name.toUpperCase()) {
            case "B" -> 0;
            case "C" -> 1;
            case "D" -> 2;
            case "E" -> 3;
            case "H" -> 4;
            case "L" -> 5;
            case "A" -> 7;
            default -> throw new RuntimeException("Unknown register: " + name);
        };
    }
}