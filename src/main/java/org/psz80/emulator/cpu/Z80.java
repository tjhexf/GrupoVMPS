package org.psz80.emulator.cpu;

import org.psz80.emulator.memory.Memory;

public class Z80 {

    private final Memory memoria;
    private final Registers registradores;

    private boolean halted = false;

    public Z80(Memory memoria, Registers registradores) {
        this.memoria = memoria;
        this.registradores = registradores;
    }

    public void step() {
        if (halted) {
            return;
        }

        int opcode = fetchByte();
        executarInstrucao(opcode);
    }

    public void executarInstrucao(int opcode) {
        opcode = opcode & 0xFF;

        switch (opcode) {

            case 0x00:
                // NOP
                break;

            case 0x76:
                // HALT
                halted = true;
                break;

            case 0xC3:
                // JP nn
                registradores.setPC(fetchWord());
                break;

            case 0x18:
                // JR e
                int offset = fetchByte();

                if (offset >= 0x80) {
                    offset -= 0x100;
                }

                registradores.setPC(registradores.getPC() + offset);
                break;

            case 0x3A:
                // LD A, (nn)
                int endereco = fetchWord();
                setA(memoria.lerByte(endereco));
                break;

            case 0xCD:
                // CALL nn
                int enderecoCall = fetchWord();
                pushWord(registradores.getPC());
                registradores.setPC(enderecoCall);
                break;

            case 0xC9:
                // RET
                registradores.setPC(popWord());
                break;

            case 0xDD:
                // Prefixo IX
                executarInstrucaoIndexada(true);
                break;

            case 0xFD:
                // Prefixo IY
                executarInstrucaoIndexada(false);
                break;

            default:
                executarInstrucaoPorPadrao(opcode);
                break;
        }
    }

    private void executarInstrucaoPorPadrao(int opcode) {
        opcode = opcode & 0xFF;

        // INC r -> 00 rrr 100
        if ((opcode & 0b11000111) == 0b00000100) {
            int reg = (opcode >> 3) & 0b111;
            incReg(reg);
            return;
        }

        // DEC r -> 00 rrr 101
        if ((opcode & 0b11000111) == 0b00000101) {
            int reg = (opcode >> 3) & 0b111;
            decReg(reg);
            return;
        }

        // LD r, n -> 00 rrr 110
        if ((opcode & 0b11000111) == 0b00000110) {
            int reg = (opcode >> 3) & 0b111;
            int valor = fetchByte();

            escreverRegOuMemoriaHL(reg, valor);
            return;
        }

        // LD r, r' -> 01 rrr rrr
        // Obs: 0x76 seria LD (HL), (HL), mas no Z80 é tratado como HALT.
        if ((opcode & 0b11000000) == 0b01000000 && opcode != 0x76) {
            int destino = (opcode >> 3) & 0b111;
            int origem = opcode & 0b111;

            int valor = lerRegOuMemoriaHL(origem);
            escreverRegOuMemoriaHL(destino, valor);
            return;
        }

        // ADD A, r -> 10000rrr
        if ((opcode & 0b11111000) == 0b10000000) {
            int reg = opcode & 0b111;
            int valor = lerRegOuMemoriaHL(reg);

            addA(valor);
            return;
        }

        // SUB r -> 10010rrr
        if ((opcode & 0b11111000) == 0b10010000) {
            int reg = opcode & 0b111;
            int valor = lerRegOuMemoriaHL(reg);

            subA(valor);
            return;
        }

        // AND r -> 10100rrr
        if ((opcode & 0b11111000) == 0b10100000) {
            int reg = opcode & 0b111;
            int valor = lerRegOuMemoriaHL(reg);

            andA(valor);
            return;
        }

        // XOR r -> 10101rrr
        if ((opcode & 0b11111000) == 0b10101000) {
            int reg = opcode & 0b111;
            int valor = lerRegOuMemoriaHL(reg);

            xorA(valor);
            return;
        }

        // OR r -> 10110rrr
        if ((opcode & 0b11111000) == 0b10110000) {
            int reg = opcode & 0b111;
            int valor = lerRegOuMemoriaHL(reg);

            orA(valor);
            return;
        }

        // CP r -> 10111rrr
        if ((opcode & 0b11111000) == 0b10111000) {
            int reg = opcode & 0b111;
            int valor = lerRegOuMemoriaHL(reg);

            cpA(valor);
            return;
        }

        // POP qq
        if ((opcode & 0b11001111) == 0b11000001) {
            int par = (opcode >> 4) & 0b11;
            int valor = popWord();

            setParRegistrador(par, valor);
            return;
        }

        // PUSH qq
        if ((opcode & 0b11001111) == 0b11000101) {
            int par = (opcode >> 4) & 0b11;
            int valor = getParRegistrador(par);

            pushWord(valor);
            return;
        }

        throw new RuntimeException(String.format("Opcode não implementado: 0x%02X", opcode));
    }

    private void executarInstrucaoIndexada(boolean usarIX) {
        int opcode = fetchByte();

        switch (opcode) {

            case 0xE5:
                // PUSH IX/IY
                if (usarIX) {
                    pushWord(registradores.getIX());
                } else {
                    pushWord(registradores.getIY());
                }
                break;

            case 0xE1:
                // POP IX/IY
                int valor = popWord();

                if (usarIX) {
                    registradores.setIX(valor);
                } else {
                    registradores.setIY(valor);
                }
                break;

            default:
                executarInstrucaoIndexadaPorPadrao(opcode, usarIX);
                break;
        }
    }

    private void executarInstrucaoIndexadaPorPadrao(int opcode, boolean usarIX) {
        opcode = opcode & 0xFF;

        // LD r, (IX+d) ou LD r, (IY+d)
        if ((opcode & 0b11000111) == 0b01000110) {
            int reg = (opcode >> 3) & 0b111;
            int deslocamento = signedByte(fetchByte());

            int base = usarIX ? registradores.getIX() : registradores.getIY();
            int endereco = (base + deslocamento) & 0xFFFF;

            int valor = memoria.lerByte(endereco);

            setRegByCode(reg, valor);
            return;
        }

        // LD (IX+d), r ou LD (IY+d), r
        if ((opcode & 0b11111000) == 0b01110000) {
            int reg = opcode & 0b111;
            int deslocamento = signedByte(fetchByte());

            int base = usarIX ? registradores.getIX() : registradores.getIY();
            int endereco = (base + deslocamento) & 0xFFFF;

            int valor = getRegByCode(reg);

            memoria.escreverByte(endereco, valor);
            return;
        }

        throw new RuntimeException(String.format(
                "Opcode indexado não implementado: %s 0x%02X",
                usarIX ? "DD" : "FD",
                opcode
        ));
    }

    private int fetchByte() {
        int pc = registradores.getPC();
        int valor = memoria.lerByte(pc);

        registradores.setPC(pc + 1);

        return valor & 0xFF;
    }

    private int fetchWord() {
        int low = fetchByte();
        int high = fetchByte();

        return ((high << 8) | low) & 0xFFFF;
    }

    private int signedByte(int valor) {
        valor = valor & 0xFF;

        if (valor >= 0x80) {
            return valor - 0x100;
        }

        return valor;
    }

    private int lerRegOuMemoriaHL(int codigo) {
        codigo = codigo & 0b111;

        if (codigo == 6) {
            return memoria.lerByte(registradores.getHL()) & 0xFF;
        }

        return getRegByCode(codigo);
    }

    private void escreverRegOuMemoriaHL(int codigo, int valor) {
        codigo = codigo & 0b111;
        valor = valor & 0xFF;

        if (codigo == 6) {
            memoria.escreverByte(registradores.getHL(), valor);
            return;
        }

        setRegByCode(codigo, valor);
    }

    private int getRegByCode(int codigo) {
        codigo = codigo & 0b111;

        switch (codigo) {
            case 0:
                return registradores.getB();

            case 1:
                return registradores.getC();

            case 2:
                return registradores.getD();

            case 3:
                return registradores.getE();

            case 4:
                return registradores.getH();

            case 5:
                return registradores.getL();

            case 7:
                return getA();

            default:
                throw new RuntimeException("Código de registrador inválido: " + codigo);
        }
    }

    private void setRegByCode(int codigo, int valor) {
        codigo = codigo & 0b111;
        valor = valor & 0xFF;

        switch (codigo) {
            case 0:
                registradores.setB(valor);
                break;

            case 1:
                registradores.setC(valor);
                break;

            case 2:
                registradores.setD(valor);
                break;

            case 3:
                registradores.setE(valor);
                break;

            case 4:
                registradores.setH(valor);
                break;

            case 5:
                registradores.setL(valor);
                break;

            case 7:
                setA(valor);
                break;

            default:
                throw new RuntimeException("Código de registrador inválido: " + codigo);
        }
    }

    private int getA() {
        return (registradores.getAF() >> 8) & 0xFF;
    }

    private void setA(int valor) {
        valor = valor & 0xFF;

        int flags = registradores.getAF() & 0xFF;

        registradores.setAF((valor << 8) | flags);
    }

    private int getParRegistrador(int codigo) {
        codigo = codigo & 0b11;

        switch (codigo) {
            case 0:
                return registradores.getBC();

            case 1:
                return registradores.getDE();

            case 2:
                return registradores.getHL();

            case 3:
                return registradores.getAF();

            default:
                throw new RuntimeException("Par de registrador inválido: " + codigo);
        }
    }

    private void setParRegistrador(int codigo, int valor) {
        codigo = codigo & 0b11;
        valor = valor & 0xFFFF;

        switch (codigo) {
            case 0:
                registradores.setBC(valor);
                break;

            case 1:
                registradores.setDE(valor);
                break;

            case 2:
                registradores.setHL(valor);
                break;

            case 3:
                registradores.setAF(valor);
                break;

            default:
                throw new RuntimeException("Par de registrador inválido: " + codigo);
        }
    }

    private void pushWord(int valor) {
        valor = valor & 0xFFFF;

        int high = (valor >> 8) & 0xFF;
        int low = valor & 0xFF;

        int sp = registradores.getSP();

        sp = (sp - 1) & 0xFFFF;
        memoria.escreverByte(sp, high);

        sp = (sp - 1) & 0xFFFF;
        memoria.escreverByte(sp, low);

        registradores.setSP(sp);
    }

    private int popWord() {
        int sp = registradores.getSP();

        int low = memoria.lerByte(sp) & 0xFF;
        sp = (sp + 1) & 0xFFFF;

        int high = memoria.lerByte(sp) & 0xFF;
        sp = (sp + 1) & 0xFFFF;

        registradores.setSP(sp);

        return ((high << 8) | low) & 0xFFFF;
    }

    private void incReg(int codigo) {
        int valorAntigo = lerRegOuMemoriaHL(codigo);
        int resultado = (valorAntigo + 1) & 0xFF;

        escreverRegOuMemoriaHL(codigo, resultado);
        atualizarFlagsInc(valorAntigo, resultado);
    }

    private void decReg(int codigo) {
        int valorAntigo = lerRegOuMemoriaHL(codigo);
        int resultado = (valorAntigo - 1) & 0xFF;

        escreverRegOuMemoriaHL(codigo, resultado);
        atualizarFlagsDec(valorAntigo, resultado);
    }

    private void addA(int valor) {
        valor = valor & 0xFF;

        int a = getA();
        int resultado = a + valor;

        setA(resultado);
        atualizarFlagsAdd(a, valor, resultado);
    }

    private void subA(int valor) {
        valor = valor & 0xFF;

        int a = getA();
        int resultado = a - valor;

        setA(resultado);
        atualizarFlagsSub(a, valor, resultado);
    }

    private void andA(int valor) {
        valor = valor & 0xFF;

        int resultado = getA() & valor;

        setA(resultado);

        atualizarFlagsLogicas(resultado);

        registradores.setHFlag();
        registradores.clearNFlag();
        registradores.clearCFlag();
    }

    private void xorA(int valor) {
        valor = valor & 0xFF;

        int resultado = getA() ^ valor;

        setA(resultado);

        atualizarFlagsLogicas(resultado);

        registradores.clearHFlag();
        registradores.clearNFlag();
        registradores.clearCFlag();
    }

    private void orA(int valor) {
        valor = valor & 0xFF;

        int resultado = getA() | valor;

        setA(resultado);

        atualizarFlagsLogicas(resultado);

        registradores.clearHFlag();
        registradores.clearNFlag();
        registradores.clearCFlag();
    }

    private void cpA(int valor) {
        valor = valor & 0xFF;

        int a = getA();
        int resultado = a - valor;

        atualizarFlagsSub(a, valor, resultado);
    }

    private void atualizarFlagsInc(int valorAntigo, int resultado) {
        resultado = resultado & 0xFF;

        atualizarFlagS(resultado);
        atualizarFlagZ(resultado);

        if (((valorAntigo & 0x0F) + 1) > 0x0F) {
            registradores.setHFlag();
        } else {
            registradores.clearHFlag();
        }

        if (valorAntigo == 0x7F) {
            registradores.setPVFlag();
        } else {
            registradores.clearPVFlag();
        }

        registradores.clearNFlag();

        // No Z80, INC não altera a flag C.
    }

    private void atualizarFlagsDec(int valorAntigo, int resultado) {
        resultado = resultado & 0xFF;

        atualizarFlagS(resultado);
        atualizarFlagZ(resultado);

        if ((valorAntigo & 0x0F) == 0) {
            registradores.setHFlag();
        } else {
            registradores.clearHFlag();
        }

        if (valorAntigo == 0x80) {
            registradores.setPVFlag();
        } else {
            registradores.clearPVFlag();
        }

        registradores.setNFlag();

        // No Z80, DEC não altera a flag C.
    }

    private void atualizarFlagsAdd(int a, int valor, int resultado) {
        int res8 = resultado & 0xFF;

        atualizarFlagS(res8);
        atualizarFlagZ(res8);

        if (((a & 0x0F) + (valor & 0x0F)) > 0x0F) {
            registradores.setHFlag();
        } else {
            registradores.clearHFlag();
        }

        if (resultado > 0xFF) {
            registradores.setCFlag();
        } else {
            registradores.clearCFlag();
        }

        boolean overflow = (~(a ^ valor) & (a ^ res8) & 0x80) != 0;

        if (overflow) {
            registradores.setPVFlag();
        } else {
            registradores.clearPVFlag();
        }

        registradores.clearNFlag();
    }

    private void atualizarFlagsSub(int a, int valor, int resultado) {
        int res8 = resultado & 0xFF;

        atualizarFlagS(res8);
        atualizarFlagZ(res8);

        if ((a & 0x0F) < (valor & 0x0F)) {
            registradores.setHFlag();
        } else {
            registradores.clearHFlag();
        }

        if (resultado < 0) {
            registradores.setCFlag();
        } else {
            registradores.clearCFlag();
        }

        boolean overflow = ((a ^ valor) & (a ^ res8) & 0x80) != 0;

        if (overflow) {
            registradores.setPVFlag();
        } else {
            registradores.clearPVFlag();
        }

        registradores.setNFlag();
    }

    private void atualizarFlagsLogicas(int resultado) {
        resultado = resultado & 0xFF;

        atualizarFlagS(resultado);
        atualizarFlagZ(resultado);

        if (temParidadePar(resultado)) {
            registradores.setPVFlag();
        } else {
            registradores.clearPVFlag();
        }
    }

    private void atualizarFlagS(int resultado) {
        resultado = resultado & 0xFF;

        if ((resultado & 0x80) != 0) {
            registradores.setSFlag();
        } else {
            registradores.clearSFlag();
        }
    }

    private void atualizarFlagZ(int resultado) {
        resultado = resultado & 0xFF;

        if (resultado == 0) {
            registradores.setZFlag();
        } else {
            registradores.clearZFlag();
        }
    }

    private boolean temParidadePar(int valor) {
        valor = valor & 0xFF;

        int quantidadeBitsLigados = Integer.bitCount(valor);

        return quantidadeBitsLigados % 2 == 0;
    }

    public boolean isHalted() {
        return halted;
    }

    public void resetHalt() {
        halted = false;
    }
}