public class Code {
    public static String dest(String d) {
        return switch (d) {
            case "M" -> "001";
            case "D" -> "010";
            case "DM", "MD" -> "011";
            case "A" -> "100";
            case "AM", "MA" -> "101";
            case "AD", "DA" -> "110";
            case "ADM", "AMD", "DAM", "DMA", "MAD", "MDA" -> "111";
            default -> "000";
        };
    }

    public static String jump(String j) {
        return switch (j) {
            case "JGT" -> "001";
            case "JEQ" -> "010";
            case "JGE" -> "011";
            case "JLT" -> "100";
            case "JNE" -> "101";
            case "JLE" -> "110";
            case "JMP" -> "111";
            default -> "000";
        };
    }

    public static String comp(String c) {
        return switch (c) {
            case "0" -> "0101010";
            case "1" -> "0111111";
            case "-1" -> "0111010";
            case "D" -> "0001100";
            case "A" -> "0110000";
            case "M" -> "1110000";
            case "!D" -> "0001101";
            case "!A" -> "0110001";
            case "!M" -> "1110001";
            case "-D" -> "0001111";
            case "-A" -> "0110011";
            case "-M" -> "1110011";
            case "D+1" -> "0011111";
            case "A+1" -> "0110111";
            case "M+1" -> "1110111";
            case "D-1" -> "0001110";
            case "A-1" -> "0110010";
            case "M-1" -> "1110010";
            case "D+A", "A+D" -> "0000010";
            case "D+M", "M+D" -> "1000010";
            case "D-A" -> "0010011";
            case "D-M" -> "1010011";
            case "A-D" -> "0000111";
            case "M-D" -> "1000111";
            case "D&A", "A&D" -> "0000000";
            case "D&M", "M&D" -> "1000000";
            case "D|A", "A|D" -> "0010101";
            case "D|M", "M|D" -> "1010101";
            default -> "";
        };
    }

    public static void main(String[] args) {
        System.out.println(Code.jump(""));
        System.out.println(Code.dest(""));
        System.out.println(Code.comp("0"));
    }
}
