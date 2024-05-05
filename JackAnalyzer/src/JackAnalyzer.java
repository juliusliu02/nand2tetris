import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class JackAnalyzer {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java JackAnalyzer <filename/dirname>");
        }

        Path path = Path.of(args[0]);
        if (!Files.isDirectory(path)) {
            compile(path);
        }
        else {
            List<Path> worklist;
            try (Stream<Path> stream = Files.list(path)) {
                worklist = stream.filter(p -> p.getFileName().toString().endsWith(".jack")).toList();
                worklist.forEach(p -> {
                    try {
                        compile(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private static void compile(Path path) throws IOException {
        JackTokenizer jt = new JackTokenizer(path);
        FileWriter fw = new FileWriter(path.toString().replaceAll("(.*).jack", "$1.vm"));
        CompilationEngine ce = new CompilationEngine(jt, fw);
        ce.compileClass();
    }
}
