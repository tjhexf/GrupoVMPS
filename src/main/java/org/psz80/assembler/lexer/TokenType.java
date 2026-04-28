package org.psz80.assembler.lexer;

public enum TokenType {
    IDENT,      // LD, A, HL, loop
    NUMBER,     // 123, 42
    COMMA,      // ,
    LPAREN,     // (
    RPAREN,     // )
    NEWLINE,    // \n
    COLON,      // :
    PLUS,
    MINUS,
    EOF
}