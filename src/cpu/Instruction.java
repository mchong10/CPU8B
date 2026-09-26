package cpu;

public class Instruction {

    String opcode;
    String address;
    String rA;
    String rB;

    Instruction(String fullInst){
        this.opcode = fullInst.substring(0,4);
        this.address = fullInst.substring(4);
        this.rA = fullInst.substring(4,6);
        this.rB = fullInst.substring(6);
    }
}
