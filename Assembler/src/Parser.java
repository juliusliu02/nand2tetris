import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

public class Parser {
    private final Scanner reader;
    private String instruction;
    public int counter = -1;

    public Parser(String f) {
        // Initialize.
        try {
            File obj = new File(f);
            reader = new Scanner(obj);
        }
        catch (FileNotFoundException e) {
            throw new NoSuchElementException("File not found.");
        }
        if (!f.endsWith(".asm"))
        {
            throw new IllegalArgumentException("File needs to end with \".asm\".");
        }
    }

    public boolean hasMoreLines() {return reader.hasNextLine();}

    public void advance() {
        // Assume hasMoreLines() is true.
        String line = reader.nextLine().trim().replaceAll("//.*", "");
        if (!line.isEmpty()) {counter++; instruction = line;}
        else {advance();}
    }

    public String instructionType() {
        // Assume all lines are valid.
        if (instruction.startsWith("@")) {
            return "A_INSTRUCTION";
        }
        else if (instruction.startsWith("(")) {
            return "L_INSTRUCTION";
        }
        else {
            return "C_INSTRUCTION";
        }
    }

    public String symbol() {
        // Assume only called with A- or L-instructions.
        String symbol;
        if (instruction.startsWith("@")) {
            symbol = instruction.replace("@", "");
        }
        else {
            symbol = instruction.replace("(", "").replace(")", "");
        }
        return symbol;
    }

    public String dest() {
        // Assume only called with C-instructions.
        if (instruction.contains("=")) {
            return instruction.replaceAll("=.*", "");
        }
        else return "";
    }

    public String comp() {
        // Assume only called with C-instructions.
        return instruction.replaceAll(".*=", "").replaceAll(";.*", "");
    }

    public String jump() {
        // Assume only called with C-instructions.
        if (instruction.contains(";")) {
            return instruction.replaceAll(".*;", "");
        }
        return "";
    }

    public static void main(String[] args) {
        Parser parser = new Parser(args[0]);
        while (parser.hasMoreLines()) {
            parser.advance();
            System.out.println(parser.instructionType());
            if (Objects.equals(parser.instructionType(), "C_INSTRUCTION")) {
                System.out.println(STR."\{parser.dest()} AND \{parser.comp()} AND \{parser.jump()}");
            }
            else {System.out.println(parser.symbol());}
        }
    }
}
