package org.psz80.assembler.lexer;

public class Token {
    public TokenType type;
    public String text;

    public Token(TokenType type, String text) {
        this.type = type;
        this.text = text;
    }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}