package cpu;

public class CPU8B {

    //-------------------------------
    // THE ISA
    //4-bit opcodes

    static final int
            NOP = 0x0, HLT = 0x1, ADD = 0x2, SUB = 0x3, NOR = 0x4,
            AND = 0x5, XOR = 0x6, NOT  = 0x7, MOV = 0x8, LDI = 0x9,
            JMP  = 0xA, JZ  = 0xB, JC = 0xC, LOD = 0xD, STR = 0xE,
            OUT = 0xF;

    //-------------------------------
    // PHYSICAL HARDWARE STATES

    static final int MEM = 16; //16 memory addresses
    static final int MSK = 0xF; //4-bit mask

    int[] dataMem = new int[MEM]; //16 4-bit data memory
    Instruction[] instMem = new Instruction[MEM]; //16 4-bit instruction memory (16 INSTRUCTIONS TOTAL PER PROGRAM)

    int pc; //program counter

    int[] registers = new int[4];//register file, rA (0), rB (1), rC (2), rD (3)

    boolean zf; //zero flag
    boolean cf; //carry flag
    boolean halted; //halt set by HLT

    void reset(){
        pc = registers[0] = registers[1] = registers[2] = registers[3] = 0b0000;
        instMem = new Instruction[MEM];
        zf = cf = false;
        halted = false;
    }

    //-------------------------------
    // ALU

    void add(int a, int b, int destReg){
        int sum = a + b; //add two inputs

        cf = sum > MSK; //carry if sum > 4 bits

        registers[destReg] = sum & MSK; //4-bits into register destination
        //ex. 0110 & 1111 -> 0110

        zf = registers[destReg] == 0b0000; //zero flag
        //ex. 0000 & 1111 -> 0000
    }

    void sub(int a, int b, int destReg){
        int dif = a - b; //subtract two inputs

        cf = dif < 0; //carry if difference < 0

        registers[destReg] = dif & MSK; //4-bits into register destination
        //ex. 0110 & 1111 -> 0110

        zf = registers[destReg] == 0b0000; //zero flag
        //ex. 0000 & 1111 -> 0000
    }

    //~ NOT
    //& AND
    //| OR
    //^ XOR

    void nor(int a, int b, int destReg){
        cf = false;//carry false
        registers[destReg] = ~(a | b) & MSK; //register destination to a nor b, masked to 4 bits
        zf = registers[destReg] == 0b0000; //zero flag
    }

    void and(int a, int b, int destReg){
        cf = false; //carry false
        registers[destReg] = (a & b) & MSK; //register destination to a and b, masked to 4 bits
        zf = registers[destReg] == 0b0000; //zero flag
    }

    void xor(int a, int b, int destReg){
        cf = false;//carry false
        registers[destReg] = (a ^ b) & MSK; //register destination to a xor b, masked to 4 bits
        zf = registers[destReg] == 0b0000; //zero flag
    }

    void not(int a, int destReg){
        cf = false;//carry false
        registers[destReg] = (~a) & MSK; //register destination to a nor b, masked to 4 bits
        zf = registers[destReg] == 0b0000; //zero flag
    }

    //-------------------------------
    // CONTROL UNIT

    //clock
    void clock(){
        if(!halted){
            cUnit(instMem[pc]);
            pc++;
        }
    }

    //full control unit
    void cUnit(Instruction inst){
        int opcode = Integer.parseInt(inst.opcode,2); //parse string into integer value

        //parse first and second register identifiers, rA = 0, rB = 1, rC = 2, rD = 3
        int firstReg = Integer.parseInt(inst.rA,2);
        int secondReg = Integer.parseInt(inst.rB,2);

        int address = Integer.parseInt(inst.address,2); //corresponding memory address


        //COMMAND STRUCTURE opcode(registers[firstReg], registers[secondReg], secondReg)
        //register[first(second)Reg] = value of listed register in instruction
        //secondReg = address of destination register, the second register listed in the instruction
        switch (opcode){
            case NOP: //noop
                break;
            case HLT:
                halted = true;
                break;
            case ADD: //add
                add(registers[firstReg], registers[secondReg], secondReg);
                break;
            case SUB: //subtract
                sub(registers[firstReg], registers[secondReg], secondReg);
                break;
            case NOR://nor
                nor(registers[firstReg], registers[secondReg], secondReg);
                break;
            case AND: //and
                and(registers[firstReg], registers[secondReg], secondReg);
                break;
            case XOR: //xor
                xor(registers[firstReg], registers[secondReg], secondReg);
                break;
            case NOT: //not
                 not(registers[firstReg], secondReg);
                break;
            case MOV: //move
                registers[secondReg] = registers[firstReg]; //duplicates first reg to second reg
                break;
            case LDI: //load immediate
                registers[0] = address; //load immediate value into regA
                break;
            case JMP: //jump
                pc = address - 1;
                break;
            case JZ: //jump zero
                if(zf) pc = address - 1;
                break;
            case JC: //jump carry
                if(cf) pc = address - 1;
                break;
            case LOD: //load
                registers[0] = dataMem[address]; //load immediate register A to data memory @ address
                break;
            case STR: //store
                dataMem[address] = registers[0]; //set immediate data @ address to register A
                break;
            case OUT: //out
                System.out.println(registers[0]);
                break;
            default:
                break;

        }



    }


}
