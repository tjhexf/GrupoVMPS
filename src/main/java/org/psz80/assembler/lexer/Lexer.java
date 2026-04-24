package org.psz80.assembler.lexer;

import java.util.ArrayList;
import java.util.List;



public class Lexer {

    private final String input;
    private final List<Token> tokens = new ArrayList<>();
    private int pos = 0;

    private boolean isAtEnd() {
        return pos >= input.length();
    }

    private Token readNumber() {
        int start = pos;

        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();
        }

        String text = input.substring(start, pos);
        return new Token(TokenType.NUMBER, text);
    }

    private Token readIdentifier() {
        int start = pos;

        while (!isAtEnd() && isIdentPart(peek())) {
            advance();
        }

        String text = input.substring(start, pos);
        return new Token(TokenType.IDENT, text);
    }

    private boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    private char peek() {
        return input.charAt(pos);
    }

    private char advance() {
        return input.charAt(pos++);
    }

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            char c = peek();

            // whitespace (ignore except newline)
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
                continue;
            }

            // newline
            if (c == '\n') {
                tokens.add(new Token(TokenType.NEWLINE, "\\n"));
                advance();
                continue;
            }

            // punctuation
            if (c == ',') {
                tokens.add(new Token(TokenType.COMMA, ","));
                advance();
                continue;
            }

            if (c == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                advance();
                continue;
            }

            if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                advance();
                continue;
            }

            // number
            if (Character.isDigit(c)) {
                tokens.add(readNumber());
                continue;
            }

            // identifier (mnemonic, register, label)
            if (isIdentStart(c)) {
                tokens.add(readIdentifier());
                continue;
            }


            throw new RuntimeException("Unexpected character: " + c);
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }
}