package org.psz80.linker;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// fernanda: módulo do arquivo .asm
public class ObjectModule {

    private final String moduleName;
    private final byte[] code;
    private final Map<String, Integer> symbols;
    private final List<RelocationEntry> relocations;

    public ObjectModule(
        String moduleName,
        byte[] code,
        Map<String, Integer> symbols,
        List<RelocationEntry> relocations
    ) {
        this.moduleName = moduleName;
        this.code = code.clone();
        this.symbols = Map.copyOf(symbols);
        this.relocations = List.copyOf(relocations);
    }

    public String getModuleName() {
        return moduleName;
    }

    public byte[] getCode() {
        return code.clone();
    }

    public Map<String, Integer> getSymbols() {
        return Collections.unmodifiableMap(symbols);
    }

    public List<RelocationEntry> getRelocations() {
        return Collections.unmodifiableList(relocations);
    }

    public int size() {
        return code.length;
    }
}
