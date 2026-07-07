package org.psz80.linker;

// tipos de relocação usados pelo ligador
public enum RelocationType {
    // Endereço absoluto de 16 bits: usado por JP nn, CALL nn, LD A,(nn)
    ABSOLUTE_16,

    // Deslocamento relativo de 8 bits: usado por JR label
    PC_RELATIVE_8,
}
