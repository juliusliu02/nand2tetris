import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Integer> st;

    public SymbolTable() {
        st = new HashMap<>();
        for (int i = 0; i < 16; i++) {
            st.put("R" + i, i);
        }
        st.put("SP", 0);
        st.put("LCL", 1);
        st.put("ARG", 2);
        st.put("THIS", 3);
        st.put("THAT", 4);
        st.put("SCREEN", 16384);
        st.put("KBD", 24576);
    }

    public void addEntry(String symbol, int address) {
        st.put(symbol, address);
    }

    public boolean contains(String symbol) {
        return st.containsKey(symbol);
    }

    public int getAddress(String symbol) {
        return st.get(symbol);
    }
}
