package org.psz80.assembler.parser;

import org.psz80.assembler.lexer.Token;
import org.psz80.assembler.lexer.TokenType;
import org.psz80.assembler.model.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    private void expect(TokenType type) {
        if (!check(type)) {
            throw new RuntimeException("Esperado: " + type + " mas encontrou: " + peek());
        }
        advance();
    }

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
        String mnemonic = advance().text;

        List<Operand> operands = new ArrayList<>();

        if (!check(TokenType.NEWLINE) && !isAtEnd()) {
            operands.add(parseOperand());

            while (check(TokenType.COMMA)) {
                advance();
                operands.add(parseOperand());
            }
        }

        return new Instruction(mnemonic, operands);
    }

    private boolean isRegister(String name) {
        return switch (name.toUpperCase()) {
            case "A", "B", "C", "D", "E", "H", "L", "HL", "BC", "DE", "AF", "IX", "IY" -> true;
            default -> false;
        };
    }

    private Operand parseOperand() {

        // ( ... )
        if (check(TokenType.LPAREN)) {
            advance(); // (

            // jolene: a busca do indexado
            if (check(TokenType.IDENT)) {
                String name = peek().text.toUpperCase();

                if (name.equals("IX") || name.equals("IY")) {
                    advance();

                    int offset = 0;

                    if (check(TokenType.PLUS) || check(TokenType.MINUS)) {
                        boolean negative = check(TokenType.MINUS);
                        advance();

                        if (!check(TokenType.NUMBER)) {
                            throw new RuntimeException("número esperado após + ou -");
                        }

                        offset = Integer.parseInt(advance().text);
                        if (negative) offset = -offset;
                    }

                    expect(TokenType.RPAREN);

                    return new MemoryOperand(new IndexedOperand(name, offset));
                }
            }

            Operand inner = parseOperand();

            expect(TokenType.RPAREN);

            return new MemoryOperand(inner);
        }

        if (check(TokenType.NUMBER)) {
            int value = parseNumber(advance().text);
            return new ImmediateOperand(value);
        }

        if (check(TokenType.MINUS)) {
            advance(); // -

            if (!check(TokenType.NUMBER)) {
                throw new RuntimeException("Expected number after '-'");
            }

            int value = -parseNumber(advance().text);
            return new ImmediateOperand(value);
        }

        if (check(TokenType.IDENT)) {
            String name = advance().text;

            if (isRegister(name)) {
                return new RegisterOperand(name);
            }

            return new IdentifierOperand(name);
        }

        throw new RuntimeException("token inexperada na operação: " + peek());
    }

    private int parseNumber(String text) {
        String t = text.toLowerCase();

        // hex prefix: 0xFF
        if (t.startsWith("0x")) {
            return Integer.parseInt(t.substring(2), 16);
        }

        // hex suffix: FFh
        if (t.endsWith("h")) {
            return Integer.parseInt(t.substring(0, t.length() - 1), 16);
        }

        // decimal
        return Integer.parseInt(t);
    }

    private Node parseLine() {
        Node node;

        if (check(TokenType.IDENT) && lookAhead(TokenType.COLON)) {
            node = parseLabel();
        }
        else if (check(TokenType.IDENT)) {
            node = parseInstruction();
        }
        else {
            throw new RuntimeException("token invalida na linha: " + peek());
        }

        // consume newline after line
        if (check(TokenType.NEWLINE)) {
            advance();
        }

        return node;
    }
}