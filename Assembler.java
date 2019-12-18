import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Converts instruction set to machine code
 * Each instruction is on a separate line
 * Supported commands: 
 * lw {INST DST_REG, INT(SRC_REG)}
 * sw {INST SRC_REG, INT(DST_REG)}
 * 
 * add {INST DST_REG, OP_REG_1, OP_REG_2}
 * sub {INST DST_REG, OP_REG_1, OP_REG_2}
 * and {INST DST_REG, OP_REG_1, OP_REG_2}
 * or  {INST DST_REG, OP_REG_1, OP_REG_2}
 * slt {INST DST_REG, OP_REG_1, OP_REG_2}
 * 
 * addi {INST DST_REG, OP_REG_1, INT}
 * andi {INST DST_REG, OP_REG_1, INT}
 * ori  {INST DST_REG, OP_REG_1, INT}
 * 
 * sll  {INST DST_REG, OP_REG_1, INT}
 * 
 * slti {INST DST_REG, OP_REG_1, INT}
 * 
 * lui {INST DST_REG, INT}
 * 
 * jr {INST OP_REG_1}
 * 
 * j {INST INT}
 * 
 * beq {INST OP_REG_1, OP_REG_2 LABEL}
 * bne {INST OP_REG_1, OP_REG_2 LABEL}
 * 
 * labels: 
 */
public class Assembler {

    class Instruction {

        String instruction;
        String[] arguments;
    }

    HashMap<String, String> regexMatcher;
    HashMap<String, Integer> labels;

    Assembler() {
        this.regexMatcher = new HashMap<>();
        this.labels = new HashMap<>();
        // Register regex
        final String register_regex = "\\$(0|[kv][01]|at|a[0123]|s[0-7]|t[0-9]|gp|sp|fp|ra)";
        // Type 1
        final String reg_offset_reg = "\\s+" + register_regex + "\\s*,\\s*(-?\\d+)\\(" + register_regex + "\\)\\s*";
        this.regexMatcher.put("LW", "lw" + reg_offset_reg);
        this.regexMatcher.put("SW", "sw" + reg_offset_reg);
        // Type 2
        final String reg_reg_reg = "\\s+" + register_regex + "\\s*,\\s*" + register_regex + "\\s*,\\s*" + register_regex + "\\s*";
        this.regexMatcher.put("ADD", "add" + reg_reg_reg);
        this.regexMatcher.put("SUB", "sub" + reg_reg_reg);
        this.regexMatcher.put("AND", "and" + reg_reg_reg);
        this.regexMatcher.put("OR", "or" + reg_reg_reg);
        this.regexMatcher.put("SLT", "slt" + reg_reg_reg);
        // Type 3
        final String reg_reg_const = "\\s+" + register_regex + "\\s*,\\s*" + register_regex + "\\s*,\\s*(-?\\d+)\\s*";
        this.regexMatcher.put("ADDI", "addi" + reg_reg_const);
        this.regexMatcher.put("ANDI", "andi" + reg_reg_const);
        this.regexMatcher.put("ORI", "ori" + reg_reg_const);
        this.regexMatcher.put("SLL", "sll" + reg_reg_const);
        this.regexMatcher.put("SLTI", "slti" + reg_reg_const);
        // Type 4
        final String reg_const = "\\s+" + register_regex + "\\s*,\\s*(-?\\d+)\\s*";
        this.regexMatcher.put("LUI", "lui" + reg_const);
        // Type 5
        final String reg = "\\s+" + register_regex + "\\s*";
        this.regexMatcher.put("JR", "jr" + reg);
        // Type 6
        final String immediate = "\\s+(\\w+)\\s*";
        this.regexMatcher.put("J", "j" + immediate);
        // Type 7
        final String reg_reg_label = "\\s+" + register_regex + "\\s*,\\s*" + register_regex + "\\s*,\\s*(\\w+)\\s*";
        this.regexMatcher.put("BEQ", "beq" + reg_reg_label);
        this.regexMatcher.put("BNE", "bne" + reg_reg_label);
        // Type 8
        final String label = "\\s*(\\w+)\\s*:\\s*";
        this.regexMatcher.put("LABEL", label);
    }

    Instruction findInstruction(String input) {
        for (String inst : this.regexMatcher.keySet()) {
            String regex = this.regexMatcher.get(inst);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                Instruction instruction = new Instruction();
                instruction.instruction = inst;
                instruction.arguments = new String[matcher.groupCount()];
                for (int i = 0; i < instruction.arguments.length; ++i) {
                    instruction.arguments[i] = matcher.group(i + 1);
                }
                return instruction;
            }
        }
        return null;
    }

    private int getRegisterNumber(String register){
        System.out.println("Fetching register " + register);
        final String c1 = register.substring(0, register.length() - 1);
        System.out.println(c1);
        final String c2 = register.substring(register.length() - 1);
        System.out.println(c2);
        if(register.equals("0"))
            return 0;
        else if(c1.equals("a")){
            if(c2.equals("t"))
                return 1;
            else{
                int x = Integer.parseInt(c2);
                return 4 + x;
            }
        }else if(c1.equals("v")){
            int x = Integer.parseInt(c2);
            return 2 + x;
        }else if(c1.equals("t")){
            int x = Integer.parseInt(c2);
            if(x <= 7)
                return 8 + x;
            else 
                return 24 + x;
        }else if(c1.equals("s")){
            int x = Integer.parseInt(c2);
            return 16 + x;
        }else if(c1.equals("k")){
            int x = Integer.parseInt(c2);
            return 26 + x;
        }else if(register.equals("gp")){
            return 28;
        }else if(register.equals("sp")){
            return 29;
        }else if(register.equals("fp")){
            return 30;
        }else if(register.equals("ra")){
            return 31;
        }
        return -1;
    }

    private void writeNumber(BitSet bits, int start, int length, int number, boolean signed) {
        // Create bits
        String binary = null;
        if (signed) {
            binary = toTwosComplement(number, length);
        } else {
            binary = Integer.toBinaryString(number);
            binary = (new String(new char[length]).replace('\0', '0')) + binary;
            binary = binary.substring(binary.length() - length);
        }
        System.out.println(number + " BIN: " + binary);
        for (int i = start; i < start + length; ++i) {
            if (binary.charAt(i - start) == '1')
                bits.set(i, true);
            else
                bits.set(i, false);
        }
    }

    private void writeNumber(BitSet bits, int start, int length, int number) {
        writeNumber(bits, start, length, number, false);
    }

    private String toTwosComplement(int number, int length) {
        String out = "";
        // Get unsigned
        String uBin = Integer.toUnsignedString(Math.abs(number), 2);
        uBin = ((new String(new char[length]).replace('\0', '0')) + uBin);
        if (number < 0) {
            boolean found = false;
            for (int i = 0; i < length; ++i) {
                final int idx = uBin.length() - 1 - i;
                if (found) {
                    out = (uBin.charAt(idx) == '0' ? '1' : '0') + out;
                } else {
                    if (uBin.charAt(idx) == '1') {
                        found = true;
                    }
                    out = uBin.charAt(idx) + out;
                }
            }
        } else {
            uBin = (new String(new char[length]).replace('\0', '0')) + uBin;
            uBin = uBin.substring(uBin.length() - length);
            out = uBin;
        }

        return out;
    }

    private void printBitSet(BitSet bits) {
        for (int i = 0; i < bits.length(); ++i) {
            System.out.print(bits.get(i) ? 1 : 0);
        }
        System.out.println();
    }

    private byte[] toByteArray(BitSet bits, int length) {
        // Get number of bits
        final int len = (length + 7) / 8;
        // System.out.println(len);
        byte[] output = new byte[len];
        for (int i = 0; i < output.length * 8; ++i) {
            int byteIdx = i / 8;
            int byteOffset = i % 8;
            if (bits.get(i)) {
                output[byteIdx] |= (1 << 7 - byteOffset);
            } else {
                output[byteIdx] &= ~(1 << 7 - byteOffset);
            }
        }
        return output;
    }

    byte[] getMachineCode(Instruction inst) throws Exception {
        BitSet bits = new BitSet(32);
        switch (inst.instruction) {
        case "ADD": {
            bits.set(0, 6, false);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[2]));
            writeNumber(bits, 16, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 21, 5, 0);
            writeNumber(bits, 26, 6, 32);
            break;
        }
        case "SUB": {
            bits.set(0, 6, false);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[2]));
            writeNumber(bits, 16, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 21, 5, 0);
            writeNumber(bits, 26, 6, 34);
            break;
        }
        case "AND": {
            bits.set(0, 6, false);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[2]));
            writeNumber(bits, 16, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 21, 5, 0);
            writeNumber(bits, 26, 6, 36);
            break;
        }
        case "OR": {
            bits.set(0, 6, false);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[2]));
            writeNumber(bits, 16, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 21, 5, 0);
            writeNumber(bits, 26, 6, 37);
            break;
        }
        case "SLL": {
            writeNumber(bits, 0, 6, 0);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));// SRC
            writeNumber(bits, 11, 5, 0);// SRC2
            writeNumber(bits, 16, 5, getRegisterNumber(inst.arguments[0]));// DST
            writeNumber(bits, 21, 5, Integer.valueOf(inst.arguments[2])); // SHAMT
            writeNumber(bits, 26, 6, 0); // FN
            break;
        }
        case "SLT": {
            bits.set(0, 6, false);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[2]));
            writeNumber(bits, 16, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 21, 5, 0);
            writeNumber(bits, 26, 6, 42);
            break;
        }
        case "LW": {
            writeNumber(bits, 0, 6, 35);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[2]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[0]));
            final int offset = Integer.valueOf(inst.arguments[1]);
            if(offset % 4 != 0)
                throw new Exception("Memory offset is not divisible by 4");
            writeNumber(bits, 16, 16, offset, true);
            break;
        }
        case "SW": {
            writeNumber(bits, 0, 6, 43);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[2]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[0]));
            final int offset = Integer.valueOf(inst.arguments[1]);
            if(offset % 4 != 0)
                throw new Exception("Memory offset is not divisible by 4");
            writeNumber(bits, 16, 16, offset, true);
            break;
        }
        case "ADDI": {
            writeNumber(bits, 0, 6, 8);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 16, 16, Integer.valueOf(inst.arguments[2]), true);
            break;
        }
        case "ANDI": {
            writeNumber(bits, 0, 6, 12);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 16, 16, Integer.valueOf(inst.arguments[2]));
            break;
        }
        case "ORI": {
            writeNumber(bits, 0, 6, 13);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 16, 16, Integer.valueOf(inst.arguments[2]));
            break;
        }
        case "SLTI": {
            writeNumber(bits, 0, 6, 10);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[1]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 16, 16, Integer.valueOf(inst.arguments[2]));
            break;
        }
        case "LUI": {
            writeNumber(bits, 0, 6, 15);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 11, 5, 0);
            writeNumber(bits, 16, 16, Integer.valueOf(inst.arguments[1]));
            break;
        }
        case "JR": {
            writeNumber(bits, 0, 6, 0);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[0]));// SRC 1
            writeNumber(bits, 11, 5, 0); // SRC 2
            writeNumber(bits, 16, 5, 0);// DST 1
            writeNumber(bits, 21, 5, 0); // SHAMT
            writeNumber(bits, 26, 6, 8); // FUNC
            break;
        }
        case "J": {
            writeNumber(bits, 0, 6, 2);
            if (!this.labels.containsKey(inst.arguments[0])) {
                throw new Exception("Label " + inst.arguments[0] + " not found");
            }
            writeNumber(bits, 6, 26, this.labels.get(inst.arguments[0]));// N in program counter
            break;
        }
        case "BEQ": {
            writeNumber(bits, 0, 6, 4);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[1]));
            // Write label number
            if (!this.labels.containsKey(inst.arguments[2])) {
                throw new Exception("Label " + inst.arguments[2] + " not found");
            }
            writeNumber(bits, 16, 16, this.labels.get(inst.arguments[2]));
            break;
        }
        case "BNE": {
            writeNumber(bits, 0, 6, 5);
            writeNumber(bits, 6, 5, getRegisterNumber(inst.arguments[0]));
            writeNumber(bits, 11, 5, getRegisterNumber(inst.arguments[1]));
            if (!this.labels.containsKey(inst.arguments[2])) {
                throw new Exception("Label " + inst.arguments[2] + " not found");
            }
            writeNumber(bits, 16, 16, this.labels.get(inst.arguments[2]));
            break;
        }
        }
        return toByteArray(bits, 32);
    }

    public ArrayList<byte[]> compileCode(String code) throws Exception {
        String[] lines = code.split("\n");
        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<byte[]> output = new ArrayList<>();
        // Parse to find syntax errors and save label positions
        for (int i = 0; i < lines.length; ++i) {
            final String line = lines[i];
            if (line.trim().equals(""))
                continue;
            Instruction inst = this.findInstruction(line);
            if (inst == null) {
                throw new Exception("Line " + (i + 1) + " \'" + line + "\' is not a valid instruction");
            } else if (inst.instruction.equals("LABEL")) {
                if (this.labels.containsKey(inst.arguments[0])) {
                    throw new Exception("Label " + inst.arguments[0] + " duplicate, line " + (i + 1));
                }
                // Adding label
                this.labels.put(inst.arguments[0], instructions.size()); // Point to the next instruction
            } else {
                instructions.add(inst);
            }
        }
        for (int i = 0; i < instructions.size(); ++i) {
            output.add(this.getMachineCode(instructions.get(i)));
        }
        return output;
    }

    public void writeMachineCodeToFile(String filename, String code) throws Exception {
        ArrayList<byte[]> codes = this.compileCode(code);
        FileOutputStream dos = new FileOutputStream(new File(filename));
        for (byte[] buffer : codes) {
            dos.write(buffer);
        }
        dos.close();
    }

}
