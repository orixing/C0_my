package analyser;

import error.*;
import instruction.*;
import symbol.*;
import tokenizer.*;
import util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Analyser {
    Tokenizer tokenizer;
    Token peekedToken = null;

    ArrayList<TokenType> terminals = new ArrayList<>(Arrays.asList(TokenType.GT,TokenType.LT, TokenType.GE, TokenType.LE, TokenType.EQ, TokenType.NEQ,TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.AS_KW));
    public boolean[][] map = {
        // >    <       >=  <=      ==  !=      +       -       *   /       as
        { true, true, true, true, true, true, false, false, false, false, false }, // >
        { true, true, true, true, true, true, false, false, false, false, false }, // <
        { true, true, true, true, true, true, false, false, false, false, false }, // >=
        { true, true, true, true, true, true, false, false, false, false, false }, // <=
        { true, true, true, true, true, true, false, false, false, false, false }, // ==
        { true, true, true, true, true, true, false, false, false, false, false }, // !=
        { true, true, true, true, true, true, true, true, false, false, false }, // +
        { true, true, true, true, true, true, true, true, false, false, false }, // -
        { true, true, true, true, true, true, true, true, true, true, false }, // *
        { true, true, true, true, true, true, true, true, true, true, false }, // /
        { true, true, true, true, true, true, true, true, true, true, true } };// as
    public static ArrayList<ArrayList<Instruction>> outputfunctions = new ArrayList<>();
    Stack<SymbolEntry> symbolTable = new Stack<>();
    Stack<Integer> index = new Stack<>();
    HashMap<String, Integer> hash = new HashMap<>();
    ArrayList<Instruction> instructions;
    ArrayList<Instruction> start;
    ArrayList<String> Globals;
    int globalOffset = 0;
    int paramOffset = 0;
    int localOffset = 0;
    int funcOffset = 1;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
        this.start = new ArrayList<>();
        this.Globals = new ArrayList<>();
        this.index.push(0);
    }

    public void analyse(FileOutputStream output) throws CompileError, IOException {
        analyseProgram();
        output(output);
    }

    /**
     * 查看下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     *
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        Token token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        Token token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        Token token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param initflag 是否已赋值
     * @param constflag    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean constflag, boolean initflag, SymbolType symbolType, SymbolRange symbolrange, Pos curPos) throws AnalyzeError {
        Integer addr = this.hash.get(name);
        if (addr != null && addr >= this.index.peek()) 
        {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } 
        else {
            if (addr != null) 
            {
                switch (symbolrange) {
                    case global:
                        this.symbolTable.push(new SymbolEntry(name, constflag, initflag, symbolType, addr, symbolrange, globalOffset++));
                        if (constflag)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case param:
                        this.symbolTable.push(new SymbolEntry(name, constflag, initflag, symbolType, addr, symbolrange, paramOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new SymbolEntry(name, constflag, initflag, symbolType, addr, symbolrange, localOffset++));
                        break;
                }
            } else {
                switch (symbolrange) {
                    case global:
                        this.symbolTable.push(new SymbolEntry(name, constflag, initflag, symbolType, symbolrange, globalOffset++));
                        if (constflag)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case param:
                        this.symbolTable.push(new SymbolEntry(name, constflag, initflag, symbolType, symbolrange, paramOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new SymbolEntry(name, constflag, initflag, symbolType, symbolrange, localOffset++));
                        break;
                }
            }
            this.hash.put(name, symbolTable.size() - 1);
        }
    }

    private void analyseProgram() throws CompileError {
        Globals.add("_start");
        start.add(new FunctionEntry(Operation.func, 0, 0, 0, globalOffset++));
        while (check(TokenType.FN_KW) || check(TokenType.LET_KW) || check(TokenType.CONST_KW)) {
            if (check(TokenType.FN_KW))
            {
                analyseFunction();
            }
            else
            {
                if (check(TokenType.CONST_KW))
                {
                    analyse_Const_Decl_Stmt(SymbolRange.global);
                }
                else
                {
                    analyse_Let_Decl_Stmt(SymbolRange.global);
                }
            }
        }
        expect(TokenType.EOF);
        start.add(new Instruction(Operation.stackalloc, 0));
        start.add(new Instruction(Operation.call, this.symbolTable.get(this.hash.get("main")).funcOffset));
    }

    
    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);
        Token nameToken = expect(TokenType.IDENT);
        SymbolEntry funcSymbol;
        String name = nameToken.getValueString();
        Integer location = this.hash.get(name);
        if (location != null && location >= this.index.peek()) 
        {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration);
        } 
        else 
        {
            funcSymbol = new SymbolEntry(name, true, SymbolRange.global, globalOffset++, funcOffset++);
            this.symbolTable.push(funcSymbol);
            this.hash.put(name, symbolTable.size() - 1);
            this.index.push(symbolTable.size());
            Globals.add(name);
        }
        localOffset = 0;
        FunctionEntry func = new FunctionEntry(Operation.func);
        instructions.add(func);
        expect(TokenType.L_PAREN);
        if (check(TokenType.IDENT))
        {
            do {
                analyse_function_param(funcSymbol.params);
            } while (nextIf(TokenType.COMMA) != null);
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        SymbolType type;
        if (peek().getTokenType()==TokenType.INT_KW)
        {
            next();
            type=SymbolType.INT;
        }
        else if (peek().getTokenType()==TokenType.DOUBLE_KW)
        {
            next();
            type=SymbolType.DOUBLE;
        }
        else if (peek().getTokenType()==TokenType.VOID_KW)
        {
            next();
            type=SymbolType.VOID;
        }
        else 
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        }
        funcSymbol.symbolType=type;
        func.paramnum=paramOffset;
        if (type == SymbolType.VOID)
        {
            func.returnnum=0;
        }
        else 
        {
            func.returnnum=1;
            int last = symbolTable.size() - 1;
            for (int i = 0; i < paramOffset; i++) 
            {
                SymbolEntry symbol = this.symbolTable.get(last - i);
                symbol.offset=symbol.offset + 1;
            }
        }
        func.offset=funcSymbol.offset;
        boolean[] b = analyse_block_stmt(true, false, type, 0, null);
        if (type != SymbolType.VOID && b[0]==false) 
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, nameToken.getStartPos());
        }
        if (type == SymbolType.VOID && b[0]==false)
        {
            instructions.add(new Instruction(Operation.ret));
        }
        func.localnum=localOffset;
    }

    private void analyse_function_param(ArrayList<SymbolType> params) throws CompileError 
    {
        boolean constflag = false;
        if (nextIf(TokenType.CONST_KW) != null)
            constflag = true;
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type;
        if (peek().getTokenType()==TokenType.INT_KW)
        {
            next();
            type=SymbolType.INT;
        }
        else if (peek().getTokenType()==TokenType.DOUBLE_KW)
        {
            next();
            type=SymbolType.DOUBLE;
        }
        else if (peek().getTokenType()==TokenType.VOID_KW)
        {
            next();
            type=SymbolType.VOID;
        }
        else 
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        }
        addSymbol(nameToken.getValueString(), constflag, true, type, SymbolRange.param, nameToken.getStartPos());
        params.add(type);
    }

    private boolean[] analyse_block_stmt(boolean funcflag, boolean loopflag, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        boolean returnflag = false;
        boolean breakcontinueflag = false;
        int returnnums = 0;
        int breakcontinuenums = 0;
        expect(TokenType.L_BRACE);
        if (!funcflag)
        {
            this.index.push(this.symbolTable.size());
        }
        while (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN) || check(TokenType.LET_KW) || check(TokenType.CONST_KW) || check(TokenType.IF_KW) || check(TokenType.WHILE_KW) || check(TokenType.BREAK_KW) || check(TokenType.CONTINUE_KW) || check(TokenType.RETURN_KW) || check(TokenType.SEMICOLON) || check(TokenType.L_BRACE)) {
            if (returnnums == 0 && returnflag)
            {
                returnnums = instructions.size();
            }
            else if (breakcontinuenums == 0 && breakcontinueflag)
            {
                breakcontinuenums = instructions.size();
            }
            if (returnflag==true && breakcontinueflag==true)
            {
                analyseStmt(loopflag, returnType, loopLoc, breakList);
            }
            else if (returnflag==true)
            {
                boolean[] b = analyseStmt(loopflag, returnType, loopLoc, breakList);
                breakcontinueflag = b[1];
            }
            else if (breakcontinueflag==true)
            {
                boolean[] b = analyseStmt(loopflag, returnType, loopLoc, breakList);
                returnflag = b[0];
            }
            else 
            {
                boolean[] b = analyseStmt(loopflag, returnType, loopLoc, breakList);
                returnflag = b[0];
                breakcontinueflag = b[1];
            }
        }
        expect(TokenType.R_BRACE);
        if (returnnums > 0)
        {
            instructions.subList(returnnums, instructions.size()).clear();
        }
        if (breakcontinuenums > 0)
        {
            instructions.subList(breakcontinuenums, instructions.size()).clear();
        }
        int endIndex = index.pop();
        for (int i = symbolTable.size(); i > endIndex; i--) 
        {
            SymbolEntry tmpSymbol = symbolTable.pop();
            if (tmpSymbol.lastaddr == -1) 
            {
                hash.remove(tmpSymbol.name);
                System.out.println();
            } 
            else 
            {
                hash.put(tmpSymbol.name, tmpSymbol.lastaddr);
            }
        }
        if (funcflag!=false)
        {
            paramOffset = 0;
        }
        boolean B[] = {returnflag, breakcontinueflag};
        return B;
    }

    private boolean[] analyseStmt(boolean loopflag, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        if (check(TokenType.CONST_KW) || check(TokenType.LET_KW))
        {
            if (check(TokenType.CONST_KW))
            {
                analyse_Const_Decl_Stmt(SymbolRange.local);
            }
            else
            {
                analyse_Let_Decl_Stmt(SymbolRange.local);
            }
        }
        else if (check(TokenType.IF_KW))
        {
            expect(TokenType.IF_KW);
            boolean returnflag;
            boolean breakcontinueflag;
            boolean elseflag = false;
            ArrayList<Integer> jumpaddrlist = new ArrayList<>();
            SymbolType t = analysebasicexpr(false);
            if (t == SymbolType.VOID)
            {
                throw new AnalyzeError(ErrorCode.InvalidInput);
            }
            instructions.add(new Instruction(Operation.brtrue, 1));
            instructions.add(new Instruction(Operation.br));
            int jumpaddr = instructions.size() - 1;
            boolean[] b = analyse_block_stmt(false, loopflag, returnType, loopLoc, breakList);
            returnflag = b[0];
            breakcontinueflag = b[1];
            jumpaddrlist.add(instructions.size());
            instructions.add(new Instruction(Operation.br));
            instructions.get(jumpaddr).setParam1(instructions.size() - jumpaddr - 1);
            if (check(TokenType.ELSE_KW)) 
            {
                while (nextIf(TokenType.ELSE_KW) != null) 
                {
                    if (nextIf(TokenType.IF_KW) != null) 
                    {
                        t = analysebasicexpr(false);
                        if (t == SymbolType.VOID)
                        {
                            throw new AnalyzeError(ErrorCode.InvalidInput);
                        }
                        instructions.add(new Instruction(Operation.brtrue, 1));
                        instructions.add(new Instruction(Operation.br));
                        jumpaddr = instructions.size() - 1;
                        b = analyse_block_stmt(false, loopflag, returnType, loopLoc, breakList);
                        returnflag &= b[0];
                        breakcontinueflag &= b[1];
                        jumpaddrlist.add(instructions.size());
                        instructions.add(new Instruction(Operation.br));
                        instructions.get(jumpaddr).setParam1(instructions.size() - jumpaddr - 1);
                    } 
                    else 
                    {
                        b = analyse_block_stmt(false, loopflag, returnType, loopLoc, breakList);
                        returnflag &= b[0];
                        breakcontinueflag &= b[1];
                        elseflag = true;
                        break;
                    }
                }
            }
            if (elseflag==false) 
            {
                returnflag = false;
                breakcontinueflag = false;
            }
            for (Integer i : jumpaddrlist) 
            {
                instructions.get(i).setParam1(instructions.size()-i-1);
            }
            return new boolean[]{returnflag, breakcontinueflag};
        }
        else if (check(TokenType.WHILE_KW))
        {
            expect(TokenType.WHILE_KW);
            ArrayList<Integer> breaks = new ArrayList<>();
            int loopaddr = instructions.size() - 1;
            analysebasicexpr(false);
            instructions.add(new Instruction(Operation.brtrue, 1));
            int jumpaddr = instructions.size();
            instructions.add(new Instruction(Operation.br));
            boolean breakcontinueflag = analyse_block_stmt(false, true, returnType, loopaddr, breakList)[1];
            if (breakcontinueflag==false)
            {
                instructions.add(new Instruction(Operation.br, loopaddr - instructions.size()));
            }
            instructions.get(jumpaddr).setParam1(instructions.size() - jumpaddr - 1);
            for (int i : breaks) 
            {
                instructions.get(i).setParam1(instructions.size()-i-1);
            }
        }
        else if (check(TokenType.BREAK_KW)) 
        {
            if (loopflag==true)
            {
                expect(TokenType.BREAK_KW);
                expect(TokenType.SEMICOLON);
                breakList.add(instructions.size());
                instructions.add(new Instruction(Operation.br));
            }
            else
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
            }
            return new boolean[]{false, true};
        } 
        else if (check(TokenType.CONTINUE_KW)) 
        {
            if (loopflag)
            {
                expect(TokenType.CONTINUE_KW);
                expect(TokenType.SEMICOLON);
                instructions.add(new Instruction(Operation.br, loopLoc - instructions.size()));
            }
            else
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
            }
            return new boolean[]{false, true};
        } 
        else if (check(TokenType.RETURN_KW)) 
        {
            Token expect = expect(TokenType.RETURN_KW);
            if (returnType != SymbolType.VOID)
            {
                instructions.add(new Instruction(Operation.arga, 0));
            }
            SymbolType type = SymbolType.VOID;
            if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN)) 
            {
                SymbolType t = analysebasicexpr(false);
                type = t;
            }
            expect(TokenType.SEMICOLON);
            if (type != returnType)
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, expect.getStartPos());
            }
            if (type != SymbolType.VOID)
            {
                instructions.add(new Instruction(Operation.store64));
            }
            instructions.add(new Instruction(Operation.ret));
            return new boolean[]{true, false};
        } 
        else if (check(TokenType.L_BRACE))
        {
            return analyse_block_stmt(false, loopflag, returnType, loopLoc, breakList);
        }
        else if (check(TokenType.SEMICOLON))
        {
            expect(TokenType.SEMICOLON);
        }
        else
        {
            SymbolType t = analysebasicexpr(false);
            expect(TokenType.SEMICOLON);
            if (t != SymbolType.VOID)
            {
                instructions.add(new Instruction(Operation.pop));
            }
        }
        return new boolean[]{false, false};
    }


    private void analyse_Const_Decl_Stmt(SymbolRange symbolrange) throws CompileError {
        boolean globalflag = symbolrange == SymbolRange.global;
        expect(TokenType.CONST_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type;
        if (peek().getTokenType()==TokenType.INT_KW)
        {
            next();
            type=SymbolType.INT;
        }
        else if (peek().getTokenType()==TokenType.DOUBLE_KW)
        {
            next();
            type=SymbolType.DOUBLE;
        }
        else if (peek().getTokenType()==TokenType.VOID_KW)
        {
            next();
            type=SymbolType.VOID;
        }
        else 
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        }
        if (type == SymbolType.VOID)
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, nameToken.getStartPos());
        }
        addSymbol(nameToken.getValueString(), true, true, type, symbolrange, nameToken.getStartPos());
        SymbolEntry symbol = this.symbolTable.peek();
        expect(TokenType.ASSIGN);
        if (globalflag==true)
        {
            start.add(new Instruction(Operation.globa, symbol.offset));
        }
        else
        {
            instructions.add(new Instruction(Operation.loca, symbol.offset));
        }
        SymbolType t = analysebasicexpr(globalflag);
        if (type != t)
        {
            throw new AnalyzeError(ErrorCode.InvalidInput);
        }
        expect(TokenType.SEMICOLON);
        if (globalflag==true)
        {
            start.add(new Instruction(Operation.store64));
        }
        else
        {
            instructions.add(new Instruction(Operation.store64));
        }
    }

    private void analyse_Let_Decl_Stmt(SymbolRange symbolrange) throws CompileError {
        boolean globalflag = symbolrange == SymbolRange.global;
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type;
        if (peek().getTokenType()==TokenType.INT_KW)
        {
            next();
            type=SymbolType.INT;
        }
        else if (peek().getTokenType()==TokenType.DOUBLE_KW)
        {
            next();
            type=SymbolType.DOUBLE;
        }
        else if (peek().getTokenType()==TokenType.VOID_KW)
        {
            next();
            type=SymbolType.VOID;
        }
        else 
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        }
        if (type == SymbolType.VOID)
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, nameToken.getStartPos());
        }
        addSymbol(nameToken.getValueString(), false, false, type, symbolrange, nameToken.getStartPos());
        SymbolEntry symbol = this.symbolTable.peek();
        if (nextIf(TokenType.ASSIGN) != null)
        {

            if (globalflag)
            {
                start.add(new Instruction(Operation.globa, symbol.offset));
            }
            else
            {
                instructions.add(new Instruction(Operation.loca, symbol.offset));
            }
            SymbolType t = analysebasicexpr(globalflag);
            if (type != t)
            {
                throw new AnalyzeError(ErrorCode.InvalidInput);
            }
            SymbolEntry tmps = symbolTable.get(hash.get(nameToken.getValueString()));
            if (tmps.constflag==true)
            {
                throw new AnalyzeError(ErrorCode.AssignToConstant);
            }
            else 
            {
                if (tmps.initflag==false)
                {
                    tmps.initflag=true;
                }
            }
            if (globalflag==true)
            {
                start.add(new Instruction(Operation.store64));
            }
            else
            {
                instructions.add(new Instruction(Operation.store64));
            }
        }
        expect(TokenType.SEMICOLON);
    }

    private SymbolType analysebasicexpr(boolean globalflag) throws CompileError {
        Stack<TokenType> symbolStack = new Stack<>();
        Stack<SymbolType> exprStack = new Stack<>();
        if (symbolStack.empty()) 
        {
            symbolStack.push(TokenType.EOF);
            exprStack.push(analyseExpr(globalflag));
        }
        while (!symbolStack.empty()) 
        {
            TokenType nextType = peek().getTokenType();
            int x = terminals.indexOf(symbolStack.peek());
            int y = terminals.indexOf(nextType);
            if (x == -1 && y == -1) break;
            else if (x == -1 || y != -1 && map[x][y] == false) 
            {
                symbolStack.push(nextType);
                next();
                if (nextType == TokenType.AS_KW) 
                {
                    SymbolType type;
                    if (peek().getTokenType()==TokenType.INT_KW)
                    {
                        next();
                        type=SymbolType.INT;
                    }
                    else if (peek().getTokenType()==TokenType.DOUBLE_KW)
                    {
                        next();
                        type=SymbolType.DOUBLE;
                    }
                    else if (peek().getTokenType()==TokenType.VOID_KW)
                    {
                        next();
                        type=SymbolType.VOID;
                    }
                    else 
                    {
                        throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
                    }
                    exprStack.push(type);
                } 
                else
                {
                    exprStack.push(analyseExpr(globalflag));
                }

            } 
            else if (y == -1 || map[x][y] == true) 
            {
                huisu(symbolStack, exprStack, globalflag);
            }
        }
        return exprStack.peek();
    }

    private SymbolType analyseExpr(boolean globalflag) throws CompileError {
        Token token;
        ArrayList<Instruction> Ins=instructions;
        if (globalflag==true)
        {
            Ins = start;
        }
        if (check(TokenType.UINT_LITERAL)) 
        {
            token = expect(TokenType.UINT_LITERAL);
            Ins.add(new Instruction(Operation.push, token.getValue()));
            return SymbolType.INT;
        } 
        else if (check(TokenType.DOUBLE_LITERAL)) 
        {
            token = expect(TokenType.DOUBLE_LITERAL);
            Ins.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double) token.getValue())));
            return SymbolType.DOUBLE;
        } 
        else if (check(TokenType.STRING_LITERAL)) 
        {
            token = expect(TokenType.STRING_LITERAL);
            Ins.add(new Instruction(Operation.push, (long) globalOffset++));
            Globals.add(token.getValueString());
            return SymbolType.INT;
        } 
        else if (check(TokenType.CHAR_LITERAL)) 
        {
            token = expect(TokenType.CHAR_LITERAL);
            Ins.add(new Instruction(Operation.push, (long) (char) token.getValue()));
            return SymbolType.INT;
        } 
        else if (check(TokenType.IDENT)) 
        {
            token = expect(TokenType.IDENT);
            Integer currentIndex = this.hash.get(token.getValueString());
            SymbolEntry symbol = null;
            if (currentIndex != null) 
            {
                symbol = this.symbolTable.get(currentIndex);
            }

            if (check(TokenType.ASSIGN)) 
            {
                if (symbol == null)
                    throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                if(symbol.symbolrange==SymbolRange.global)
                {
                    instructions.add(new Instruction(Operation.globa, symbol.offset));
                }
                else if(symbol.symbolrange==SymbolRange.local)
                {
                    instructions.add(new Instruction(Operation.loca, symbol.offset));
                }
                else if(symbol.symbolrange==SymbolRange.param)
                {
                    instructions.add(new Instruction(Operation.arga, symbol.offset));
                }
                next();
                SymbolType t = analysebasicexpr(false);
                SymbolEntry tmps = symbolTable.get(hash.get(token.getValueString()));
                if (tmps.constflag==true)
                {
                    throw new AnalyzeError(ErrorCode.AssignToConstant);
                }
                else 
                {
                    if (tmps.initflag==false)
                    {
                        tmps.initflag=true;
                    }
                }
                if (t != symbol.symbolType)
                {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment);
                }
                instructions.add(new Instruction(Operation.store64));
                return SymbolType.VOID;
            } 
            else if (nextIf(TokenType.L_PAREN) != null) 
            {
                SymbolType funcReturnType;
                ArrayList<SymbolType> params;
                int callnameaddr = -1;
                if (symbol == null) 
                {
                    if(token.getValueString()=="getint" || token.getValueString()=="getchar")
                    {
                        funcReturnType = SymbolType.INT;
                        params = new ArrayList<>();
                    }
                    else if(token.getValueString()=="getdouble")
                    {
                        funcReturnType = SymbolType.DOUBLE;
                        params = new ArrayList<>();
                    }
                    else if(token.getValueString()=="putint" || token.getValueString()=="putchar"|| token.getValueString()=="putstr")
                    {
                        funcReturnType = SymbolType.VOID;
                        params = new ArrayList<SymbolType>()
                        {
                            {
                                add(SymbolType.INT);
                            }
                        };
                    }
                    else if(token.getValueString()=="putdouble")
                    {
                        funcReturnType = SymbolType.VOID;
                        params = new ArrayList<SymbolType>()
                        {
                            {
                                add(SymbolType.DOUBLE);
                            }
                        };
                    }
                    else if(token.getValueString()=="putln")
                    {
                        funcReturnType = SymbolType.VOID;
                        params = new ArrayList<SymbolType>();
                    }
                    else
                    {
                        throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                    }
                    Globals.add(token.getValueString());
                    callnameaddr = globalOffset++;
                } 
                else 
                {
                    funcReturnType = symbol.symbolType;
                    params = symbol.params;
                }
                int stackSize=0;
                if (funcReturnType != SymbolType.VOID)
                {
                    stackSize+=1;
                }
                Ins.add(new Instruction(Operation.stackalloc, stackSize));
                int paramsSize = params.size();
                int i = 0;
                if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN)) 
                {
                    SymbolType t = analysebasicexpr(globalflag);
                    if (i + 1 > paramsSize || t != params.get(i++))
                    {
                        throw new AnalyzeError(ErrorCode.InvalidInput);
                    }
                    while (nextIf(TokenType.COMMA) != null) 
                    {
                        t = analysebasicexpr(globalflag);
                        if (i + 1 > paramsSize || t != params.get(i++))
                        {
                            throw new AnalyzeError(ErrorCode.InvalidInput);
                        }
                    }
                }
                expect(TokenType.R_PAREN);
                if (symbol == null)
                {
                    Ins.add(new Instruction(Operation.callname, callnameaddr));
                }
                else
                {
                    Ins.add(new Instruction(Operation.call, symbol.funcOffset));
                }
                expect(TokenType.R_PAREN);
                return funcReturnType;
            } 
            else 
            {
                if (symbol == null)
                {
                    throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                }
                if(symbol.symbolrange==SymbolRange.global)
                {
                    Ins.add(new Instruction(Operation.globa, symbol.offset));
                }
                else if(symbol.symbolrange==SymbolRange.local)
                {
                    Ins.add(new Instruction(Operation.loca, symbol.offset));
                }
                else if(symbol.symbolrange==SymbolRange.param)
                {
                    Ins.add(new Instruction(Operation.arga, symbol.offset));
                }
                Ins.add(new Instruction(Operation.load64));
                return symbol.symbolType;
            }
        } 
        else if (check(TokenType.MINUS)) 
        {
            expect(TokenType.MINUS);
            SymbolType t = analyseExpr(globalflag);
            if (t == SymbolType.INT) 
            {
                if (globalflag==true)
                {
                    start.add(new Instruction(Operation.negi));
                }
                else
                {
                    instructions.add(new Instruction(Operation.negi));
                }
            } 
            else 
            {
                if (globalflag)
                {
                    start.add(new Instruction(Operation.negf));
                }
                else
                {
                    instructions.add(new Instruction(Operation.negf));
                }
            }
            return t;
        }
        else if (check(TokenType.L_PAREN)) 
        {
            expect(TokenType.L_PAREN);
            SymbolType element = analysebasicexpr(globalflag);
            expect(TokenType.R_PAREN);
            return element;
        } 
        else
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        }
    }

    private void huisu(Stack<TokenType> symbols, Stack<SymbolType> Exprs, boolean globalflag) throws CompileError {
        if (Exprs.size() <= 1) 
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        }
        SymbolType type2 = Exprs.pop();
        SymbolType type1 = Exprs.peek();
        TokenType symboltype = symbols.pop();
        if (symboltype == TokenType.AS_KW) 
        {
            if (type1 == SymbolType.VOID || type2 == SymbolType.VOID) 
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
            }
            else
            {
                if (type1 == SymbolType.INT && type2 == SymbolType.DOUBLE)
                {
                    type1 = SymbolType.DOUBLE;
                    if (globalflag == true) 
                    {
                        start.add(new Instruction(Operation.itof));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.itof));
                    }
                } 
                else if (type1 == SymbolType.DOUBLE && type2 == SymbolType.INT) 
                {
                    type1 = SymbolType.INT;
                    if (globalflag == true) 
                    {
                        start.add(new Instruction(Operation.ftoi));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.ftoi));
                    }
                }
            }
        } 
        else 
        {
            if (type1 != type2) 
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
            }
            if (symboltype == TokenType.GT) 
            {
                if (type1 == SymbolType.INT) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setgt));
                type1 = SymbolType.BOOL;
            } 
            else if (symboltype == TokenType.LT) 
            {
                if (type1 == SymbolType.INT) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setlt));
                type1 = SymbolType.BOOL;
            } 
            else if (symboltype == TokenType.GE) 
            {
                if (type1 == SymbolType.INT) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setlt));
                instructions.add(new Instruction(Operation.not));
                type1 = SymbolType.BOOL;
            } 
            else if (symboltype == TokenType.LE) 
            {
                if (type1 == SymbolType.INT
                )
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setgt));
                instructions.add(new Instruction(Operation.not));
                type1 = SymbolType.BOOL;
            } 
            else if (symboltype == TokenType.EQ) 
            {
                if (type1 == SymbolType.INT) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.not));
                type1 = SymbolType.BOOL;
            } 
            else if (symboltype == TokenType.NEQ) 
            {
                if (type1 == SymbolType.INT) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                type1 = SymbolType.BOOL;
            } 
            else if (symboltype == TokenType.PLUS) 
            {
                if (globalflag == true) 
                {
                    if (type1 == SymbolType.INT) {
                        start.add(new Instruction(Operation.addi));
                    } 
                    else 
                    {
                        start.add(new Instruction(Operation.addf));
                    }
                } 
                else 
                {
                    if (type1 == SymbolType.INT) 
                    {
                        instructions.add(new Instruction(Operation.addi));
                    } 
                    else {
                        instructions.add(new Instruction(Operation.addf));
                    }
                }
            } 
            else if (symboltype == TokenType.MINUS) 
            {
                if (globalflag == true) 
                {
                    if (type1 == SymbolType.INT) 
                    {
                        start.add(new Instruction(Operation.subi));
                    } 
                    else 
                    {
                        start.add(new Instruction(Operation.subf));
                    }
                } 
                else 
                {
                    if (type1 == SymbolType.INT) 
                    {
                        instructions.add(new Instruction(Operation.subi));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.subf));
                    }
                }
            } 
            else if (symboltype == TokenType.MUL) 
            {
                if (globalflag == true) 
                {
                    if (type1 == SymbolType.INT) 
                    {
                        start.add(new Instruction(Operation.muli));
                    } 
                    else 
                    {
                        start.add(new Instruction(Operation.mulf));
                    }
                } 
                else 
                {
                    if (type1 == SymbolType.INT) 
                    {
                        instructions.add(new Instruction(Operation.muli));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.mulf));
                    }
                }
            } 
            else if (symboltype == TokenType.DIV) 
            {
                if (globalflag == true) 
                {
                    if (type1 == SymbolType.INT) 
                    {
                        start.add(new Instruction(Operation.divi));
                    } 
                    else 
                    {
                        start.add(new Instruction(Operation.divf));
                    }
                } 
                else 
                {
                    if (type1 == SymbolType.INT) 
                    {
                        instructions.add(new Instruction(Operation.divi));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.divf));
                    }
                }
            }
        }
    }

    public void output(FileOutputStream out) throws IOException {
        printint(out,0x72303b3e);
        printint(out,1);
        printint(out,Globals.size());
        for (String global : Globals) {
            if (global == "0") {
                out.write(0);
                printint(out,8);
                printlong(out,0L);
            } else if (global == "1") {
                out.write(1);
                printint(out,8);
                printlong(out,0L);
            } else {
                out.write(1);
                printint(out,global.length());
                out.write(global.getBytes());
            }
        }
        outputfunctions.add(start);
        int first = 0;
        for (int i = 1; i < instructions.size(); i++) 
        {
            if (instructions.get(i).operation == Operation.func) 
            {
                outputfunctions.add(new ArrayList<>(instructions.subList(first, i)));
                first = i;
            }
        }
        outputfunctions.add(new ArrayList<>(instructions.subList(first, instructions.size())));
        printint(out,outputfunctions.size());
        for (ArrayList<Instruction> funcins : outputfunctions) {
            for (Instruction ain : funcins) 
            {
                if (ain.operation==Operation.func) {
                    FunctionEntry afunc = (FunctionEntry) ain;
                    printint(out,afunc.offset);
                    printint(out,afunc.returnnum);
                    printint(out,afunc.paramnum);
                    printint(out,afunc.localnum);
                    printint(out,funcins.size() - 1);
                } else 
                {
                    out.write(ain.operation.getValue());
                    if (ain.x != null) {
                        if (ain.operation == Operation.push) {
                            printlong(out,Long.valueOf(ain.x.toString()));
                        } else {
                            printint(out,(int) ain.x);
                        }
                    }

                }
            }
        }
        out.close();
    }

    public static void printlong(FileOutputStream out,long val) throws IOException {
        byte[] b = new byte[8];
        b[7] = (byte) (val & 0xff);
        b[6] = (byte) ((val >> 8) & 0xff);
        b[5] = (byte) ((val >> 16) & 0xff);
        b[4] = (byte) ((val >> 24) & 0xff);
        b[3] = (byte) ((val >> 32) & 0xff);
        b[2] = (byte) ((val >> 40) & 0xff);
        b[1] = (byte) ((val >> 48) & 0xff);
        b[0] = (byte) ((val >> 56) & 0xff);
        out.write(b);
    }

    public static void printint(FileOutputStream out,int val) throws IOException {
        byte[] b = new byte[4];
        b[3] = (byte) (val & 0xff);
        b[2] = (byte) ((val >> 8) & 0xff);
        b[1] = (byte) ((val >> 16) & 0xff);
        b[0] = (byte) ((val >> 24) & 0xff);
        out.write(b);
    }



}

