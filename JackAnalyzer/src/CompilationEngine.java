import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CompilationEngine {
    private final JackTokenizer jt;
    private final VMWriter vmw;
    private final SymbolTable classTable;
    private SymbolTable subTable;

    private static final HashSet<String> ops = new HashSet<>();
    private String className;
    private String subroutineName;
    private String subroutineKind;
    private int branchCount;

    static {
        final String[] op = new String[] {"+",  "-",  "*",  "/",  "&",  "|",  "<",  ">",  "="};
        ops.addAll(Arrays.asList(op));
    }

    public CompilationEngine(JackTokenizer input, FileWriter output) {
        jt = input;
        vmw = new VMWriter(output);
        classTable = new SymbolTable();
    }

    private boolean isKeyword(String keyword) {
        return jt.tokenType() == JackTokenizer.tokenTypes.keyword && jt.KeyWord().equals(keyword);
    }

    private boolean isSymbol(char c) {
        return jt.tokenType() == JackTokenizer.tokenTypes.symbol && jt.symbol() == c;
    }

    private void interrupt() throws IOException {
        vmw.close();
        throw new RuntimeException(STR."Syntax error found for \{jt.KeyWord()}");
    }

    public void compileClass() throws IOException {
        // A routine check for its opening keyword and closing symbol.
        // Opening check
        if (isKeyword("class")) {
            // keyword, identifier, open bracket
            jt.advance();
            className = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
            jt.advance();
        } else interrupt();

        while (isKeyword("field") || isKeyword("static")) {
            compileClassVarDec();
        }
        // Assume compileClassVarDec advanced the token.
        while (isKeyword("constructor") || isKeyword("method") || isKeyword("function")) {
            compileSubroutine();
        }

        // Closing check
        if (isSymbol('}')) {
            jt.advance();
            vmw.close();
        } else interrupt();
        // for debugging purposes
        // System.out.println(classTable);
    }

    private void defineInClass(String kind, String type, String identifier) {
        classTable.define(SymbolTable.Kind.valueOf(kind.toUpperCase()), type, identifier);
        // for debugging purposes
        // System.out.println(STR."\{kind} \{type} \{identifier} is defined in class.");
    }

    private void defineInSubroutine(String kind, String type, String identifier) {
        subTable.define(SymbolTable.Kind.valueOf(kind.toUpperCase()), type, identifier);
        // for debugging purposes
        // System.out.println(STR."\{kind} \{type} \{identifier} is defined in subroutine.");
    }

    private String fetchAndAdvance(JackTokenizer.tokenTypes type) {
        String result = switch (type) {
            case keyword -> jt.KeyWord();
            case identifier -> jt.identifier();
            case symbol -> String.valueOf(jt.symbol());
            default -> throw new UnsupportedOperationException(STR."fetch \{type} Not implemented.");
        };
        jt.advance();
        return result;
    }

    public void compileClassVarDec() {

        // kind, type, identifier
        String kind = fetchAndAdvance(JackTokenizer.tokenTypes.keyword);
        String type = fetchAndAdvance(jt.tokenType() == JackTokenizer.tokenTypes.keyword ? JackTokenizer.tokenTypes.keyword : JackTokenizer.tokenTypes.identifier);
        String identifier = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
        defineInClass(kind, type, identifier);

        while (isSymbol(',')) {
            jt.advance();
            identifier = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
            defineInClass(kind, type, identifier);
        }

        // ;
        jt.advance();
    }

    public void compileSubroutine() throws IOException {
        subTable = new SymbolTable();
        // keyword, type/identifier, identifier, open parenthesis
        subroutineKind = fetchAndAdvance(JackTokenizer.tokenTypes.keyword);
        if (subroutineKind.equals("method")) {
            defineInSubroutine("arg", className, "this");
        }
        // String subroutineType = fetchAndAdvance(jt.tokenType() == JackTokenizer.tokenTypes.keyword ? JackTokenizer.tokenTypes.keyword : JackTokenizer.tokenTypes.identifier);
        jt.advance(); // Not clear so far if type is used
        subroutineName = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
        // Reset branching
        branchCount = 0;
        jt.advance();
        compileParameterList();
        // close parenthesis
        jt.advance();
        compileSubroutineBody();
        // for debugging purposes
        // System.out.println(subTable);
    }

    public void compileParameterList() {
        // compileParameterList does not handle parentheses
        while (!isSymbol(')')) {
            // , + type + identifier
            if (isSymbol(',')) {
                jt.advance();
                continue;
            }
            // type + identifier
            String type = fetchAndAdvance(jt.tokenType() == JackTokenizer.tokenTypes.keyword ? JackTokenizer.tokenTypes.keyword : JackTokenizer.tokenTypes.identifier);
            String identifier = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
            defineInSubroutine("arg", type, identifier);
        }
    }

    public void compileSubroutineBody() throws IOException {
        // compileSubroutineBody handles curly brackets.
        if (isSymbol('{')) {
            jt.advance();
        } else interrupt();

        // Define variables
        compileVarDec();
        vmw.writeFunction(STR."\{className}.\{subroutineName}", subTable.varCount(SymbolTable.Kind.VAR));

        if (subroutineKind.equals("method")) {
            vmw.writePush("arg", 0);
            vmw.writePop("pointer", 0);
        } else if (subroutineKind.equals("constructor")) {
            vmw.writePush("int", classTable.varCount(SymbolTable.Kind.FIELD));
            vmw.writeCall("Memory.alloc", 1);
            vmw.writePop("pointer", 0);
        }
        compileStatements();

        if (isSymbol('}')) {
            jt.advance();
        } else interrupt();
    }

    public void compileVarDec() {
        while (isKeyword("var")) {
            // var type name
            String keyword = fetchAndAdvance(JackTokenizer.tokenTypes.keyword);
            String type = fetchAndAdvance(jt.tokenType() == JackTokenizer.tokenTypes.keyword ? JackTokenizer.tokenTypes.keyword : JackTokenizer.tokenTypes.identifier);
            String identifier = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
            defineInSubroutine(keyword, type, identifier);

            while (!isSymbol(';')) {
                jt.advance();
                identifier = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
                defineInSubroutine(keyword, type, identifier);
            }

            // ;
            jt.advance();
        }
    }

    public void compileStatements() throws IOException {
        while (isKeyword("let") || isKeyword("if") || isKeyword("while") || isKeyword("do") || isKeyword("return")) {
            switch (jt.KeyWord()) {
                case "let" -> compileLet();
                case "if" -> compileIf();
                case "while" -> compileWhile();
                case "do" -> compileDo();
                case "return" -> compileReturn();
                default -> interrupt();
            }
        }
    }

    private void popVar(String varName) throws IOException {
        if (subTable.kindOf(varName) != SymbolTable.Kind.NONE) {
            vmw.writePop(subTable.kindOf(varName).name(), subTable.indexOf(varName));
        } else if (classTable.kindOf(varName) != SymbolTable.Kind.NONE) {
            vmw.writePop(classTable.kindOf(varName).name(), classTable.indexOf(varName));
        }
        else throw new NoSuchElementException(STR."Tries to pop a var that's not defined: \{varName}.");
    }

    private void pushVar(String varName) throws IOException {
        if (subTable.kindOf(varName) != SymbolTable.Kind.NONE) {
            vmw.writePush(subTable.kindOf(varName).name(), subTable.indexOf(varName));
        } else if (classTable.kindOf(varName) != SymbolTable.Kind.NONE) {
            vmw.writePush(classTable.kindOf(varName).name(), classTable.indexOf(varName));
        }
        else throw new NoSuchElementException(STR."Tries to push a var that's not defined: \{varName}.");
    }

    public void compileLet() throws IOException {
        // let, identifier, =, identifier/constant, ;

        // let varName
        jt.advance();
        String varName = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);

        if (isSymbol('[')) {
            // [
            pushVar(varName);
            jt.advance();
            compileExpression();
            // ]
            jt.advance();
            vmw.writeArithmetic('+');
            jt.advance();
            compileExpression();
            vmw.writePop("temp", 0);
            vmw.writePop("pointer", 1);
            vmw.writePush("temp", 0);
            vmw.writePop("that", 0);
        }
        else {
            // =
            jt.advance();
            compileExpression();

            if (isDefined(varName)) {
                popVar(varName);
            } else {
                throw new RuntimeException(STR."\{varName} is undefined.");
            }
        }

        // ;
        jt.advance();
    }

    public void compileIf() throws IOException {
        int currentCount = branchCount++;
        // if, (
        jt.advance();
        jt.advance();
        compileExpression();
        vmw.writeArithmetic('~');
        // If condition is not true, skip to ELSE branch
        vmw.writeIf(STR."\{className}.\{subroutineName}$br\{currentCount}IF-ELSE");
        // ) {
        jt.advance();
        jt.advance();
        // THEN branch
        compileStatements();
        // Skip ELSE branch
        vmw.writeGoto(STR."\{className}.\{subroutineName}$br\{currentCount}IF-OUT");
        // Label for branch 0
        vmw.writeLabel(STR."\{className}.\{subroutineName}$br\{currentCount}IF-ELSE");
        // compileStatements terminate at }
        jt.advance();
        // branch 1
        if (isKeyword("else")) {
            // else {
            jt.advance();
            jt.advance();
            compileStatements();
            // }
            jt.advance();
        }
        vmw.writeLabel(STR."\{className}.\{subroutineName}$br\{currentCount}IF-OUT");
    }

    public void compileWhile() throws IOException {
        int currentCount = branchCount++;
        // while (
        jt.advance();
        jt.advance();
        vmw.writeLabel(STR."\{className}.\{subroutineName}$br\{currentCount}WHILE-EXP");
        compileExpression();
        vmw.writeArithmetic('~');
        vmw.writeIf(STR."\{className}.\{subroutineName}$br\{currentCount}WHILE-OUT");
        // ) {
        jt.advance();
        jt.advance();
        compileStatements();
        vmw.writeGoto(STR."\{className}.\{subroutineName}$br\{currentCount}WHILE-EXP");
        // compileStatements terminate at }
        jt.advance();
        vmw.writeLabel(STR."\{className}.\{subroutineName}$br\{currentCount}WHILE-OUT");
    }

    public void compileDo() throws IOException {
        // do
        jt.advance();
        // subroutineCall
        lookAhead();
        // ;
        jt.advance();
        vmw.writePop("temp", 0);
    }

    public void compileReturn() throws IOException {
        // return
        jt.advance();
        // expression?
        if (!isSymbol(';')) {
            compileExpression();
        }
        else vmw.writePush("int", 0);
        vmw.writeReturn();
        // ;
        jt.advance();
    }

    private boolean nextIsOp() {
        return jt.tokenType() == JackTokenizer.tokenTypes.symbol && ops.contains(String.valueOf(jt.symbol()));
    }

    public void compileExpression() throws IOException {
        // term (op term)*
        Deque<Character> stack = new ArrayDeque<>();
        compileTerm();
        while (nextIsOp()) {
            char op = fetchAndAdvance(JackTokenizer.tokenTypes.symbol).charAt(0);
            if (isSymbol('=')) {
                // >= or <=
                if (op == '<') stack.push('>');
                else if (op == '>') stack.push('<');
                stack.push('~');
                jt.advance();
            } else stack.push(op);
            compileTerm();
        }
        while (!stack.isEmpty()) {
            if (stack.peek() == '-') {
                // A - B == A + (-B)
                vmw.writeArithmetic('-');
                vmw.writeArithmetic('+');
                stack.pop();
            }
            else vmw.writeArithmetic(stack.pop());
        }
    }

    public void compileTerm() throws IOException {
        switch (jt.tokenType()) {
            case integerConstant -> {
                vmw.writePush("int", jt.intVal());
                jt.advance();
            }
            case stringConstant -> {
                vmw.writePush("int", jt.stringVal().length());
                vmw.writeCall("String.new", 1);
                for (char c : jt.stringVal().toCharArray()) {
                    vmw.writePush("int", c);
                    vmw.writeCall("String.appendChar", 2);
                }
                jt.advance();
            }
            case keyword -> {
                switch (jt.KeyWord()) {
                    case "true" -> {
                        vmw.writePush("int", 1);
                        vmw.writeArithmetic('-');
                        jt.advance();
                    }
                    case "null", "false" -> {
                        vmw.writePush("int", 0);
                        jt.advance();
                    }
                    case "this" -> {
                        vmw.writePush("pointer" , 0);
                        jt.advance();
                    }
                    default -> throw new UnsupportedOperationException(STR."write \{jt.KeyWord()} not implemented.");
                }
            }
            case symbol -> {
                // parentheses
                if (jt.symbol() == '(') {
                    jt.advance();
                    compileExpression();
                    jt.advance();
                }
                // unary ops
                else if (jt.symbol() == '-' || jt.symbol() == '~') {
                    char op = (fetchAndAdvance(JackTokenizer.tokenTypes.symbol).charAt(0));
                    compileTerm();
                    vmw.writeArithmetic(op);
                }
            }
            case identifier -> lookAhead();
        }
    }

    private boolean isDefined(String varName) {
        return subTable.kindOf(varName) != SymbolTable.Kind.NONE || classTable.kindOf(varName) != SymbolTable.Kind.NONE;
    }

    private SymbolTable lookUp(String varName) {
        if (subTable.kindOf(varName) != SymbolTable.Kind.NONE) return subTable;
        else if (classTable.kindOf(varName) != SymbolTable.Kind.NONE) return classTable;
        else throw new NoSuchElementException(STR."\{varName} is not defined.");
    }

    private void lookAhead() throws IOException {
        String identifier = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
        if (jt.tokenType() != JackTokenizer.tokenTypes.symbol) {
            throw new RuntimeException(STR."lookAhead for \{jt.KeyWord()} not supported.");
        }
        switch (jt.symbol()) {
            // array index
            case '[' -> {
                pushVar(identifier);
                jt.advance();
                compileExpression();
                vmw.writeArithmetic('+');
                vmw.writePop("pointer", 1);
                vmw.writePush("that", 0);
                jt.advance();
            }
            // method calls
            case '(' -> {
                jt.advance();
                vmw.writePush("pointer", 0);
                vmw.writeCall(STR."\{className}.\{identifier}", compileExpressionList() + 1);
                // )
                jt.advance();
            }
            // subroutine calls
            case '.' -> {
                // . identifier (
                jt.advance();
                String method = fetchAndAdvance(JackTokenizer.tokenTypes.identifier);
                jt.advance();
                // identifier is an object, calling a method
                if (isDefined(identifier)) {
                    SymbolTable table = lookUp(identifier);
                    vmw.writePush(table.kindOf(identifier).name(), table.indexOf(identifier));
                    vmw.writeCall(STR."\{table.typeOf(identifier)}.\{method}", compileExpressionList() + 1);
                } // identifier is a class, calling a function
                else vmw.writeCall(STR."\{identifier}.\{method}", compileExpressionList());
                // )
                jt.advance();
            }
            default -> pushVar(identifier);
        }
    }

    public int compileExpressionList() throws IOException {
        int counter = 0;
        while (!isSymbol(')')) {
            if (isSymbol(',')) {
                jt.advance();
                continue;
            }
            compileExpression();
            counter++;
        }
        return counter;
    }
}
