package org.psz80.assembler.parser;

import org.psz80.assembler.lexer.Token;
import org.psz80.assembler.lexer.TokenType;
import org.psz80.assembler.model.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }
}