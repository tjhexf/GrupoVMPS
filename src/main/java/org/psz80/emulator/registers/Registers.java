public class Register{
    private int B,C,D,E,H,L; // REGISTRADORES 8 BITS
    private int A; // ACUMULADOR
    private int F; // FLAG, Ulisses: Ainda não entendi esse

    //Ulisses: Aqui os de 16 Bits (Especiais)
    private int PC; // progam counter
    private int SP; // stack pointer
    private int IX,IY; // reg de indice
}


//Ulisses: Metodo get dos reg de 8 bits

public int getB(){
    return B;
}

public int getC(){
    return C;
}

public int getD(){
    return D;
}

public int getE(){
    return E;
}

public int getH(){
    return H;
}

public int getL(){
    return L;
}

//Ulisses: Metodo set dos reg de 8 bits

public void setB(int value){
    this.B = value & 0xFF;
}

public void setC(int value){
    this.C = value & 0xFF;
}

public void setD(int value){
    this.D = value & 0xFF;
}

public void setE(int value){
    this.E = value & 0xFF;
}

public void setH(int value){
    this.H = value & 0xFF;
}

public void setL(int value){
    this.L = value & 0xFF;
}

//Ulisses: Set dos de 16 bits (pares)

public void setBC(int value){
    this.B = (value >> 8) & 0xFF;
    this.C = value & 0xFF;
}

public void setDE(int value){
    this.D = (value >> 8) & 0xFF;
    this.E = value & 0xFF;
}

public void setHL(int value){
    this.H = (value >> 8) & 0xFF;
    this.L = value & 0xFF;
}

public void setAF(int value){ // Ulisses: to em duvidas se esse é assim mas ta ai
    this.A = (value >> 8) & 0xFF;
    this.F = value & 0xFF;
}


//Ulisses: Get dos de 16 bits (pares)

public int getBC(){
    return (B << 8) | (C & 0xFF);
}

public int getDE(){
    return (D << 8) | (E & 0xFF);
}

public int getHL(){
    return (H << 8) | (L & 0xFF);
}

public int getAF(){
    return (A << 8) | (F & 0xFF);
}

//Ulisses: Registradores especiais

public int getPC() {
    return PC;
}

public void setPC(int value) {
    this.PC = value & 0xFFFF;
} // Ulisses: Faz sentido isso ou o ideal seria definir o quanto incrementa o pc já aqui?

public int getSP() {
    return SP;
}

public void setSP(int value) {
    this.SP = value & 0xFFFF;
}

public int getIX() {
    return IX;
}
public void setIX(int value) {
    this.IX = value & 0xFFFF;
}

public int getIY() {
    return IY;
}

public void setIY(int value) {
    this.IY = value & 0xFFFF;
}
