import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

public class Parser {
    private final Scanner reader;
    public String command;

    public Parser(String f) throws IOException {
        // Initialize.
        Path obj = Path.of(f);
        reader = new Scanner(obj);

        if (!f.endsWith(".vm"))
        {
            throw new IllegalArgumentException("File needs to end with \".vm\".");
        }
    }

    public boolean hasMoreLines() {return reader.hasNextLine();}

    public void advance() {
        // Assume hasMoreLines() is true.
        String line = reader.nextLine().replaceAll("//.*", "").trim();
        if (!line.isEmpty()) {
            command = line;}
        else {advance();}
    }

    public String commandType() {
        return switch (command.toLowerCase()) {
            case "add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not" -> "arithmetic";
            default -> command.split(" ")[0];
        };
    }

    public String arg1() {
        if (Objects.equals(commandType(), "arithmetic")) {
            return command;
        }
        else return command.split(" ")[1];
    }

    public int arg2() {
        return Integer.parseInt(command.split(" ")[2]);
    }
}
