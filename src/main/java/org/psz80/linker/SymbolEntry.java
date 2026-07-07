package org.psz80.linker;

// entrada da tabela global de símbolos do ligador.
public class SymbolEntry {

    private final String name;
    private final String moduleName;
    private final int offset;
    private final int linkedAddress;

    public SymbolEntry(
        String name,
        String moduleName,
        int offset,
        int linkedAddress
    ) {
        this.name = name;
        this.moduleName = moduleName;
        this.offset = offset;
        this.linkedAddress = linkedAddress;
    }

    public String getName() {
        return name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public int getOffset() {
        return offset;
    }

    public int getLinkedAddress() {
        return linkedAddress;
    }

    @Override
    public String toString() {
        return (
            "SymbolEntry{" +
            "name='" +
            name +
            '\'' +
            ", moduleName='" +
            moduleName +
            '\'' +
            ", offset=" +
            offset +
            ", linkedAddress=" +
            linkedAddress +
            '}'
        );
    }
}
