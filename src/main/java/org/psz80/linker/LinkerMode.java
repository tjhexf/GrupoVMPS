package org.psz80.linker;

// define os dois modos pedidos
public enum LinkerMode {
    // Apenas liga os módulos -- relocação absoluta final fica para o carregador relocador.
    LINK_ONLY,

    // Liga e já reloca para um endereço de carga conhecido -- programa pronto para carregador absoluto.
    LINK_AND_RELOCATE,
}
