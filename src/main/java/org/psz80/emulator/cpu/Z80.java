package org.psz80.emulator.cpu;

import org.psz80.emulator.memory.Memory;
import org.psz80.emulator.cpu.Registers;

public class Z80 {

    private Memory memoria;
    private Registers registradores;

    public Z80(Memory memoria, Registers registradores) {
        this.memoria = memoria;
        this.registradores = registradores;
    }

    public void step(){

        int pcAtual = registradores.getPC();
        int opcode = memoria.lerByte(pcAtual);
        registradores.setPC(pcAtual + 1);
        executarInstrucao(opcode);
    }

    //Ulisses: dps implementar a logica das instruções e adicionar o restante (estao no InstructionTable)
    public void executarInstrucao(int opcode){
        switch (opcode) {
            case 0x00: //NOP
                break;
            case 0x76: //HALT
                //Ulisses: parar o processador
                break;
            case 0x04: //INC
                break;
            case 0x05:
                break;
        }
    }


}
