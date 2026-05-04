package org.psz80.emulator.memory;

public class Memory {

    private int[] memoria;
    //private static Memory instance;
    //Ulisses: acho que é so ísso a memoria, nao sei se vai ter mais alguma coisa
    //Ulisses: talvez no futuro surja mais necessidades. Mas por hora da para ler e escrever :)
    public Memory(){
        this.memoria = new int[65536];
    }

    public int lerByte(int address) {
        return this.memoria[address & 0xFFFF];
    }

    public void escreverByte(int address, int value){
        this.memoria[address & 0xFFFF] = value & 0xFF;
    }


}
