package org.psz80.assembler;

import org.psz80.assembler.lexer.*;
import org.psz80.assembler.macro.MacroProcessor;
import org.psz80.assembler.parser.*;
import org.psz80.assembler.pass.*;
import org.psz80.assembler.model.*;

import java.util.List;
import java.util.Map;

public class Assembler {

    public byte[] assemble(String source) {

        MacroProcessor macroProcessor = new MacroProcessor(source);
        String sourceExpandido = macroProcessor.process();
  

        Lexer lexer = new Lexer(sourceExpandido);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        List<Node> program = parser.parse();

        Pass1 pass1 = new Pass1();
        Map<String, Integer> symbols = pass1.run(program);

        Pass2 pass2 = new Pass2(symbols);
        return pass2.run(program);
    }
}