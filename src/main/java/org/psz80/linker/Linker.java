package org.psz80.linker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Ligador de duas passagens.
public class Linker {

    public LinkedProgram link(
        List<ObjectModule> modules,
        LinkerMode mode,
        int loadAddress
    ) {
        if (modules == null || modules.isEmpty()) {
            throw new RuntimeException("No object modules provided to linker.");
        }

        Map<ObjectModule, Integer> moduleBases = new HashMap<>();
        Map<String, SymbolEntry> globalSymbols = new HashMap<>();

        int totalSize = firstPass(modules, moduleBases, globalSymbols);

        if (totalSize > 0x10000) {
            throw new RuntimeException(
                "Linked program exceeds Z80 memory size: " + totalSize
            );
        }

        if (mode == LinkerMode.LINK_AND_RELOCATE) {
            int endAddress = (loadAddress & 0xFFFF) + totalSize;

            if (endAddress > 0x10000) {
                throw new RuntimeException(
                    String.format(
                        "Program does not fit in memory at load address 0x%04X.",
                        loadAddress & 0xFFFF
                    )
                );
            }
        }

        return secondPass(
            modules,
            mode,
            loadAddress,
            totalSize,
            moduleBases,
            globalSymbols
        );
    }

    // Passagem 1:
    // - calcula base relativa de cada módulo;
    // - monta tabela global de símbolos;
    // - detecta símbolos duplicados.
    private int firstPass(
        List<ObjectModule> modules,
        Map<ObjectModule, Integer> moduleBases,
        Map<String, SymbolEntry> globalSymbols
    ) {
        int currentBase = 0;

        for (ObjectModule module : modules) {
            moduleBases.put(module, currentBase);

            for (Map.Entry<String, Integer> symbol : module
                .getSymbols()
                .entrySet()) {
                String symbolName = symbol.getKey();
                int localOffset = symbol.getValue();
                int linkedAddress = currentBase + localOffset;

                if (globalSymbols.containsKey(symbolName)) {
                    SymbolEntry previous = globalSymbols.get(symbolName);

                    throw new RuntimeException(
                        "Duplicate symbol '" +
                            symbolName +
                            "' defined in modules '" +
                            previous.getModuleName() +
                            "' and '" +
                            module.getModuleName() +
                            "'."
                    );
                }

                globalSymbols.put(
                    symbolName,
                    new SymbolEntry(
                        symbolName,
                        module.getModuleName(),
                        localOffset,
                        linkedAddress
                    )
                );
            }

            currentBase += module.size();
        }

        return currentBase;
    }

    // Passagem 2:
    // - concatena os códigos;
    // - resolve cada relocação;
    // - no modo LINK_AND_RELOCATE, já soma o loadAddress;
    // - no modo LINK_ONLY, deixa tabela para o carregador relocador.
    private LinkedProgram secondPass(
        List<ObjectModule> modules,
        LinkerMode mode,
        int loadAddress,
        int totalSize,
        Map<ObjectModule, Integer> moduleBases,
        Map<String, SymbolEntry> globalSymbols
    ) {
        byte[] output = new byte[totalSize];
        List<RelocationEntry> loaderRelocations = new ArrayList<>();

        for (ObjectModule module : modules) {
            int moduleBase = moduleBases.get(module);
            byte[] code = module.getCode();

            System.arraycopy(code, 0, output, moduleBase, code.length);

            for (RelocationEntry relocation : module.getRelocations()) {
                int globalOffset = moduleBase + relocation.getOffset();

                SymbolEntry targetSymbol = globalSymbols.get(
                    relocation.getSymbolName()
                );

                if (targetSymbol == null) {
                    throw new RuntimeException(
                        "Undefined external symbol '" +
                            relocation.getSymbolName() +
                            "' referenced in module '" +
                            module.getModuleName() +
                            "'."
                    );
                }

                switch (relocation.getType()) {
                    case ABSOLUTE_16 -> {
                        int targetRelativeAddress =
                            targetSymbol.getLinkedAddress();

                        if (mode == LinkerMode.LINK_AND_RELOCATE) {
                            int absoluteAddress =
                                ((loadAddress & 0xFFFF) +
                                    targetRelativeAddress) &
                                0xFFFF;

                            writeWord(output, globalOffset, absoluteAddress);
                        } else {
                            writeWord(
                                output,
                                globalOffset,
                                targetRelativeAddress
                            );

                            // Em LINK_ONLY, o carregador relocador somará o endereço de carga.
                            loaderRelocations.add(
                                new RelocationEntry(
                                    globalOffset,
                                    relocation.getSymbolName(),
                                    RelocationType.ABSOLUTE_16
                                )
                            );
                        }
                    }
                    case PC_RELATIVE_8 -> {
                        int targetRelativeAddress =
                            targetSymbol.getLinkedAddress();

                        // O offset do RelocationEntry aponta para o byte de deslocamento do JR.
                        // O PC após ler esse byte será globalOffset + 1.
                        int sourceNextRelativeAddress = globalOffset + 1;

                        int displacement =
                            targetRelativeAddress - sourceNextRelativeAddress;

                        if (displacement < -128 || displacement > 127) {
                            throw new RuntimeException(
                                "JR target out of range for symbol '" +
                                    relocation.getSymbolName() +
                                    "': " +
                                    displacement
                            );
                        }

                        output[globalOffset] = (byte) (displacement & 0xFF);

                        // PC_RELATIVE_8 não precisa ficar para o loader,
                        // pois base de carga cancela nos dois lados.
                    }
                }
            }
        }

        return new LinkedProgram(output, loaderRelocations);
    }

    private static void writeWord(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
}
