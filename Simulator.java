import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

class Simulator extends JFrame{

    class RInstruction{
        int opcode;
        int r1;
        int r2;
        int r3;
        int shamt;
        int fn;
    }
    class IInstruction{
        int opcode;
        int r1;
        int r2;
        int immediate;
    }
    class JInstruction{
        int opcode;
        int immediate;
    }

    class ValueView{
        JLabel view;
        int value;
    }

    private static final long serialVersionUID = 1504897909001058412L;
    private JButton executeProgram;
    private JButton executeInstruction;
    private JLabel programCounter;
    private JLabel fullCode;
    private JLabel opcode;
    private JLabel instructionType;
    private int nextInstruction;
    private ArrayList<byte[]> commands;
    private HashMap<Integer, ValueView> registerContents;
    private JPanel registersView;
    private HashMap<Integer, ValueView> memoryContents;
    private JPanel memoryView;

    Simulator(){
        super("MIPS-32 Simulator");
        JPanel contentPane = new JPanel();
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        contentPane.setBorder(padding);
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        SimulatorEventListener el = new SimulatorEventListener();

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.LINE_AXIS));
        JButton loadProgram = new JButton("Load program");
        loadProgram.setActionCommand("load_program");
        loadProgram.addActionListener(el);
        executeProgram = new JButton("Execute Program");
        executeProgram.setEnabled(false);
        executeProgram.setActionCommand("execute_program");
        executeProgram.addActionListener(el);
        executeInstruction = new JButton("Execute Instruction");
        executeInstruction.setEnabled(false);
        executeInstruction.setActionCommand("execute_instruction");
        executeInstruction.addActionListener(el);
        programCounter = new JLabel("-1");
        programCounter.setBorder(BorderFactory.createLineBorder(Color.decode("#005500"), 3));
        programCounter.setFont(new Font("Monospaced", Font.PLAIN, 25));
        toolbar.add(loadProgram);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(executeProgram);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(executeInstruction);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(programCounter);
        this.add(toolbar);

        JPanel instruction_info = new JPanel();
        instruction_info.setLayout(new BoxLayout(instruction_info, BoxLayout.LINE_AXIS));
        opcode = new JLabel("XXXXXX");
        opcode.setFont(new Font("Monospaced", Font.PLAIN, 18));
        instruction_info.add(opcode);
        instruction_info.add(Box.createHorizontalGlue());
        fullCode = new JLabel("END");
        fullCode.setFont(new Font("sans-serif", Font.PLAIN, 12));
        instruction_info.add(fullCode);
        instruction_info.add(Box.createHorizontalGlue());
        instructionType = new JLabel();
        instructionType.setFont(new Font("sans-serif", Font.ITALIC, 14));
        instruction_info.add(instructionType);
        this.add(instruction_info);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        this.add(row);
        registersView = new JPanel();
        registersView.setLayout(new BoxLayout(registersView, BoxLayout.PAGE_AXIS));
        row.add(registersView);
        memoryView = new JPanel();
        memoryView.setLayout(new BoxLayout(memoryView, BoxLayout.PAGE_AXIS));
        row.add(memoryView);

        this.pack();
        this.setSize(new Dimension(600, 300));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    void loadNextInstruction(){
        if(nextInstruction >= commands.size()){
            this.fullCode.setText("END");
            this.opcode.setText("XXXXXX");
            this.instructionType.setText("");
            this.programCounter.setText("-1");
            this.executeInstruction.setEnabled(false);
            this.executeProgram.setEnabled(false);
            return;
        }
        byte[] instruction = commands.get(nextInstruction);
        final String instruction_code = getByteBits(instruction);
        this.fullCode.setText(instruction_code);
        final String opcode = instruction_code.substring(0, 6);
        this.opcode.setText(opcode);
        if(opcode == "000000") this.instructionType.setText("R Type");
        else if(opcode == "000010") this.instructionType.setText("J Type");
        else this.instructionType.setText("I Type");
        this.programCounter.setText(String.valueOf(nextInstruction));
    }

    void updateRegister(int n, int val){
        getRegister(n);
        ValueView vv = registerContents.get(n);
        vv.value = val;
        vv.view.setText("R" + n + ": " + val);
    }

    int getRegister(int n){
        // Check if register was accesses and added to view
        if(registerContents.containsKey(n)){
            return registerContents.get(n).value;
        }else{
            ValueView vv = new ValueView();
            vv.value = 0;
            vv.view = new JLabel();
            vv.view.setFont(new Font("Monospaced", Font.PLAIN, 16));
            registersView.add(vv.view);
            registerContents.put(n, vv);
            updateRegister(n, vv.value);
        }
        return 0;
    }

    void updateMemory(int address, int value){
        getMemory(address);
        ValueView vv = memoryContents.get(address);
        vv.value = value;
        vv.view.setText("0x" + Integer.toBinaryString(address) + ": " + value);
    }

    int getMemory(int address){
        if(memoryContents.containsKey(address)){
            return memoryContents.get(address).value;
        }else{
            ValueView vv = new ValueView();
            vv.value = 0;
            vv.view = new JLabel();
            vv.view.setFont(new Font("Monospaced", Font.PLAIN, 16));
            memoryView.add(vv.view);
            memoryContents.put(address, vv);
            updateMemory(address, vv.value);
        }
        return 0;
    }

    void executeNextInstruction(){
        byte[] code = commands.get(nextInstruction);
        final String instruction_code = getByteBits(code);
        final String opcode = instruction_code.substring(0, 6);
        switch(opcode){
            case "000000":{
                RInstruction instruction = decodeR(code);
                switch(instruction.fn){
                    case 32:{ // ADD
                        int r1 = getRegister(instruction.r1);
                        int r2 = getRegister(instruction.r2);
                        updateRegister(instruction.r3, r1+r2);
                        break;
                    }
                    case 34:{ // SUB
                        int r1 = getRegister(instruction.r1);
                        int r2 = getRegister(instruction.r2);
                        updateRegister(instruction.r3, r1-r2);
                        break;
                    }
                    case 36:{ // AND
                        int r1 = getRegister(instruction.r1);
                        int r2 = getRegister(instruction.r2);
                        updateRegister(instruction.r3, r1&r2);
                        break;
                    }
                    case 37:{ // OR
                        int r1 = getRegister(instruction.r1);
                        int r2 = getRegister(instruction.r2);
                        updateRegister(instruction.r3, r1|r2);
                        break;
                    }
                    case 42:{ // SLT
                        int r1 = getRegister(instruction.r1);
                        int r2 = getRegister(instruction.r2);
                        updateRegister(instruction.r3, r1<r2 ? 1 : 0);
                        break;
                    }
                    case 0:{ // SLL
                        int r1 = getRegister(instruction.r1);
                        updateRegister(instruction.r3, (r1 << instruction.shamt));
                        break;
                    }
                    case 8: { // JR
                        int r1 = getRegister(instruction.r1);
                        nextInstruction = r1-1;
                        break;
                    }
                }
                break;
            }
            case "100011":{ // LW
                IInstruction instruction = decodeI(code);
                int srcMemAdd = getRegister(instruction.r1);
                int srcMem = getMemory(srcMemAdd + instruction.immediate);
                updateRegister(instruction.r2, srcMem);
                break;
            }
            case "101011":{ // SW
                IInstruction instruction = decodeI(code);
                int dstMemAdd = getRegister(instruction.r1);
                int srcReg = getRegister(instruction.r2);
                updateMemory(dstMemAdd + instruction.immediate, srcReg);
                break;
            }
            case "001000":{ // ADDi
                IInstruction instruction = decodeI(code);
                int src = getRegister(instruction.r1);
                updateRegister(instruction.r2, src + instruction.immediate);
                break;
            }
            case "001100":{ // ANDI
                IInstruction instruction = decodeI(code);
                int src = getRegister(instruction.r1);
                updateRegister(instruction.r2, src & instruction.immediate);
                break;
            }
            case "001101":{ // ORI
                IInstruction instruction = decodeI(code);
                int src = getRegister(instruction.r1);
                updateRegister(instruction.r2, src | instruction.immediate);
                break;
            }
            case "001010":{ // SLTI
                IInstruction instruction = decodeI(code);
                int src = getRegister(instruction.r1);
                updateRegister(instruction.r2, src < instruction.immediate ? 1 : 0);
                break;
            }
            case "001111":{ // LUI
                IInstruction instruction = decodeI(code, false);
                System.out.println(instruction.immediate);
                System.out.println(instruction.immediate << 16);
                updateRegister(instruction.r1, (instruction.immediate << 16));
                break;
            }
            case "000010":{ // J
                JInstruction instruction = decodeJ(code);
                nextInstruction = instruction.immediate-1;
                break;
            }
            case "000100":{ // BEQ
                IInstruction instruction = decodeI(code);
                int reg1 = getRegister(instruction.r1);
                int reg2 = getRegister(instruction.r2);
                int pc = instruction.immediate;
                if(reg1 == reg2){
                    nextInstruction = pc-1;
                }
                break;
            }
            case "000101":{ // BNE
                IInstruction instruction = decodeI(code);
                int reg1 = getRegister(instruction.r1);
                int reg2 = getRegister(instruction.r2);
                int pc = instruction.immediate;
                if(reg1 != reg2){
                    nextInstruction = pc-1;
                }
                break;
            }
        }
        nextInstruction++;
        loadNextInstruction();
    }

    RInstruction decodeR(byte[] b){
        // 6 bits opcode, 5 src reg, 5 src reg, 5 dst reg, 5 shamt, 6 fn
        RInstruction instruction = new RInstruction();
        final String code = getByteBits(b);
        final String opcode = code.substring(0, 6);
        instruction.opcode = binaryStrToInt(opcode, false);
        final String src1 = code.substring(6, 11);
        instruction.r1 = binaryStrToInt(src1, false);
        final String src2 = code.substring(11, 16);
        instruction.r2 = binaryStrToInt(src2, false);
        final String dst = code.substring(16, 21);
        instruction.r3 = binaryStrToInt(dst, false);
        final String shamt = code.substring(21, 26);
        instruction.shamt = binaryStrToInt(shamt, false);
        final String fn = code.substring(26, 32);
        instruction.fn = binaryStrToInt(fn, false);
        return instruction;
    }

    IInstruction decodeI(byte[] b, boolean signedImmediate){
        // 6 bits opcode, 5 src reg, 5 src reg, 16 immediate
        IInstruction instruction = new IInstruction();
        final String code = getByteBits(b);
        final String opcode = code.substring(0, 6);
        instruction.opcode = binaryStrToInt(opcode, false);
        final String src = code.substring(6, 11);
        instruction.r1 = binaryStrToInt(src, false);
        final String dst = code.substring(11, 16);
        instruction.r2 = binaryStrToInt(dst, false);
        final String immediate = code.substring(16, 32);
        instruction.immediate = binaryStrToInt(immediate, signedImmediate);
        return instruction;
    }
    IInstruction decodeI(byte[] b){
        return decodeI(b, true);
    }

    JInstruction decodeJ(byte[] b){
        // 6 bits opcode, other immediate
        JInstruction instruction = new JInstruction();
        final String code = getByteBits(b);
        final String opcode = code.substring(0, 6);
        instruction.opcode = binaryStrToInt(opcode, false);
        final String immediate = code.substring(6, 32);
        instruction.immediate = binaryStrToInt(immediate, true);
        return instruction;
    }

    int binaryStrToInt(String binary, boolean signed){
        boolean positive = binary.charAt(0) == '0';
        if(signed && !positive){
            String inverted = "";
            for(int i = 0; i < binary.length(); ++i){
                inverted += binary.charAt(i) == '1' ? '0' : '1';
            }
            int val = Integer.parseInt(inverted, 2);
            return (val + 1) * -1;
        }else{
            return Integer.parseInt(binary, 2);
        }
    }


    String getByteBits(byte[] b){
        String output = "";
        for(int i = 0; i < b.length; ++i){
            for(int j = 7; j >= 0; --j){
                output += ((1 << j) & b[i]) > 0 ? '1' : '0';
            }
        }
        return output;
    }

    class SimulatorEventListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent arg0) {
            final String action = arg0.getActionCommand();
            switch(action){
                case "load_program":
                    JFileChooser fChooser = new JFileChooser(new File(System.getProperty("user.dir")));
                    int stat = fChooser.showOpenDialog(Simulator.this);
                    if(stat == JFileChooser.APPROVE_OPTION){
                        // Load codes
                        commands = new ArrayList<>();
                        registerContents = new HashMap<>();
                        memoryContents = new HashMap<>();
                        registersView.removeAll();
                        registersView.add(Box.createHorizontalGlue());
                        registersView.add(Box.createVerticalStrut(10));
                        memoryView.removeAll();
                        memoryView.add(Box.createHorizontalGlue());
                        registersView.add(Box.createVerticalStrut(10));
                        try {
                            FileInputStream fis = new FileInputStream(fChooser.getSelectedFile());
                            byte[] command = new byte[4];
                            while(fis.read(command) > 0){
                                byte[] b = Arrays.copyOf(command, 4);
                                commands.add(b);
                            }
                            fis.close();
                            executeProgram.setEnabled(true);
                            executeInstruction.setEnabled(true);
                            nextInstruction = 0;
                            loadNextInstruction();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "execute_instruction":
                    executeNextInstruction();
                    break;
                case "execute_program":
                    while(nextInstruction < commands.size()){
                        executeNextInstruction();
                    }
                    break;
            }
        }

    }

}
