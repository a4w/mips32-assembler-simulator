import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

class IDE extends JFrame{
    private static final long serialVersionUID = 8291500763083492646L;

    JMenuBar menuBar;
    JTextArea code_tArea;
    String fileName;

    IDE() {
        super("MIPS-32 IDE");
        // Init frame
        this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.PAGE_AXIS));

        // Init main GUI components
        menuBar = new JMenuBar();
        code_tArea = new JTextArea();
        code_tArea.setFont(new Font("Monospaced", Font.PLAIN, 16));

        // Add menubar items
        MenuActionListener listener = new MenuActionListener();
        // FILE
        JMenu fileMenu = new JMenu("File");
        JMenuItem newFile = new JMenuItem("New file");
        newFile.setActionCommand("new_file");
        newFile.addActionListener(listener);
        JMenuItem openFile = new JMenuItem("Open file");
        openFile.setActionCommand("open_file");
        openFile.addActionListener(listener);
        JMenuItem saveFile = new JMenuItem("Save file");
        saveFile.setActionCommand("save_file");
        saveFile.addActionListener(listener);
        JMenuItem close = new JMenuItem("Close");
        close.setActionCommand("close");
        close.addActionListener(listener);
        fileMenu.add(newFile);
        fileMenu.add(openFile);
        fileMenu.add(saveFile);
        fileMenu.add(close);
        menuBar.add(fileMenu);

        // Compile
        JMenu compileMenu = new JMenu("Compile");
        JMenuItem compile = new JMenuItem("Build");
        compile.setActionCommand("compile");
        compileMenu.add(compile);
        compile.addActionListener(listener);
        menuBar.add(compileMenu);

        this.setJMenuBar(menuBar);
        this.add(code_tArea);
        this.setMinimumSize(new Dimension(500, 500));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    void writeToFile(){
        // Save text to file
        try {
            FileWriter fw = new FileWriter(fileName);
            fw.write(code_tArea.getText());
            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MenuActionListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent arg0) {
            final String action = arg0.getActionCommand();
            switch(action){
                case "compile":
                    if(fileName == null){
                        JOptionPane.showMessageDialog(IDE.this, "Please save file before compiling");
                        return;
                    }
                    Assembler assembler = new Assembler();
                try {
                    assembler.writeMachineCodeToFile(fileName + ".o", code_tArea.getText());
                    JOptionPane.showMessageDialog(IDE.this, "Compiled Successfully");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(IDE.this, e.getMessage());
                }
                break;
            case "save_file":
                if(fileName == null){
                    JFileChooser fChooser = new JFileChooser();
                    int status = fChooser.showSaveDialog(IDE.this);
                    if(status == JFileChooser.APPROVE_OPTION){
                        fileName = fChooser.getSelectedFile().getAbsolutePath();
                        writeToFile();
                    }
                }else{
                    writeToFile();
                }
                break;
            case "new_file":
                    if(fileName == null && !code_tArea.getText().trim().equals("")){
                        JOptionPane.showMessageDialog(IDE.this, "Please save file before closing");
                        return;
                    }
                    if(fileName != null) writeToFile();
                    code_tArea.setText("");

                break;

            case "open_file":
                    if(fileName == null && !code_tArea.getText().trim().equals("")){
                        JOptionPane.showMessageDialog(IDE.this, "Please save file before closing");
                        return;
                    }
                    if(fileName != null) writeToFile();
                    JFileChooser fChooser = new JFileChooser();
                    int status = fChooser.showOpenDialog(IDE.this);
                    if(status == JFileChooser.APPROVE_OPTION){
                        fileName = fChooser.getSelectedFile().getAbsolutePath();
                        Scanner fr;
                        try {
                            fr = new Scanner(new FileInputStream(fileName));
                            String tmp = "";
                            while(fr.hasNextLine()){
                                tmp += fr.nextLine() + '\n';
                            }
                        code_tArea.setText(tmp);
                        fr.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                break;
            case "close":
                if(fileName == null && !code_tArea.getText().trim().equals("")){
                    JOptionPane.showMessageDialog(IDE.this, "Please save file before closing");
                    return;
                }
                if(fileName != null) writeToFile();
                dispose();
                break;
            }
        }

    }
}
