package org.psz80.assembler;

import java.util.List;
import java.util.Map;
import org.psz80.assembler.lexer.*;
import org.psz80.assembler.macro.MacroProcessor;
import org.psz80.assembler.model.*;
import org.psz80.assembler.parser.*;
import org.psz80.assembler.pass.*;
import org.psz80.linker.ObjectModule;

// adiciona assembleObject(), de resto: mantém assemble() antigo.
public class Assembler {

    public List<Node> parse(String source) {
        MacroProcessor macroProcessor = new MacroProcessor(source);
        String sourceExpandido = macroProcessor.process();

        Lexer lexer = new Lexer(sourceExpandido);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    public byte[] assemble(String source) {
        List<Node> program = parse(source);

        Pass1 pass1 = new Pass1();
        Map<String, Integer> symbols = pass1.run(program);

        Pass2 pass2 = new Pass2(symbols);
        return pass2.run(program);
    }

    // módulo objeto relocável para o ligador.
    public ObjectModule assembleObject(String moduleName, String source) {
        List<Node> program = parse(source);

        Pass1 pass1 = new Pass1();
        Map<String, Integer> symbols = pass1.run(program);

        Pass2 pass2 = new Pass2(symbols, true);
        byte[] code = pass2.run(program);

        return new ObjectModule(
            moduleName,
            code,
            symbols,
            pass2.getRelocations()
        );
    }
}
