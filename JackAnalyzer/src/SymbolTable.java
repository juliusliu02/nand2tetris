import java.util.HashMap;
import java.util.NoSuchElementException;

public class SymbolTable {

    public enum Kind { STATIC, FIELD, ARG, VAR, NONE }
    private int staticCount;
    private int fieldCount;
    private int argCount;
    private int varCount;

    static class Entry {
        String type;
        Kind kind;
        int index;
        public Entry(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
        public String toString() {
            return STR."type: \{type}, kind: \{kind}, index: \{index}";
        }
    }

    private final HashMap<String, Entry> table;

    public SymbolTable() {
        table = new HashMap<>();
    }

    public void define(Kind kind, String type, String name) {
        table.put(name, new Entry(type, kind, varCount(kind)));
        switch (kind) {
            case STATIC -> staticCount++;
            case FIELD -> fieldCount++;
            case ARG -> argCount++;
            case VAR -> varCount++;
            default -> throw new IllegalArgumentException("Kind is not valid.");
        }
    }

    public int varCount(Kind kind) {
        switch (kind) {
            case STATIC -> {
                return staticCount;
            }
            case FIELD -> {
                return fieldCount;
            }
            case ARG -> {
                return argCount;
            }
            case VAR -> {
                return varCount;
            }
            default -> throw new IllegalArgumentException("Kind is not valid.");
        }
    }

    public Kind kindOf(String name) {
        return table.containsKey(name) ? table.get(name).kind : Kind.NONE;
    }

    public String typeOf(String name) {
        if (table.containsKey(name)) {
            return table.get(name).type;
        }
        throw new NoSuchElementException(STR."\{name} is not defined.");
    }

    public int indexOf(String name) {
        if (table.containsKey(name)) {
            return table.get(name).index;
        }
        throw new NoSuchElementException(STR."\{name} is not defined.");
    }

    public String toString() {
        return table.isEmpty() ? "EMPTY" : table.toString();
    }
}
