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

    private boolean isHexDigit(char c) {
        return Character.isDigit(c) ||
                (c >= 'a' && c <= 'f') ||
                (c >= 'A' && c <= 'F');
    }

    private Token readNumber() {
        int start = pos;


        // achar o hex: prefixo 0x
        if (peek() == '0' && pos + 1 < input.length() &&
                (input.charAt(pos + 1) == 'x' || input.charAt(pos + 1) == 'X')) {

            advance(); // 0
            advance(); // x

            while (!isAtEnd() && isHexDigit(peek())) {
                advance();
            }

            String text = input.substring(start, pos);
            return new Token(TokenType.NUMBER, text);
        }

        // sufixo h (1234h)
        while (!isAtEnd() && isHexDigit(peek())) {
            advance();
        }

        if (!isAtEnd() && (peek() == 'h' || peek() == 'H')) {
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

            // jolene: indexados
            if (c == '+') {
                tokens.add(new Token(TokenType.PLUS, "+"));
                advance();
                continue;
            }
            // indexados aaaa
            if (c == '-') {
                tokens.add(new Token(TokenType.MINUS, "-"));
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

            // colon, for labels
            if (c == ':') {
                tokens.add(new Token(TokenType.COLON, ":"));
                advance();
                continue;
            }

            throw new RuntimeException("Unexpected character: " + c);
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }
}