package org.psz80.assembler.macro;

import java.util.ArrayList;
import java.util.List;

public class MacroProcessor {

    private final String input;
    private int pos = 0;

    //tabelas MNT e MDT
    private final List<Macro> mnt = new ArrayList<>();
    private final List<String> mdt = new ArrayList<>();

    //para macros aninhadas
    private boolean definitionMode = false;
    private int definitionLevel = 0;

    // Argument List Array
    private final List<String> formalParameters = new ArrayList<>();
    private final List<String> ala = new ArrayList<>();


    private boolean expansionMode = false;
    private int mdtIndex = 0;

    
    private final List<Integer> mdtIndexStack = new ArrayList<>(); //pilha para chamadas aninhadas
    private final List<List<String>> alaStack = new ArrayList<>(); 

    public MacroProcessor(String input) { //construtor
        this.input = input;
    }

    //auxiliares
    private boolean isAtEnd() {
        return pos >= input.length();
    }

    private char advance() {
        return input.charAt(pos++);
    }

    
    public String process() { //ver tokenize do lexer
        StringBuilder output = new StringBuilder();

        while (!isAtEnd() || expansionMode) {
            String line = readNextLine();

            if (line == null) break;

            processLine(line, output);
        }

        return output.toString();
    }


    private String readNextLine() {
        if (expansionMode) {

            if (mdtIndex < mdt.size()) {
                String mdtLine = mdt.get(mdtIndex++);
                
                if (mdtLine.trim().equals("MCEND")) {

                    if (!mdtIndexStack.isEmpty()) {
                        int topo = mdtIndexStack.size() - 1;
                        mdtIndex = mdtIndexStack.remove(topo);
                        ala.clear();
                        ala.addAll(alaStack.remove(topo));

                        return readNextLine(); 

                    } else {
                        expansionMode = false;

                        return readNextLine();
                    }

                }
                return substituteAlaParameters(mdtLine);

            } else {
                expansionMode = false;
            }
        }

        if (isAtEnd()) return null;

        StringBuilder lineBuilder = new StringBuilder();
        while (!isAtEnd()) {
            char c = advance();
            lineBuilder.append(c);
            if (c == '\n') {
                break;
            }
        }

        return lineBuilder.toString();
    }



    private void processLine(String line, StringBuilder output){

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            if (!definitionMode) {
                output.append(line);
            }
            return;
        }

        String[] parts = trimmed.split("\\s+", 2);
        String opcode = parts[0];

        if (definitionMode) {

            if (opcode.equals("MCDEFN")) {
                definitionLevel++; 

            } else if (opcode.equals("MCEND")) {
                definitionLevel--; 

                if (definitionLevel == 0) {
                    definitionMode = false; 
                    mdt.add("MCEND");
                    return;
                }

            }

            String preparedLine = prepareMdtLine(line);
            mdt.add(preparedLine);
            return;
        }

      
        if (opcode.equals("MCDEFN")) {   //modo normal
            definitionMode = true;
            definitionLevel = 1;
            formalParameters.clear();

            String prototypeLine = readNextLine();

            if (prototypeLine != null) {
                mdt.add(prototypeLine);
                String[] protoParts = prototypeLine.trim().split("\\s+", 2);
                String macroName = protoParts[0];

                if (protoParts.length > 1) {
                    for (String p : protoParts[1].split(",")) {
                        formalParameters.add(p.trim());
                    }
                }
                mnt.add(new Macro(macroName, mdt.size()));
            }

            return;
        }

        Macro foundMacro = findMacro(opcode); //checa se é outra macro
        if (foundMacro != null) {
            if (expansionMode) {
               
                mdtIndexStack.add(mdtIndex);
                alaStack.add(new ArrayList<>(ala));
            }

            expansionMode = true;
            mdtIndex = foundMacro.getMdtStartIndex();
            ala.clear();

            if (parts.length > 1) {
                for (String p : parts[1].split(",")) {
                    ala.add(p.trim());
                }
            }
            return; // linha da chamada não vai para o código final
        }

        output.append(line);
    }


    private Macro findMacro(String name) {
        for (int i = mnt.size() - 1; i >= 0; i--) {
            if (mnt.get(i).getName().equals(name)) {
                return mnt.get(i);
            }
        }
        return null;
    }


    private String prepareMdtLine(String line) {
        String result = line;

        for (int i = 0; i < formalParameters.size(); i++) {
            result = result.replace(formalParameters.get(i), "#(" + definitionLevel + "," + i + ")");
        }

        return result;
    }

    
    private String substituteAlaParameters(String line) {
        String result = line;

        for (int i = 0; i < ala.size(); i++) {
            result = result.replace("#(" + definitionLevel + "," + i + ")", ala.get(i));
            result = result.replace("#(1," + i + ")", ala.get(i)); 
        }

        return result;
    }
}