import java.io.*;
import java.util.*;

public class SyntaxAnalyzer {

    static class Token {
        String type, lexeme;
        int line;

        Token(String type, String lexeme, int line) {
            this.type = type;
            this.lexeme = lexeme;
            this.line = line;
        }
    }

    static ArrayList<Token> tokens = new ArrayList<>();
    static ArrayList<String[]> symbolTable = new ArrayList<>();
    static ArrayList<String> errors = new ArrayList<>();

    static int index = 0;
    static String scope = "Global";

    static Token current() {
        if (index < tokens.size())
            return tokens.get(index);

        return new Token("END", "END",
                tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).line);
    }

    static boolean check(String lexeme) {
        return current().lexeme.equalsIgnoreCase(lexeme);
    }

    static Token advance() {
        Token t = current();
        if (index < tokens.size())
            index++;
        return t;
    }

    static void error(String message) {
        errors.add("Line " + current().line + ": " + message);
        int errorLine = current().line;

        while (index < tokens.size()
                && current().line == errorLine) {
            index++;
        }
    }

    static boolean match(String lexeme) {
        if (check(lexeme)) {
            advance();
            return true;
        }
        return false;
    }

    static boolean isType() {
        return check("int") || check("char")
                || check("void");
    }

    static void expect(String lexeme) {
        if (!match(lexeme)) {
            throw new RuntimeException(
                    "Expected '" + lexeme + "'");
        }
    }

    static void addSymbol(String name, String type, String arraySize) {
        symbolTable.add(new String[]{
                name, type, scope, arraySize
        });
    }

    // Read tokens from lexical analyzer output
    static void readTokens() throws Exception {

        BufferedReader br = new BufferedReader(
                new FileReader("output.txt"));

        String line;

        while ((line = br.readLine()) != null) {

            String[] parts = line.trim().split("\\s+");

            if (parts.length < 3)
                continue;

            if (parts[0].equals("Token")
                    || parts[0].startsWith("-")
                    || parts[0].equals("ERROR:"))
                continue;

            try {
                String type = parts[0];
                int lineNo = Integer.parseInt(parts[parts.length - 1]);

                StringBuilder lexeme = new StringBuilder();

                for (int i = 1; i < parts.length - 1; i++) {
                    if (i > 1) lexeme.append(" ");
                    lexeme.append(parts[i]);
                }

                tokens.add(new Token(
                        type, lexeme.toString(), lineNo));

            } catch (Exception e) {
                // Ignore invalid token rows
            }
        }

        br.close();
    }

    // Declaration:
    // type identifier [, identifier] ;
    static void declaration() {

        String type = advance().lexeme;

        while (true) {

            if (!current().type.equals("ID")) {
                throw new RuntimeException(
                        "Identifier expected");
            }

            String name = advance().lexeme;
            String size = "";

            if (match("[")) {

                if (current().type.equals("INTEGER")) {
                    size = advance().lexeme;
                } else {
                    size = "0";
                }

                expect("]");
            }

            addSymbol(name, type, size);

            if (!match(","))
                break;
        }

        expect(";");
    }

    // Expression:
    // identifier operator identifier/constant
   
static void expression() {

    // First operand can be ID or constant
    if (current().type.equals("ID")
            || current().type.equals("INTEGER")
            || current().type.equals("CHAR")) {

        advance();

    } else {
        throw new RuntimeException(
                "Identifier or constant expected");
    }

    // Check operators and next operands
    while (isOperator(current().lexeme)) {

        advance();

        if (current().type.equals("ID")
                || current().type.equals("INTEGER")
                || current().type.equals("CHAR")) {

            advance();

        } else {
            throw new RuntimeException(
                    "Identifier or constant expected");
        }
    }
}

    static boolean isOperator(String op) {
        return Arrays.asList(
                "+", "-", "*", "/", ">", "<", ">=", "<=", "=="
        ).contains(op);
    }

    // Assignment statement
    static void assignment() {
        if (!current().type.equals("ID")) {
            throw new RuntimeException(
                    "Identifier expected");
        }

        advance();
        expect("=");

        expression();
        expect(";");
    }

    // Statement
    static void statement() {

        if (check("{")) {
            compoundStatement();
        }

        else if (check("if")) {
            selectionStatement();
        }

        else if (check("while")) {
            iterationStatement();
        }

        else if (check("for")) {
            forStatement();
        }

        else if (current().type.equals("ID")) {
            assignment();
        }

        else if (match(";")) {
            // Empty statement
        }

        else {
            throw new RuntimeException(
                    "Invalid statement");
        }
    }

    // Compound statement
    static void compoundStatement() {

        expect("{");

        while (!check("}") && !check("END")) {

            int oldIndex = index;

            try {
                if (isType() && !check("void")) {
                    declaration();
                } else {
                    statement();
                }

            } catch (RuntimeException e) {
                error(e.getMessage());
            }

            if (oldIndex == index)
                advance();
        }

        expect("}");
    }

    // If / Else
    static void selectionStatement() {

        expect("if");
        expect("(");

        expression();

        expect(")");
        statement();

        if (match("else")) {
            statement();
        }
    }

    // While
    static void iterationStatement() {

        expect("while");
        expect("(");

        expression();

        expect(")");
        statement();
    }

    // For
    static void forStatement() {

        expect("for");
        expect("(");

        expression();
        expect(";");

        expression();
        expect(";");

        expression();

        expect(")");
        statement();
    }

    // Main function
    static void functionDefinition() {

        if (!isType()) {
            throw new RuntimeException(
                    "Function type expected");
        }

        advance();

        if (!check("main")) {
            throw new RuntimeException(
                    "main function expected");
        }

        addSymbol("main", "Function", "");
        advance();

        expect("(");

        if (isType()) {
            String parameterType = advance().lexeme;

            if (current().type.equals("ID")) {
                String parameterName = advance().lexeme;

                scope = "main";
                addSymbol(parameterName, parameterType, "");
            }
        }

        expect(")");

        scope = "main";

        compoundStatement();
    }

    // Start parsing
    static void parse() {

        while (index < tokens.size()) {

            int oldIndex = index;

            try {
                if (check("main")
                        || isType()) {
                    functionDefinition();
                } else if (isType()) {
                    declaration();
                } else {
                    throw new RuntimeException(
                            "Invalid declaration or function");
                }

            } catch (RuntimeException e) {
                error(e.getMessage());
            }

            if (oldIndex == index)
                advance();
        }
    }

    // Save Symbol Table
   
static void saveSymbolTable() throws Exception {

    PrintWriter out = new PrintWriter("symbol_table.txt");

    // Table heading
    out.printf("%-15s %-15s %-15s %-12s%n",
            "Lexeme", "Type", "Scope", "Array Size");

    out.println("----------------------------------------------------------");

    // Print symbol table rows
    for (String[] row : symbolTable) {

        out.printf("%-15s %-15s %-15s %-12s%n",
                row[0],
                row[1],
                row[2],
                row[3].isEmpty() ? "-" : row[3]);
    }

    out.close();
}

    // Save Syntax Errors
    static void saveErrors() throws Exception {

        PrintWriter out = new PrintWriter(
                "syntax_errors.txt");

        if (errors.isEmpty()) {
            out.println("No Syntax Errors Found.");
        } else {
            for (String e : errors)
                out.println(e);
        }

        out.close();
    }

    public static void main(String[] args) throws Exception {

        readTokens();

        parse();

        saveSymbolTable();
        saveErrors();

        System.out.println("Syntax Analysis Completed!");

        System.out.println(
                "Symbol Table generated successfully.");

        System.out.println(
                "Syntax Errors: " + errors.size());
    }
}
