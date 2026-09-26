package cpu;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

public class Assembler8B {

    //-------------------------------
    // THE ISA
    //4-bit opcodes in String form

    static final String
            NOP = "0000", HLT = "0001", ADD = "0010", SUB = "0011",
            NOR = "0100", AND = "0101", XOR = "0110", NOT = "0111",
            MOV = "1000", LDI = "1001", JMP = "1010", JZ = "1011",
            JC = "1100", LOD = "1101", STR = "1110", OUT = "1111";

    //-------------------------------
    // REGISTER DEFINITIONS

    static final String
            RA = "00", RB = "01", RC = "10", RD = "11";

    //-------------------------------
    // OTHER DEFINITIONS

    static final String EMPTY_ADDRESS = "0000";

    static final int MEM = 16; //16 instructions can fit in instruction memory
    static final int maxLines = 256; //source file can have blank lines and comments too
    static final int maxLabels = 64;
    static final int maxDefs = 64;

    //-------------------------------
    // LABELS
    //ex.
    //loop:
    //    JMP loop

    String[] labelNames = new String[maxLabels];
    int[] labelValues = new int[maxLabels];
    int labelCount;

    //-------------------------------
    // LABELS

    String saveLabels(String line, int address, int lineNumber) throws Exception {
        int colon = line.indexOf(':');
        String label;

        while (colon >= 0) {
            label = line.substring(0, colon).trim().toUpperCase();
            checkName(label, lineNumber);

            if (address >= MEM) {
                throw new Exception("Line " + lineNumber + ": label is outside instruction memory.");
            }

            addLabel(label, address, lineNumber);

            line = line.substring(colon + 1).trim();
            colon = line.indexOf(':');
        }

        return line;
    }

    String removeLabels(String line) {
        int colon = line.indexOf(':');

        while (colon >= 0) {
            line = line.substring(colon + 1).trim();
            colon = line.indexOf(':');
        }

        return line;
    }

    void addLabel(String name, int value, int lineNumber) throws Exception {
        if (findLabel(name) >= 0 || findDefine(name) >= 0) {
            throw new Exception("Line " + lineNumber + ": name already exists: " + name);
        }

        if (labelCount >= maxLabels) {
            throw new Exception("Line " + lineNumber + ": too many labels.");
        }

        labelNames[labelCount] = name;
        labelValues[labelCount] = value;
        labelCount++;
    }

    int findLabel(String name) {
        int i;

        name = name.toUpperCase();

        for (i = 0; i < labelCount; i++) {
            if (labelNames[i].equals(name)) {
                return labelValues[i];
            }
        }

        return -1;
    }


    //-------------------------------
    // DEFINES
    //Examples:
    //DEF TEN 10

    String[] defineNames = new String[maxDefs];
    int[] defineValues = new int[maxDefs];
    int defineCount;


    boolean isDefineLine(String line) {
        String[] word = line.split(" +");
        String first = word[0].toUpperCase();

        if (first.equals("DEF")) return true;
        if (first.equals(".DEF")) return true;
        if (first.equals("DEFINE")) return true;
        if (first.equals(".DEFINE")) return true;

        if (word.length >= 2 && word[1].toUpperCase().equals("EQU")) return true;

        return false;
    }

    void saveDefine(String line, int lineNumber) throws Exception {
        String[] word = line.split(" +");
        String first = word[0].toUpperCase();
        String name;
        String valueText;
        int value;

        if (first.equals("DEF") || first.equals(".DEF") || first.equals("DEFINE") || first.equals(".DEFINE")) {
            if (word.length != 3) {
                throw new Exception("Line " + lineNumber + ": define format is DEF name value.");
            }

            name = word[1].toUpperCase();
            valueText = word[2];
        } else {
            if (word.length != 3 || !word[1].toUpperCase().equals("EQU")) {
                throw new Exception("Line " + lineNumber + ": define format is name EQU value.");
            }

            name = word[0].toUpperCase();
            valueText = word[2];
        }

        checkName(name, lineNumber);
        value = parseNumber(valueText, lineNumber);
        addDefine(name, value, lineNumber);
    }

    void addDefine(String name, int value, int lineNumber) throws Exception {
        if (findDefine(name) >= 0 || findLabel(name) >= 0) {
            throw new Exception("Line " + lineNumber + ": name already exists: " + name);
        }

        if (defineCount >= maxDefs) {
            throw new Exception("Line " + lineNumber + ": too many defines.");
        }

        defineNames[defineCount] = name;
        defineValues[defineCount] = value;
        defineCount++;
    }

    int findDefine(String name) {
        int i;

        name = name.toUpperCase();

        for (i = 0; i < defineCount; i++) {
            if (defineNames[i].equals(name)) {
                return defineValues[i];
            }
        }

        return -1;
    }


    //-------------------------------
    //ASSEMBLER METHODS

    //file reading
    public Instruction[] assembleFile(String fileName) throws Exception {
        String[] lines = new String[maxLines];
        int lineCount = 0;

        Scanner file = new Scanner(new File(fileName));

        while (file.hasNextLine()) {
            if (lineCount >= maxLines) {
                file.close();
                throw new Exception("Too many lines in asm file. Max is " + maxLines + ".");
            }

            lines[lineCount] = file.nextLine();
            lineCount++;
        }

        file.close();

        return assembleLines(lines, lineCount);
    }

    public Instruction[] assembleLines(String[] lines, int lineCount) throws Exception {
        labelCount = 0;
        defineCount = 0;

        firstPass(lines, lineCount);          //find labels and definitions
        return secondPass(lines, lineCount);  //make Instruction objects
    }

    public void printMachineCode(Instruction[] program) {
        int i;

        for (i = 0; i < MEM; i++) {
            System.out.println(readableLine(i, program[i]));
        }
    }

    public void saveMachineCode(Instruction[] program, String fileName) throws Exception {
        int i;
        PrintWriter out = new PrintWriter(new FileWriter(fileName));

        for (i = 0; i < MEM; i++) {
            out.println(readableLine(i, program[i]));
        }

        out.close();
    }

    //-------------------------------
    // PASS 1
    //Builds labels

    void firstPass(String[] lines, int lineCount) throws Exception {
        int i;
        int address = 0;
        String line;

        for (i = 0; i < lineCount; i++) {
            line = cleanLine(lines[i]);
            line = saveLabels(line, address, i + 1);

            if (line.length() > 0) {
                if (isDefineLine(line)) {
                    saveDefine(line, i + 1);
                } else {
                    address++;

                    if (address > MEM) {
                        throw new Exception("Line " + (i + 1) + ": program is too large. Max is 16 instructions.");
                    }
                }
            }
        }
    }

    //-------------------------------
    // PASS 2
    //Compilation to Machine Code

    Instruction[] secondPass(String[] lines, int lineCount) throws Exception {
        Instruction[] program = new Instruction[MEM];
        int i;
        int address = 0;
        String line;

        //Fill unused instruction memory with NOPs so the CPU does not read null.
        for (i = 0; i < MEM; i++) {
            program[i] = new Instruction(NOP + EMPTY_ADDRESS);
        }

        for (i = 0; i < lineCount; i++) {
            line = cleanLine(lines[i]);
            line = removeLabels(line);

            if (line.length() > 0 && !isDefineLine(line)) {
                program[address] = assembleInstruction(line, i + 1);
                address++;
            }
        }

        return program;
    }

    //-------------------------------
    // CLEANING

    String cleanLine(String line) {
        int cut = line.length();
        int spot;

        //comments can start with ;, #, or //
        spot = line.indexOf(";");
        if (spot >= 0 && spot < cut) cut = spot;

        spot = line.indexOf("#");
        if (spot >= 0 && spot < cut) cut = spot;

        spot = line.indexOf("//");
        if (spot >= 0 && spot < cut) cut = spot;

        line = line.substring(0, cut);
        line = line.replace(',', ' ');
        line = line.replace('\t', ' ');
        line = line.trim();

        return line;
    }


    //-------------------------------
    // NAMES

    void checkName(String name, int lineNumber) throws Exception {
        int i;
        char c;

        if (name.length() == 0) {
            throw new Exception("Line " + lineNumber + ": blank name.");
        }

        c = name.charAt(0);
        if (c >= '0' && c <= '9') {
            throw new Exception("Line " + lineNumber + ": name cannot start with a number: " + name);
        }

        for (i = 0; i < name.length(); i++) {
            c = name.charAt(i);

            if (!isNameChar(c)) {
                throw new Exception("Line " + lineNumber + ": bad name: " + name);
            }
        }
    }

    boolean isNameChar(char c) {
        if (c >= 'A' && c <= 'Z') return true;
        if (c >= '0' && c <= '9') return true;
        if (c == '_') return true;
        return false;
    }

    //-------------------------------
    // ASSEMBLE ONE INSTRUCTION

    Instruction assembleInstruction(String line, int lineNumber) throws Exception {
        String[] word = line.split(" +");
        String mnemonic = word[0].toUpperCase();
        String opcode = getOpcode(mnemonic, lineNumber);
        String fullInst;
        String firstReg;
        String secondReg;
        int number;

        if (opcode.equals(NOP) || opcode.equals(HLT)) {
            checkWordCount(word, 1, lineNumber, mnemonic);
            fullInst = opcode + EMPTY_ADDRESS;
        } else if (opcode.equals(OUT)) {
            if (word.length > 2) { //either OUT or OUT rA
                throw new Exception("Line " + lineNumber + ": OUT uses 0 or 1 operands.");
            }

            if (word.length == 2 && !parseRegister(word[1], lineNumber).equals(RA)) {
                throw new Exception("Line " + lineNumber + ": CPU8B can only OUT rA right now.");
            }

            fullInst = opcode + EMPTY_ADDRESS;
        } else if (isRegisterInstruction(opcode)) {
            checkWordCount(word, 3, lineNumber, mnemonic);

            firstReg = parseRegister(word[1], lineNumber);
            secondReg = parseRegister(word[2], lineNumber);

            //register instructions use bits 3-2 for the first register
            //and bits 1-0 for the second register / destination register
            fullInst = opcode + firstReg + secondReg;
        } else {
            checkWordCount(word, 2, lineNumber, mnemonic);

            number = parseNumberOrName(word[1], lineNumber);

            //immediate/address instructions use the low 4 bits as one number
            fullInst = opcode + toBits(number, 4);
        }

        return new Instruction(fullInst);
    }

    boolean isRegisterInstruction(String opcode) {
        if (opcode.equals(ADD)) return true;
        if (opcode.equals(SUB)) return true;
        if (opcode.equals(NOR)) return true;
        if (opcode.equals(AND)) return true;
        if (opcode.equals(XOR)) return true;
        if (opcode.equals(NOT)) return true;
        if (opcode.equals(MOV)) return true;
        return false;
    }

    void checkWordCount(String[] word, int wanted, int lineNumber, String mnemonic) throws Exception {
        if (word.length != wanted) {
            throw new Exception("Line " + lineNumber + ": " + mnemonic + " uses " + (wanted - 1) + " operand(s).");
        }
    }

    //-------------------------------
    // PARSE MNEMONICS, REGISTERS, NUMBERS, LABELS, AND DEFINES

    String getOpcode(String mnemonic, int lineNumber) throws Exception {
        if (mnemonic.equals("NOP")) return NOP;
        if (mnemonic.equals("HLT")) return HLT;
        if (mnemonic.equals("ADD")) return ADD;
        if (mnemonic.equals("SUB")) return SUB;
        if (mnemonic.equals("NOR")) return NOR;
        if (mnemonic.equals("AND")) return AND;
        if (mnemonic.equals("XOR")) return XOR;
        if (mnemonic.equals("NOT")) return NOT;
        if (mnemonic.equals("MOV")) return MOV;
        if (mnemonic.equals("LDI")) return LDI;
        if (mnemonic.equals("JMP")) return JMP;
        if (mnemonic.equals("JZ")) return JZ;
        if (mnemonic.equals("JC")) return JC;
        if (mnemonic.equals("LOD")) return LOD;
        if (mnemonic.equals("STR")) return STR;
        if (mnemonic.equals("OUT")) return OUT;

        throw new Exception("Line " + lineNumber + ": unknown instruction: " + mnemonic);
    }

    String parseRegister(String text, int lineNumber) throws Exception {
        text = text.toUpperCase();

        if (text.equals("RA") || text.equals("A") || text.equals("R0") || text.equals("0")) return RA;
        if (text.equals("RB") || text.equals("B") || text.equals("R1") || text.equals("1")) return RB;
        if (text.equals("RC") || text.equals("C") || text.equals("R2") || text.equals("2")) return RC;
        if (text.equals("RD") || text.equals("D") || text.equals("R3") || text.equals("3")) return RD;

        throw new Exception("Line " + lineNumber + ": bad register: " + text);
    }

    int parseNumberOrName(String text, int lineNumber) throws Exception {
        String upper = text.toUpperCase();
        int value;

        value = findLabel(upper);
        if (value >= 0) {
            return value;
        }

        value = findDefine(upper);
        if (value >= 0) {
            return value;
        }

        return parseNumber(text, lineNumber);
    }

    int parseNumber(String text, int lineNumber) throws Exception {
        String upper = text.toUpperCase();
        int value;

        try {
            if (upper.startsWith("0B")) {
                value = Integer.parseInt(upper.substring(2), 2);
            } else if (upper.startsWith("0X")) {
                value = Integer.parseInt(upper.substring(2), 16);
            } else {
                value = Integer.parseInt(upper);
            }
        } catch (NumberFormatException e) {
            throw new Exception("Line " + lineNumber + ": bad number or missing name: " + text);
        }

        if (value < 0 || value > 15) {
            throw new Exception("Line " + lineNumber + ": number must fit in 4 bits, 0 to 15: " + text);
        }

        return value;
    }

    //-------------------------------
    // READABLE MACHINE CODE OUTPUT

    String readableLine(int address, Instruction inst) {
        String fullInst = inst.opcode + inst.address;

        return toBits(address, 4) + "  " +
                inst.opcode + " " + inst.rA + " " + inst.rB + "  " +
                "0b" + fullInst + "  " +
                "0x" + toHex(fullInst) + "  " +
                getMnemonicFromOpcode(inst.opcode);
    }

    String getMnemonicFromOpcode(String opcode) {
        if (opcode.equals(NOP)) return "NOP";
        if (opcode.equals(HLT)) return "HLT";
        if (opcode.equals(ADD)) return "ADD";
        if (opcode.equals(SUB)) return "SUB";
        if (opcode.equals(NOR)) return "NOR";
        if (opcode.equals(AND)) return "AND";
        if (opcode.equals(XOR)) return "XOR";
        if (opcode.equals(NOT)) return "NOT";
        if (opcode.equals(MOV)) return "MOV";
        if (opcode.equals(LDI)) return "LDI";
        if (opcode.equals(JMP)) return "JMP";
        if (opcode.equals(JZ)) return "JZ";
        if (opcode.equals(JC)) return "JC";
        if (opcode.equals(LOD)) return "LOD";
        if (opcode.equals(STR)) return "STR";
        if (opcode.equals(OUT)) return "OUT";
        return "???";
    }

    String toBits(int value, int bits) {
        String text = Integer.toBinaryString(value);

        while (text.length() < bits) {
            text = "0" + text;
        }

        if (text.length() > bits) {
            text = text.substring(text.length() - bits);
        }

        return text;
    }

    String toHex(String bits) {
        int value = Integer.parseInt(bits, 2);
        String text = Integer.toHexString(value).toUpperCase();

        while (text.length() < 2) {
            text = "0" + text;
        }

        return text;
    }

}