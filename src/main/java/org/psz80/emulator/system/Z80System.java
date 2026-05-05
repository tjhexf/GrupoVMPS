package org.psz80.emulator.system;

import org.psz80.emulator.cpu.Registers;
import org.psz80.emulator.cpu.Z80;
import org.psz80.emulator.memory.Memory;

public class Z80System {

    private final Memory memory;
    private final Registers registers;
    private final Z80 cpu;

    public Z80System() {
        this.memory = new Memory();
        this.registers = new Registers();
        this.cpu = new Z80(memory, registers);

        reset();
    }

    public void reset() {
        registers.setPC(0x0000);
        registers.setSP(0xFFFE);
        registers.setIX(0x0000);
        registers.setIY(0x0000);

        registers.setAF(0x0000);
        registers.setBC(0x0000);
        registers.setDE(0x0000);
        registers.setHL(0x0000);

        cpu.resetHalt();
    }

    public void loadProgram(int startAddress, int[] programBytes) {
        int address = startAddress & 0xFFFF;

        for (int i = 0; i < programBytes.length; i++) {
            memory.escreverByte(address + i, programBytes[i]);
        }

        registers.setPC(address);
    }

    public void step() {
        cpu.step();
    }

    public void runUntilHalt(int maxSteps) {
        int steps = 0;

        while (!cpu.isHalted() && steps < maxSteps) {
            cpu.step();
            steps++;
        }
    }

    public boolean isHalted() {
        return cpu.isHalted();
    }

    public Memory getMemory() {
        return memory;
    }

    public Registers getRegisters() {
        return registers;
    }

    public Z80 getCpu() {
        return cpu;
    }
}