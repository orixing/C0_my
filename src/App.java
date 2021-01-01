
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import analyser.Analyser;
import error.CompileError;
import instruction.Instruction;
import tokenizer.StringIter;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;

public class App {
    public static void main(String[] args) throws CompileError, IOException {
        File input = new File(args[0]);
        File output = new File(args[1]);
        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);
        var analyzer = analyze(tokenizer);
        analyzer.analyse(output);
    }
       

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
    private static Analyser analyze(Tokenizer tokenizer) {
        var analyzer = new Analyser(tokenizer);
        return analyzer;
    }
}

