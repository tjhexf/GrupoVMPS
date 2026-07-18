package org.psz80.linker;

public enum RelocationType {
    // fer: aqui é por JP nn, CALL nn, LD A,(nn)
    ABSOLUTE_16,

    // : JR label
    PC_RELATIVE_8,
}
