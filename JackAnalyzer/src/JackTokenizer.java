import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class JackTokenizer {
    private final Scanner reader;
    private final Queue<String> tokens = new ArrayDeque<>();
    private static final HashSet<String> validSymbol = new HashSet<>();
    private static final HashSet<String> validKeyword = new HashSet<>();
    public enum tokenTypes { keyword, symbol, identifier, integerConstant, stringConstant }

    static {
        final String[] symbols = new String[] {"{",  "}",  "(",  ")",  "[",  "]",  ".",  ",",  ";",  "+",  "-",  "*",  "/",  "&",  "|",  "<",  ">",  "=",  "~"};
        final String[] keywords = new String[] {"class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"};
        validSymbol.addAll(List.of(symbols));
        validKeyword.addAll(List.of(keywords));
    }

    public JackTokenizer(Path path) throws IOException {
        reader = new Scanner(path);
        parseLine();
    }

    public boolean hasMoreTokens() {
        return !tokens.isEmpty();
    }

    private String fetchNextNonCommentLine() {
        String line = reader.nextLine().replaceAll("//.*", "").trim();
        while (line.isEmpty() && reader.hasNextLine()) {
            line = fetchNextNonCommentLine();
        }
        if (line.contains("/*")) {
            while (!line.contains("*/") && reader.hasNextLine()) {
                line = reader.nextLine();
            }
            line = fetchNextNonCommentLine();
        }
        return line;
    }

    private void parseLine() {
        String line = fetchNextNonCommentLine();
        // Two pointers
        int left = 0;
        for (int right = 0; right < line.length(); right++) {
            // Ignore white spaces
            if (line.charAt(right) == ' ') {
                left = right + 1;
                continue;
            }
            else if (validSymbol.contains(String.valueOf(line.charAt(right)))) {
                tokens.offer(String.valueOf(line.charAt(right)));
                left = right + 1;
                continue;
            }
            // Start with a double quote
            else if (line.charAt(right) == '"') {
                while (right < line.length() - 1 && line.charAt(right + 1) != '"') {
                    right++;
                }
                // Include the closing double quote
                right++;
            }
            // Start with a char
            else if (Character.isLetter(line.charAt(right))) {
                while (right < line.length() - 1 && Character.isLetterOrDigit(line.charAt(right + 1))) {
                    right++;
                }
            }
            // Start with a digit
            else if (Character.isDigit(line.charAt(right))) {
                while (right < line.length() - 1 && Character.isDigit(line.charAt(right + 1))) {
                    right++;
                }
            }
            tokens.offer(line.substring(left, right + 1));
            left = right + 1;
        }
    }

    public void advance() {
        tokens.poll();
        if (tokens.isEmpty() && reader.hasNextLine()) {
            parseLine();
        }
    }

    public tokenTypes tokenType() {
        String s = tokens.peek();
        if (validSymbol.contains(s)) return tokenTypes.symbol;
        else if (validKeyword.contains(s)) return tokenTypes.keyword;
        else if (Character.isDigit(s.charAt(0))) return tokenTypes.integerConstant;
        else if (s.startsWith("\"")) return tokenTypes.stringConstant;
        else return tokenTypes.identifier;
    }

    public String KeyWord() {
        return tokens.peek();
    }

    public char symbol() {
        return tokens.peek().charAt(0);
    }

    public String identifier() {
        return tokens.peek();
    }

    public int intVal() {
        return Integer.parseInt(tokens.peek());
    }

    public String stringVal() {
        return tokens.peek().replaceAll("\"(.*)\"", "$1");
    }
}
