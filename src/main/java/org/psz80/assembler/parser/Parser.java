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

    public List<Node> parse() {
        List<Node> nodes = new ArrayList<>();

        while (!isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance(); // jolene: skip empty lines
                continue;
            }

            nodes.add(parseLine());
        }

        return nodes;
    }

    private boolean lookAhead(TokenType type) {
        if (pos + 1 >= tokens.size()) return false;
        return tokens.get(pos + 1).type == type;
    }

    private Label parseLabel() {
        String name = advance().text; // IDENT
        advance(); // jolene: comer o :

        return new Label(name);
    }

    private Instruction parseInstruction() {
        String mnemonic = advance().text; // IDENT

        // skip rest of line for now
        while (!check(TokenType.NEWLINE) && !isAtEnd()) {
            advance();
        }

        return new Instruction(mnemonic, List.of());
    }


    private Node parseLine() {
        if (check(TokenType.IDENT) && lookAhead(TokenType.COLON)) {
            return parseLabel();
        }

        return parseInstruction();
    }
}