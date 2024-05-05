import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {
    private final BufferedWriter bw;
    public enum Segment {CONSTANT, ARGUMENT, LOCAL, STATIC, THIS, THAT, POINTER, TEMP}
    public enum Arithmetic {ADD, NEG, EQ, GT, LT, AND, OR, NOT}

    public VMWriter(FileWriter output) {
        this.bw = new BufferedWriter(output);
    }

    private void writeWithLineBreak(String command) throws IOException {
        bw.write(command); bw.newLine();
    }

    private Segment mapKindToSegment(String kind) {
        switch (kind.toLowerCase()) {
            case "static" -> {
                return Segment.STATIC;
            }
            case "arg" -> {
                return Segment.ARGUMENT;
            }
            case "var" -> {
                return Segment.LOCAL;
            }
            case "int" -> {
                return Segment.CONSTANT;
            }
            case "field" -> {
                return Segment.THIS;
            }
            case "that" -> {
                return Segment.THAT;
            }
            case "temp" -> {
                return Segment.TEMP;
            }
            case "pointer" -> {
                return Segment.POINTER;
            }
            default -> throw new UnsupportedOperationException(STR."Write \{kind} not implemented.");
        }
    }

    public void writePush(String kind, int index) throws IOException {
        writeWithLineBreak(STR."push \{mapKindToSegment(kind).name().toLowerCase()} \{index}");
    }

    public void writePop(String kind, int index) throws IOException {
        writeWithLineBreak(STR."pop \{mapKindToSegment(kind).name().toLowerCase()} \{index}");
    }

    private Arithmetic mapOpToArithmetic(Character op) {
        switch (op) {
            case '-' -> {
                return Arithmetic.NEG;
            }
            case '~' -> {
                return Arithmetic.NOT;
            }
            case '+' -> {
                return Arithmetic.ADD;
            }
            case '>' -> {
                return Arithmetic.GT;
            }
            case '<' -> {
                return Arithmetic.LT;
            }
            case '=' -> {
                return Arithmetic.EQ;
            }
            case '&' -> {
                return Arithmetic.AND;
            }
            case '|' -> {
                return Arithmetic.OR;
            }
            default -> throw new UnsupportedOperationException(STR."Write \{op} Unimplemented.");
        }
    }

    public void writeArithmetic(Character op) throws IOException {
        switch (op) {
            case '*' -> writeCall("Math.multiply", 2);
            case '/' -> writeCall("Math.divide", 2);
            default -> writeWithLineBreak(mapOpToArithmetic(op).name().toLowerCase());
        }
    }

    public void writeLabel(String label) throws IOException {
        writeWithLineBreak(STR."label \{label}");
    }

    public void writeGoto(String label) throws IOException {
        writeWithLineBreak(STR."goto \{label}");
    }

    public void writeIf(String label) throws IOException {
        writeWithLineBreak(STR."if-goto \{label}");
    }

    public void writeCall(String name, int nArgs) throws IOException {
        writeWithLineBreak(STR."call \{name} \{nArgs}");
    }

    public void writeFunction(String name, int nVars) throws IOException {
        writeWithLineBreak(STR."function \{name} \{nVars}");
    }

    public void writeReturn() throws IOException {
        writeWithLineBreak("return");
    }

    public void close() throws IOException {
        bw.close();
    }
}
