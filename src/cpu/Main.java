package cpu;

public class Main {
    public static void main(String[] args) throws Exception {
        Assembler8B assembler = new Assembler8B();

        Instruction[] program = assembler.assembleFile("fib.asm8");

        CPU8B cpu = new CPU8B();
        cpu.reset();

        cpu.instMem = program;

        while(!cpu.halted){
            cpu.clock();
        }
    }
}
