package org.psz80;
import org.psz80.assembler.lexer.Lexer;
import org.psz80.assembler.lexer.Token;
import org.psz80.assembler.model.Node;
import org.psz80.assembler.parser.Parser;
import org.psz80.assembler.pass.Pass1;
import org.psz80.assembler.pass.Pass2;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        IO.println(String.format("Hello and welcome!"));

        for (int i = 1; i <= 5; i++) {
            //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
            IO.println("i = " + i);
        }


        String source = """
                       PUSH IY
                       POP IY
            """;

        // --- LEXER ---
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        System.out.println("TOKENS:");
        for (Token t : tokens) {
            System.out.println(t);
        }

        // --- PARSER ---
        Parser parser = new Parser(tokens);
        List<Node> nodes = parser.parse();

        System.out.println("\nNODES:");
        for (Node node : nodes) {
            System.out.println(node);
        }


        Pass1 pass1 = new Pass1();
        var symbols = pass1.run(nodes);

        System.out.println("\nSYMBOLS:");
        for (var entry : symbols.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());

        }
        Pass2 pass2 = new Pass2(symbols);
        byte[] code = pass2.run(nodes);

        for (byte b : code) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }
}
