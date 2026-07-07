package org.psz80.tests;

import java.util.Arrays;
import java.util.List;
import org.psz80.assembler.Assembler;
import org.psz80.linker.LinkedProgram;
import org.psz80.linker.Linker;
import org.psz80.linker.LinkerMode;
import org.psz80.linker.ObjectModule;

// Compara:
// 1) ligador-relocador
// 2) ligador + carregador relocador
//
// Os bytes finais devem ser iguais.
public class linkerCompare {

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

        int loadAddress = 0x8000;

        LinkedProgram relocador = linker.link(
            List.of(mainObj, ioObj),
            LinkerMode.LINK_AND_RELOCATE,
            loadAddress
        );

        LinkedProgram apenasLigador = linker.link(
            List.of(mainObj, ioObj),
            LinkerMode.LINK_ONLY,
            0
        );

        byte[] bytesRelocador = relocador.getBytes();
        byte[] bytesLoader = apenasLigador.relocateAt(loadAddress);

        printBytes("Modo ligador-relocador", bytesRelocador);
        printBytes("Modo ligador + carregador relocador", bytesLoader);

        if (!Arrays.equals(bytesRelocador, bytesLoader)) {
            throw new RuntimeException(
                "ERRO: os dois modos geraram bytes finais diferentes."
            );
        }

        System.out.println("OK: os dois modos geraram o mesmo programa final.");
    }

    private static void printBytes(String title, byte[] bytes) {
        System.out.println(title + ":");

        for (byte b : bytes) {
            System.out.printf("%02X ", b & 0xFF);
        }

        System.out.println();
    }
}
