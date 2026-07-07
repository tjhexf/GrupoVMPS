package org.psz80.tests;

import java.util.List;
import org.psz80.assembler.Assembler;
import org.psz80.emulator.system.Z80System;
import org.psz80.linker.LinkedProgram;
import org.psz80.linker.Linker;
import org.psz80.linker.LinkerMode;
import org.psz80.linker.ObjectModule;

// Testa o modo "apenas ligador".
// Nesse modo, o ligador deixa a relocação final para o carregador relocador.
public class linkerOnly {

    public static void main(String[] args) {
        String mainSource = """
            START:
                CALL PRINT
                HALT
            """;

        String ioSource = """
            PRINT:
                LD A, 65
                RET
            """;

        Assembler assembler = new Assembler();

        ObjectModule mainObj = assembler.assembleObject("main", mainSource);
        ObjectModule ioObj = assembler.assembleObject("io", ioSource);

        Linker linker = new Linker();

        LinkedProgram linkedOnly = linker.link(
            List.of(mainObj, ioObj),
            LinkerMode.LINK_ONLY,
            0
        );

        printBytes(
            "Bytes apenas ligados, antes da relocação do loader",
            linkedOnly.getBytes()
        );

        int loadAddress = 0x8000;

        byte[] relocatedBytes = linkedOnly.relocateAt(loadAddress);

        printBytes(
            "Bytes após carregador relocador aplicar base 0x8000",
            relocatedBytes
        );

        Z80System system = new Z80System();

        system.loadProgram(
            loadAddress,
            LinkedProgram.toIntArray(relocatedBytes)
        );

        system.runUntilHalt(1000);

        int a = (system.getRegisters().getAF() >> 8) & 0xFF;
        int pc = system.getRegisters().getPC();

        System.out.printf("Registrador A = %d%n", a);
        System.out.printf("PC final = 0x%04X%n", pc);

        if (a != 65) {
            throw new RuntimeException(
                "ERRO: esperado A = 65, mas veio A = " + a
            );
        }

        if (!system.isHalted()) {
            throw new RuntimeException("ERRO: CPU não entrou em HALT.");
        }

        System.out.println("OK: TestLinkerOnly passou.");
    }

    private static void printBytes(String title, byte[] bytes) {
        System.out.println(title + ":");

        for (byte b : bytes) {
            System.out.printf("%02X ", b & 0xFF);
        }

        System.out.println();
    }
}
