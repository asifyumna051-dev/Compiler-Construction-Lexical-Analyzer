import java.io.*;
import java.util.*;

public class LexicalAnalyzer {

    // C language keywords
    static Set<String> keywords = new HashSet<>(Arrays.asList(
            "auto", "break", "case", "char", "const", "continue",
            "default", "do", "double", "else", "enum", "extern",
            "float", "for", "goto", "if", "int", "long",
            "register", "return", "short", "signed", "sizeof",
            "static", "struct", "switch", "typedef", "union",
            "unsigned", "void", "volatile", "while"
    ));

    // Operators allowed in C
    static Set<String> operators = new HashSet<>(Arrays.asList(
            "+", "-", "*", "/", "%", "=",
            "==", "!=", "<", ">", "<=", ">=",
            "&&", "||", "!", "++", "--",
            "+=", "-=", "*=", "/=", "%="
    ));

    // Symbols / separators
    static Set<Character> symbols = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', '[', ']',
            ';', ',', ':', '.'
    ));

    static void writeToken(BufferedWriter writer,
                           String token,
                           String lexeme,
                           int line) throws IOException {

        writer.write(String.format("%-15s %-25s %d",
                token, lexeme, line));
        writer.newLine();
    }

    public static void main(String[] args) {

        String inputFile = "input.txt";
        String outputFile = "output.txt";

        try (
                BufferedReader reader = new BufferedReader(
                        new FileReader(inputFile));

                BufferedWriter writer = new BufferedWriter(
                        new FileWriter(outputFile))
        ) {

            writer.write(String.format("%-15s %-25s %s",
                    "Token", "Lexeme", "Line No"));
            writer.newLine();

            writer.write("-----------------------------------------------");
            writer.newLine();

            String line;
            int lineNumber = 0;
            boolean insideComment = false;

            while ((line = reader.readLine()) != null) {

                lineNumber++;
                int i = 0;
                boolean skipLine = false;

                while (i < line.length()) {

                    char ch = line.charAt(i);

                    if (insideComment) {

                        int endComment = line.indexOf("*/", i);

                        if (endComment != -1) {
                            insideComment = false;
                            i = endComment + 2;
                        } else {
                            i = line.length();
                        }

                        continue;
                    }

                    
                    if (Character.isWhitespace(ch)) {
                        i++;
                        continue;
                    }

                    
                    if (ch == '/' &&
                            i + 1 < line.length() &&
                            line.charAt(i + 1) == '*') {

                        insideComment = true;
                        i += 2;
                        continue;
                    }

                    if (ch == '"') {

                        int start = i;
                        i++;

                        boolean stringClosed = false;

                        while (i < line.length()) {

                            if (line.charAt(i) == '"') {
                                stringClosed = true;
                                i++;
                                break;
                            }

                            i++;
                        }

                        if (!stringClosed) {

                            writer.write("ERROR: String constant exceeds line at line "
                                    + lineNumber);
                            writer.newLine();

                            skipLine = true;
                            break;
                        }

                        String lexeme = line.substring(start, i);

                        writeToken(writer,
                                "STRING",
                                lexeme,
                                lineNumber);

                        continue;
                    }

                 
                    if (ch == '\'') {

                        int start = i;
                        i++;

                        int charCount = 0;
                        boolean closed = false;

                        while (i < line.length()) {

                            if (line.charAt(i) == '\'') {
                                closed = true;
                                i++;
                                break;
                            }

                            charCount++;
                            i++;
                        }

                        if (!closed || charCount != 1) {

                            writer.write("ERROR: Char constant too long at line "
                                    + lineNumber);
                            writer.newLine();

                            skipLine = true;
                            break;
                        }

                        String lexeme = line.substring(start, i);

                        writeToken(writer,
                                "CHAR",
                                lexeme,
                                lineNumber);

                        continue;
                    }

                   
                    if (Character.isLetter(ch) || ch == '_') {

                        int start = i;

                        i++;

                        while (i < line.length()) {

                            char next = line.charAt(i);

                            if (Character.isLetterOrDigit(next)
                                    || next == '_') {
                                i++;
                            } else {
                                break;
                            }
                        }

                        String lexeme = line.substring(start, i);

                        if (keywords.contains(lexeme)) {

                            writeToken(writer,
                                    "KEYWORD",
                                    lexeme,
                                    lineNumber);

                        } else {

                            writeToken(writer,
                                    "ID",
                                    lexeme,
                                    lineNumber);
                        }

                        continue;
                    }

                  
                    if (Character.isDigit(ch)) {

                        int start = i;

                        i++;

                        while (i < line.length()
                                && Character.isDigit(line.charAt(i))) {
                            i++;
                        }

                        String lexeme = line.substring(start, i);

                        writeToken(writer,
                                "INTEGER",
                                lexeme,
                                lineNumber);

                        continue;
                    }

            
                    if (i + 1 < line.length()) {

                        String twoChars =
                                line.substring(i, i + 2);

                        if (operators.contains(twoChars)) {

                            writeToken(writer,
                                    "OPERATOR",
                                    twoChars,
                                    lineNumber);

                            i += 2;
                            continue;
                        }
                    }

                   
                    String oneChar = String.valueOf(ch);

                    if (operators.contains(oneChar)) {

                        writeToken(writer,
                                "OPERATOR",
                                oneChar,
                                lineNumber);

                        i++;
                        continue;
                    }

                    
                    if (symbols.contains(ch)) {

                        writeToken(writer,
                                "SYMBOL",
                                String.valueOf(ch),
                                lineNumber);

                        i++;
                        continue;
                    }

                    
                    writer.write("ERROR: Undefined symbol "
                            + ch
                            + " at line "
                            + lineNumber);

                    writer.newLine();

                    skipLine = true;
                    break;
                }

                if (skipLine) {
                    continue;
                }
            }

            
            if (insideComment) {

                writer.write("ERROR: Un-terminated comment");
                writer.newLine();
            }

            System.out.println(
                    "Lexical analysis completed successfully.");

            System.out.println(
                    "Output generated in output.txt");

        } catch (FileNotFoundException e) {

            System.out.println(
                    "Error: input.txt not found.");

        } catch (IOException e) {

            System.out.println(
                    "File error: " + e.getMessage());
        }
    }
}
