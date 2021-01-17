
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
    public static void main(String[] args) throws Exception{
        Scanner sc=new Scanner(new File(args[0]));
        StringIter it=new StringIter(sc);
        Tokenizer tokenizer=new Tokenizer(it);
        Analyser analyser=new Analyser(tokenizer);
        analyser.analyse(args[1]);
    }
}

