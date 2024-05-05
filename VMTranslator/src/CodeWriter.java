import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class CodeWriter {
    private final FileWriter writer;
    private final BufferedWriter bw;
    private int loopCounter;
    private String fileName;
    private String funcName;
    private int retCounter;

    public CodeWriter(String output) throws IOException {
        writer = new FileWriter(STR."\{output}.asm");
        bw = new BufferedWriter(writer);
        bw.write("@256"); bw.newLine();
        bw.write("D=A"); bw.newLine(); // D = 256
        bw.write("@0"); bw.newLine(); // Select SP
        bw.write("M=D"); bw.newLine(); // SP = 256
        writeCall("Sys.init", 0);
    }

    private void popDFromSP() throws IOException {
        bw.write("@0"); // Select SP
        bw.newLine();
        bw.write("M=M-1"); // RAM[0](SP)--
        bw.newLine();
        bw.write("A=M"); // Select SP
        bw.newLine();
        bw.write("D=M"); // D = RAM[SP]
        bw.newLine();
    }

    private void pushDtoStack() throws IOException {
        bw.write("@0"); // Select SP
        bw.newLine();
        bw.write("A=M"); // Select RAM[SP]
        bw.newLine();
        bw.write("M=D"); // RAM[SP] = D
        bw.newLine();
        bw.write("@0"); // Select SP
        bw.newLine();
        bw.write("M=M+1"); // RAM[0](SP)++
        bw.newLine();
    }

    private void changeLastOnStack(String command) throws IOException {
        // Exclusively for single variable operations.
        bw.write("@0"); // Select RAM[0](SP)
        bw.newLine();
        bw.write("A=M-1"); // Select RAM[SP--]
        bw.newLine();
        switch(command) {
            case "neg" -> bw.write("M=-M");
            case "not" -> bw.write("M=!M");
        }
        bw.newLine();
    }

    private void changeLastOnStackWithD(String command) throws IOException {
        // For double variable operations, assume second operand is in D.
        bw.write("@0"); // Select RAM[0](SP)
        bw.newLine();
        bw.write("A=M-1"); // Select RAM[SP--]
        bw.newLine();
        switch(command) {
            case "add" -> bw.write("M=M+D");
            case "sub" -> bw.write("M=M-D");
            case "and" -> bw.write("M=M&D");
            case "or" -> bw.write("M=M|D");
            case "gt", "eq", "lt" -> changeLastWithCond(command);
        }
        bw.newLine();
    }

    private void changeLastWithCond(String command) throws IOException {
        bw.write("D=M-D"); bw.newLine();
        bw.write("M=-1"); bw.newLine();
        bw.write(STR."@END\{loopCounter}"); bw.newLine();
        switch(command) {
            case "gt" -> bw.write("D;JGT");
            case "eq" -> bw.write("D;JEQ");
            case "lt" -> bw.write("D;JLT");
        }
        bw.newLine();
        bw.write("@0"); // Select RAM[0](SP)
        bw.newLine();
        bw.write("A=M-1"); // Select RAM[SP--]
        bw.newLine();
        bw.write("M=0"); bw.newLine();
        bw.write(STR."(END\{loopCounter})"); bw.newLine();
        loopCounter++;
    }

    public void writeArithmetic(String command) throws IOException {
        bw.write(STR."// \{command}"); bw.newLine();
        // Single variable operation doesn't need push and pop.
        if (Objects.equals(command, "neg") || Objects.equals(command, "not")) changeLastOnStack(command);
        // Double variables operation
        else {
            popDFromSP();
            changeLastOnStackWithD(command);
        }
    }

    private void fetchAddress(String segment, int index) throws IOException {
        bw.write(STR."@\{index}"); bw.newLine();
        bw.write("D=A"); bw.newLine();
        switch(segment) {
            case "local" -> bw.write("@1");
            case "argument" -> bw.write("@2");
            case "this" -> bw.write("@3");
            case "that" -> bw.write("@4");
        }
        bw.newLine(); bw.write("A=M+D");
        bw.newLine();
    }

    private void fetchStaticAddress(String segment, int index) throws IOException {
        switch(segment) {
            case "sp" -> bw.write("@0");
            case "temp" -> bw.write(STR."@\{index + 5}");
            case "static" -> bw.write(STR."@\{fileName}.\{index}");
            case "pointer" -> bw.write(STR."@\{index + 3}");
        }
        bw.newLine();
    }

    public void writePushPop(String command, String segment, int index) throws IOException {
        // Write comments
        bw.write(STR."// \{command} \{segment} \{index}"); bw.newLine();

        // Handle constants separately.
        if (segment.equals("constant")) {
            bw.write(STR."@\{index}"); bw.newLine();
            bw.write("D=A"); bw.newLine();
            pushDtoStack(); bw.newLine();
            return;
        }

        // Fetch Address to A.
        switch(segment) {
            case "sp" -> fetchAddress(segment, 0);
            case "local", "argument", "this", "that" -> fetchAddress(segment, index);
            case "static", "temp", "pointer" -> fetchStaticAddress(segment, index);
        }

        if (command.equals("push")) {
            bw.write("D=M"); bw.newLine();
            pushDtoStack();
        }
        else {
            bw.write("D=A"); bw.newLine();
            bw.write("@15"); bw.newLine();
            bw.write("M=D"); bw.newLine();
            popDFromSP();
            bw.write("@15"); bw.newLine();
            bw.write("A=M"); bw.newLine();
            bw.write("M=D"); bw.newLine();
        }
    }

    public void setFileName(String f) {
        fileName = f;
    }

    public void writeLabel(String label) throws IOException {
        bw.write(STR."// label \{label}"); bw.newLine();
        bw.write(STR."(\{funcName}$\{label})"); bw.newLine();
    }

    public void writeGoto(String label) throws IOException {
        bw.write(STR."// goto \{label}"); bw.newLine();
        bw.write(STR."@\{funcName}$\{label}"); bw.newLine();
        bw.write("0;JMP"); bw.newLine();
    }

    public void writeIf(String label) throws IOException {
        bw.write(STR."// if-goto \{label}"); bw.newLine();
        popDFromSP();
        bw.write(STR."@\{funcName}$\{label}"); bw.newLine();
        bw.write("D;JNE"); bw.newLine();
    }

    public void writeFunction(String functionName, int nVars) throws IOException {
        funcName = functionName;
        retCounter = 0;
        bw.write(STR."// label \{functionName}"); bw.newLine();
        bw.write(STR."(\{functionName})"); bw.newLine();
        int i = 0;
        while (i < nVars) {
            bw.write("@0"); bw.newLine();
            bw.write("D=A"); bw.newLine();
            pushDtoStack();
            i++;
        }
    }

    public void writeCall(String functionName, int nArgs) throws IOException {
        bw.write(STR."// call \{functionName}"); bw.newLine();
        // push retAddr, LCL, ARG, THIS, THAT
        bw.write(STR."@\{funcName}$ret.\{retCounter}"); bw.newLine();
        bw.write("D=A"); bw.newLine();
        pushDtoStack();
        bw.write("@1"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        pushDtoStack();
        bw.write("@2"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        pushDtoStack();
        bw.write("@3"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        pushDtoStack();
        bw.write("@4"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        pushDtoStack();
        // ARG = SP-5-nArgs;
        bw.write("@0"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        bw.write(STR."@\{5+nArgs}"); bw.newLine();
        bw.write("D=D-A"); bw.newLine();
        bw.write("@2"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // LCL = SP
        bw.write("@0"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        bw.write("@1"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // goto f
        bw.write(STR."@\{functionName}"); bw.newLine();
        bw.write("0;JMP"); bw.newLine();
        // (retAddr)
        bw.write(STR."(\{funcName}$ret.\{retCounter})");
        retCounter++;
    }

    public void writeReturn() throws IOException {
        bw.write(STR."// return from \{funcName}"); bw.newLine();
        // frame = LCL
        bw.write("@1"); bw.newLine();
        bw.write("D=M"); bw.newLine(); // D = LCL
        bw.write("@13"); bw.newLine(); // Select R13
        bw.write("M=D"); bw.newLine(); // R13 = D
        // retAddr = *(frame-5)
        bw.write("@5"); bw.newLine(); // Select 5
        bw.write("A=D-A"); bw.newLine(); // Select D-5
        bw.write("D=M"); bw.newLine(); // D = retAddr
        bw.write("@14"); bw.newLine();
        bw.write("M=D"); bw.newLine(); // R14 = D
        // *ARG = pop()
        popDFromSP();
        bw.write("@2"); bw.newLine();
        bw.write("A=M"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // SP = ARG+1
        bw.write("@2"); bw.newLine();
        bw.write("D=M+1"); bw.newLine();
        bw.write("@0"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // THAT = *(frame-1)
        bw.write("@13"); bw.newLine();
        bw.write("M=M-1"); bw.newLine();
        bw.write("A=M"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        bw.write("@4"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // THIS = *(frame-2)
        bw.write("@13"); bw.newLine();
        bw.write("M=M-1"); bw.newLine();
        bw.write("A=M"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        bw.write("@3"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // ARG = *(frame-3)
        bw.write("@13"); bw.newLine();
        bw.write("M=M-1"); bw.newLine();
        bw.write("A=M"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        bw.write("@2"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // LCL = *(frame-1)
        bw.write("@13"); bw.newLine();
        bw.write("M=M-1"); bw.newLine();
        bw.write("A=M"); bw.newLine();
        bw.write("D=M"); bw.newLine();
        bw.write("@1"); bw.newLine();
        bw.write("M=D"); bw.newLine();
        // goto retAddr
        bw.write("@14"); bw.newLine();
        bw.write("A=M"); bw.newLine();
        bw.write("0;JMP"); bw.newLine();
    }

    public void close() throws IOException {
        bw.close();
        writer.close();
    }
}
