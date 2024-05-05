import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class VMTranslator {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Enter one and only one argument.");
        }

        Parser parser;
        final CodeWriter writer;
        String filename = args[0];

        Path f = Path.of(filename);
        if (!Files.isDirectory(f)) {
            writer = new CodeWriter(filename.replace(".vm", ".asm"));
            parser = new Parser(filename);
            translate(parser, writer);
        }
        else {
            writer = new CodeWriter(filename);
            List<Path> worklist;
            try {
                worklist = Files.list(f).filter(path -> path.getFileName().toString().endsWith(".vm")).toList();
                for (Path path : worklist) {
                    parser = new Parser(path.toString());
                    writer.setFileName(path.getFileName().toString().replace(".vm", ""));
                    translate(parser, writer);
                }
            } catch (IOException e) {
                throw new RuntimeException("Can't open directory.");
            }
        }
        writer.close();
    }

    private static void translate(Parser parser, CodeWriter writer) throws IOException {
        do {
            parser.advance();
            System.out.println(parser.command);
            switch(parser.commandType()) {
                case "arithmetic" -> writer.writeArithmetic(parser.arg1());
                case "push" -> writer.writePushPop("push", parser.arg1(), parser.arg2());
                case "pop" -> writer.writePushPop("pop", parser.arg1(), parser.arg2());
                case "label" -> writer.writeLabel(parser.arg1());
                case "goto" -> writer.writeGoto(parser.arg1());
                case "if-goto" -> writer.writeIf(parser.arg1());
                case "function" -> writer.writeFunction(parser.arg1(), parser.arg2());
                case "call" -> writer.writeCall(parser.arg1(), parser.arg2());
                case "return" -> writer.writeReturn();
                default -> parser.advance();
            }
        } while (parser.hasMoreLines());
    }
}
