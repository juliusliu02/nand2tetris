import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Assembler {
    private static final int length = 16;
    private static int symbolCounter = 16;
    private final Parser parser;
    private final Parser parserMain;
    private final FileWriter writer;
    private final BufferedWriter bw;
    private final SymbolTable st;

    public Assembler(String source) throws IOException {
        parser = new Parser(source);
        parserMain = new Parser(source);
        writer = new FileWriter(source.replace(".asm", ".hack"));
        bw = new BufferedWriter(writer);
        st = new SymbolTable();
    }

    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static String padLeftZeros(String inputString) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    private void firstPass() {
        int counter = 0;
        while (parser.hasMoreLines()) {
            parser.advance();
            if (parser.instructionType().equals("A_INSTRUCTION") || parser.instructionType().equals("C_INSTRUCTION")) {
                counter++;
            }
            else if (parser.instructionType().equals("L_INSTRUCTION"))
                st.addEntry(parser.symbol(), counter);
        }
    }

    public static void main(String[] args) throws IOException {
        Assembler asm = new Assembler(args[0]);
        asm.firstPass();

        do {
            asm.parserMain.advance();
            String binary;
            String insType = asm.parserMain.instructionType();
            if (insType.equals("L_INSTRUCTION")) {
                continue;
            }
            if (insType.equals("C_INSTRUCTION")) {
                binary = STR."111\{Code.comp(asm.parserMain.comp())}\{Code.dest(asm.parserMain.dest())}\{Code.jump(asm.parserMain.jump())}";
            }
            else {
                String s = asm.parserMain.symbol();
                if (isNumeric(s)) {
                    binary = padLeftZeros(Integer.toBinaryString(Integer.parseInt(s)));
                }
                else if (asm.st.contains(s)) {
                    binary = padLeftZeros(Integer.toBinaryString(asm.st.getAddress(s)));
                }
                else {
                    asm.st.addEntry(s, symbolCounter);
                    binary = padLeftZeros(Integer.toBinaryString(symbolCounter));
                    symbolCounter++;
                }
            }
            asm.bw.write(binary);
            asm.bw.newLine();
        } while (asm.parserMain.hasMoreLines());

        asm.bw.close();
        asm.writer.close();
    }
}
