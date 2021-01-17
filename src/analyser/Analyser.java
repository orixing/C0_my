package analyser;

import list.*;
import error.AnalyzeError;
import error.CompileError;
import error.ErrorCode;
import error.ExpectedTokenError;
import error.TokenizeError;
import instruction.Instruction;
import instruction.Operation;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;
import util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表（原mini，不用） */
    //HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 输出表 */
    /** 初始化指令 */
    ArrayList<Instruction> start;
    /** 正常指令 */
    ArrayList<Instruction> instructions;
    /** 变量 */
    ArrayList<String> Globals;

    /** 下一个变量的栈偏移 */
    int globalOffset = 0;
    int localOffset = 0;
    int paramOffset = 0;
    int funcOffset = 0;
    

    /** 符号表，记录所有作用域的变量的地址  */
    Stack<SymbolEntry> symbolTable = new Stack<>();
    /** 每一圈符号表的大小 */
    Stack<Integer> index = new Stack<>();
    /** 变量名与对应的当前地址位置，记录当前作用域的变量地址*/
    HashMap<String, Integer> hash = new HashMap<>();

    static ArrayList<ArrayList<Instruction>> funcs = new ArrayList<>();

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.start = new ArrayList<>();
        this.instructions = new ArrayList<>();
        this.Globals = new ArrayList<>();
    }

    public void analyse(String fileName) throws CompileError, IOException {
        analyseProgram();
        for (String i : Globals) {
            System.out.println(i);
        }
        System.out.println();
        for (Instruction i : instructions) {
            System.out.println(i.toString());
        }
        System.out.println();
        for (Instruction i : start) {
            System.out.println(i.toString());
        }
        // output(args);
        WriteFile.writeFile(fileName, Globals, instructions, start);
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
            var token = peekedToken;
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
        var token = peek();
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
        var token = peek();
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
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    // /**
    // * 获取下一个变量的栈偏移
    // *
    // * @return
    // */
    // private int getNextVariableOffset() {
    // return this.nextOffset++;
    // }

    // private int getNextFuncOffset() {
    // return this.funcOffset++;
    // }

    // /**
    // * 添加一个符号
    // *
    // * @param name 名字
    // * @param isInitialized 是否已赋值
    // * @param isConstant 是否是常量
    // * @param curPos 当前 token 的位置（报错用）
    // * @throws AnalyzeError 如果重复定义了则抛异常
    // */
    // private void addSymbol(String name, boolean isInitialized, boolean
    // isConstant, Pos curPos, SymbolType type) throws AnalyzeError {
    // if (this.symbolTable.get(name) != null) {
    // throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
    // } else {
    // this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized,
    // getNextVariableOffset(),type));
    // }
    // }
    // /**
    // * 添加一个全局变量
    // *
    // * @param name 名字
    // * @param isInitialized 是否已赋值
    // * @param isConstant 是否是常量
    // * @param curPos 当前 token 的位置（报错用）
    // * @throws AnalyzeError 如果重复定义了则抛异常
    // */
    // private void addGlobal(String name, boolean isInitialized, boolean
    // isConstant, Pos curPos, SymbolType type) throws AnalyzeError {
    // if (this.globalTable.get(name) != null) {
    // throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
    // } else {
    // this.globalTable.put(name, new SymbolEntry(isConstant, isInitialized,
    // getNextVariableOffset(),type));
    // }
    // }
    ArrayList<TokenType> terminals = new ArrayList<>(Arrays.asList(TokenType.GT,
            TokenType.LT, TokenType.GE, TokenType.LE, TokenType.EQ, TokenType.NEQ,
            TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.AS_KW));

    public int[][] matrix = {
            {2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1},
            {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1},
            {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1},
            {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2}};

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
            if (type1 == SymbolType.Void || type2 == SymbolType.Void) 
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
            }
            else
            {
                if (type1 == SymbolType.Int && type2 == SymbolType.Double)
                {
                    type1 = SymbolType.Double;
                    if (globalflag == true) 
                    {
                        start.add(new Instruction(Operation.itof));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.itof));
                    }
                } 
                else if (type1 == SymbolType.Double && type2 == SymbolType.Int) 
                {
                    type1 = SymbolType.Int;
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
                if (type1 == SymbolType.Int) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setgt));
                type1 = SymbolType.Bool;
            } 
            else if (symboltype == TokenType.LT) 
            {
                if (type1 == SymbolType.Int) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setlt));
                type1 = SymbolType.Bool;
            } 
            else if (symboltype == TokenType.GE) 
            {
                if (type1 == SymbolType.Int) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setlt));
                instructions.add(new Instruction(Operation.not));
                type1 = SymbolType.Bool;
            } 
            else if (symboltype == TokenType.LE) 
            {
                if (type1 == SymbolType.Int)
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.setgt));
                instructions.add(new Instruction(Operation.not));
                type1 = SymbolType.Bool;
            } 
            else if (symboltype == TokenType.EQ) 
            {
                if (type1 == SymbolType.Int) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                instructions.add(new Instruction(Operation.not));
                type1 = SymbolType.Bool;
            } 
            else if (symboltype == TokenType.NEQ) 
            {
                if (type1 == SymbolType.Int) 
                {
                    instructions.add(new Instruction(Operation.cmpi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.cmpf));
                }
                type1 = SymbolType.Bool;
            } 
            else if (symboltype == TokenType.PLUS) 
            {
                if (globalflag == true) 
                {
                    if (type1 == SymbolType.Int) {
                        start.add(new Instruction(Operation.addi));
                    } 
                    else 
                    {
                        start.add(new Instruction(Operation.addf));
                    }
                } 
                else 
                {
                    if (type1 == SymbolType.Int) 
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
                    if (type1 == SymbolType.Int) 
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
                    if (type1 == SymbolType.Int) 
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
                    if (type1 == SymbolType.Int) 
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
                    if (type1 == SymbolType.Int) 
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
                    if (type1 == SymbolType.Int) 
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
                    if (type1 == SymbolType.Int) 
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

    /**
     * 添加一个符号
     * 
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private SymbolEntry addSymbol(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, SymbolRange range, Pos curPos) throws AnalyzeError {
        Integer addr = this.hash.get(name);
        if (addr != null && addr >= this.index.peek()) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            if (addr != null) {
                switch (range) {
                    case Global:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, isInitialized, symbolType, range, globalOffset++,addr));
                        if (isConstant)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case Param:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, isInitialized, symbolType, range, paramOffset++,addr));
                        break;
                    case Local:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, isInitialized, symbolType, range, localOffset++,addr));
                        break;
                }
                
            } else {
                switch (range) {
                    case Global:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, isInitialized, symbolType, range, globalOffset++));
                        if (isConstant)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case Param:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, isInitialized, symbolType, range, paramOffset++));
                        break;
                    case Local:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, isInitialized, symbolType, range, localOffset++));
                        break;
                }
            }
            this.hash.put(name, symbolTable.size() - 1);
        }
        return this.symbolTable.peek();
    }

    // 添加一个函数
    private void addFuncSymbol(String name, Pos curPos) throws AnalyzeError {
        if (this.hash.get(name) != null && this.hash.get(name) >= this.index.peek()) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            SymbolEntry s = new SymbolEntry(name, true, SymbolRange.Global, globalOffset++, funcOffset++);
            this.symbolTable.push(s);
            this.hash.put(name, symbolTable.size() - 1);
            this.index.push(symbolTable.size());
            Globals.add(name);
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void declareSymbol(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.symbolTable.get(hash.get(name));
        if (entry.isConstant) {
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        } 
        else 
        {
            if (entry.isInitialized==false)
            {
                entry.isInitialized=true;
            }
        }
    }

    /**
     * 获取变量在栈上的偏移
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    // private int getOffset(String name, Pos curPos) throws AnalyzeError {
    // var entry = this.symbolTable.get(name);
    // if (entry == null) {
    // throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
    // } else {
    // return entry.getStackOffset();
    // }
    // }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    // private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
    // var entry = this.symbolTable.get(name);
    // if (entry == null) {
    // throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
    // } else {
    // return entry.isConstant();
    // }
    // }

    // 开始语法分析

    /**
     * program -> item*
     */
    private void analyseProgram() throws CompileError {
        // start函数,添加函数，全局变量，alloc0和调用main
        Globals.add("_start");
        FunctionEntry s = new FunctionEntry(Operation.func,0, 0, 0, globalOffset++);
        start.add(s);

        while (check(TokenType.FN_KW) || check(TokenType.LET_KW) || check(TokenType.CONST_KW))
        {
            if (check(TokenType.FN_KW))
            {
                analyseFunction();
            }
            else
            {
                analyseDecl_stmt(SymbolRange.Global);
            }
        }

        expect(TokenType.EOF);
        Instruction alloc = new Instruction(Operation.stackalloc, 0);
        start.add(alloc);
        Instruction call = new Instruction(Operation.call, this.symbolTable.get(this.hash.get("main")).funcOffset);
        start.add(call);
    }

    /**
     * item -> function | decl_stmt
     */
    // private void analyseItem(SymbolRange range) throws CompileError {
    //     if (peek().getTokenType() == TokenType.FN_KW) {
    //         analyseFunction();
    //     } else if (peek().getTokenType() == TokenType.LET_KW || peek().getTokenType() == TokenType.CONST_KW) {
    //         analyseDecl_stmt(range);
    //     }
    // }

    /**
     * function_param -> 'const'? IDENT ':' ty function_param_list -> function_param
     * (',' function_param)* function -> 'fn' IDENT '(' function_param_list? ')'
     * '->' ty block_stmt
     */
    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);
        Token funcname = expect(TokenType.IDENT);

        Integer location = this.hash.get(funcname.getValueString());
        if (location != null && location >= this.index.peek()) 
        {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, funcname.getStartPos());
        }
        SymbolEntry s = new SymbolEntry(funcname.getValueString(), true, SymbolRange.Global, globalOffset++,
                funcOffset++);
        this.symbolTable.push(s);
        this.hash.put(funcname.getValueString(), symbolTable.size() - 1);
        this.index.push(symbolTable.size());
        Globals.add(funcname.getValueString());
        System.out.println("add:" + symbolTable.peek().name);
        localOffset = 0;
        FunctionEntry func = new FunctionEntry(Operation.func);
        instructions.add(func);
        expect(TokenType.L_PAREN);
        if (peek().getTokenType() == TokenType.IDENT) {
            analyseFunction_param_list(s.params);
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        SymbolType type;
        if (peek().getTokenType() == TokenType.INT_KW) {
            next();
            type = SymbolType.Int;
        } else if (peek().getTokenType() == TokenType.DOUBLE_KW) {
            next();
            type = SymbolType.Double;
        } else if (peek().getTokenType() == TokenType.VOID_KW) {
            next();
            type = SymbolType.Void;
        } else {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        }
        s.symbolType = type;
        func.paramnum = paramOffset;
        if (type == SymbolType.Void) 
        {
            func.returnum = 0;
        } 
        else 
        {
            func.returnum = 1;
            int size = symbolTable.size() - 1;
            for (int i = 0; i < paramOffset; i++) {
                SymbolEntry symbol = this.symbolTable.get(size - i);
                symbol.stackoffset += 1;
            }
        }
        func.offset = s.stackoffset;
        boolean[] b = analyseBlock_stmt(true, false, type, 0, null);
        boolean returnflag = b[0];
        boolean breakcontinueflag = b[1];
        if (returnflag == false)
        {
            if (type != SymbolType.Void) 
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
            }
            else
            {
                instructions.add(new Instruction(Operation.ret));
            }
        }
        func.localnum = localOffset;
    }

    /**
     * function_param_list -> function_param (',' function_param)*
     */
    private void analyseFunction_param_list(ArrayList<SymbolType> params) throws CompileError {
        do {
            analyseFunction_param(params);
        } while (nextIf(TokenType.COMMA) != null);
    }

    /**
     * function_param -> 'const'? IDENT ':' ty
     */
    private void analyseFunction_param(ArrayList<SymbolType> params) throws CompileError {
        boolean constflag = false;
        if (peek().getTokenType() == TokenType.CONST_KW) {
            expect(TokenType.CONST_KW);
            constflag = true;
        }
        Token name = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type;
        if (nextIf(TokenType.INT_KW) != null)
            type = SymbolType.Int;
        else if (nextIf(TokenType.DOUBLE_KW) != null)
            type = SymbolType.Double;
        else if (nextIf(TokenType.VOID_KW) != null)
            type = SymbolType.Void;
        else {
            List<TokenType> list = Arrays.asList(TokenType.INT_KW, TokenType.DOUBLE_KW, TokenType.VOID_KW);
            throw new ExpectedTokenError(list, peek());
        }
        addSymbol(name.getValueString(), constflag, true, type, SymbolRange.Param, name.getStartPos());
        params.add(type);

    }

    /**
     * let_decl_stmt ->
     *  'let' IDENT ':' ty ('=' expr)? ';' const_decl_stmt ->
     * 'const' IDENT ':' ty '=' expr ';' decl_stmt -> let_decl_stmt |
     * const_decl_stmt
     */
    private void analyseDecl_stmt(SymbolRange range) throws CompileError {
        if (peek().getTokenType() == TokenType.LET_KW) {
            analyseLet_decl_stmt(range);
        } else if (peek().getTokenType() == TokenType.CONST_KW) {
            analyseConst_decl_stmt(range);
        } else {
            throw new ExpectedTokenError(peek().getTokenType(), peek());
        } 
    }

    /**
     * let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
     */
    private void analyseLet_decl_stmt(SymbolRange range) throws CompileError {
        expect(TokenType.LET_KW);
        boolean globalflag = range == SymbolRange.Global;
        Token NAME = expect(TokenType.IDENT);// 变量名
        expect(TokenType.COLON);
        SymbolType type;
        if (nextIf(TokenType.INT_KW) != null) {
            type = SymbolType.Int;
        } else if (nextIf(TokenType.DOUBLE_KW) != null){
            type = SymbolType.Double;
        } else if (nextIf(TokenType.VOID_KW) != null) {
            type = SymbolType.Void;
        } else {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        } // 变量类型
        if (type == SymbolType.Void)
        {
            throw new AnalyzeError(ErrorCode.InvalidIdentifier, NAME.getStartPos());
        }
        addSymbol(NAME.getValueString(), false, false, type, range, NAME.getStartPos());
        SymbolEntry thissymbol = symbolTable.peek();
        if (nextIf(TokenType.ASSIGN) != null) {
            if (globalflag == true) 
            {
                start.add(new Instruction(Operation.globa, thissymbol.stackoffset));
            } 
            else 
            {
                instructions.add(new Instruction(Operation.loca, thissymbol.stackoffset));
            }
            SymbolType t = analysebasicexpr(globalflag);
            if (type != t)
                throw new AnalyzeError(ErrorCode.InvalidIdentifier, new Pos(0, 0));
            declareSymbol(NAME.getValueString(), NAME.getStartPos());
            if (globalflag == true) 
            {
                start.add(new Instruction(Operation.store64));
            } else 
            {
                instructions.add(new Instruction(Operation.store64));
            }
        }
        expect(TokenType.SEMICOLON);
    }

    /**
     * const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
     */
    private void analyseConst_decl_stmt(SymbolRange range) throws CompileError {
        boolean isGlobal = range == SymbolRange.Global;
        expect(TokenType.CONST_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type;
        if (nextIf(TokenType.INT_KW) != null) {
            type = SymbolType.Int;
        } else if (nextIf(TokenType.DOUBLE_KW) != null){
            type = SymbolType.Double;
        } else if (nextIf(TokenType.VOID_KW) != null) {
            type = SymbolType.Void;
        } else {
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
        } // 变量类型
        if (type == SymbolType.Void)
            throw new AnalyzeError(ErrorCode.InvalidInput, nameToken.getStartPos());
        SymbolEntry symbol = addSymbol(nameToken.getValueString(), true, true, type, range, nameToken.getStartPos());
        expect(TokenType.ASSIGN);

        if (isGlobal)
            start.add(new Instruction(Operation.globa, symbol.stackoffset));
        else
            instructions.add(new Instruction(Operation.loca, symbol.stackoffset));

        SymbolType t = analysebasicexpr(isGlobal);
        if (type != t)
            throw new AnalyzeError(ErrorCode.InvalidInput,new Pos(0,0));
        expect(TokenType.SEMICOLON);
        if (isGlobal)
            start.add(new Instruction(Operation.store64));
        else
            instructions.add(new Instruction(Operation.store64));
    }

    /**
     * block_stmt -> '{' stmt* '}'
     */
    private boolean[] analyseBlock_stmt(boolean isFunction, boolean insideWhile, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        boolean haveReturn = false;
        boolean haveBreakOrContinue = false;
        int returnSize = 0;
        int breakOrContinueSize = 0;
        expect(TokenType.L_BRACE);
        if (!isFunction)
            this.index.push(this.symbolTable.size());
        while (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL
        ) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN) || check(TokenType.LET_KW) ||
                check(TokenType.CONST_KW) || check(TokenType.IF_KW) || check(TokenType.WHILE_KW) || check(TokenType.BREAK_KW) || check(TokenType.CONTINUE_KW) || check(TokenType.RETURN_KW) || check(TokenType.SEMICOLON) || check(TokenType.L_BRACE)) {
            if (returnSize == 0 && haveReturn)
//                throw new AnalyzeError(ErrorCode.unreachableStatement, peek().getStartPos());
                returnSize = instructions.size();
            else if (breakOrContinueSize == 0 && haveBreakOrContinue)
                breakOrContinueSize = instructions.size();

            if (haveReturn && haveBreakOrContinue)
                analyseStmt(insideWhile, returnType, loopLoc, breakList);
            else if (haveReturn)
                haveBreakOrContinue = analyseStmt(insideWhile, returnType, loopLoc, breakList)[1];
            else if (haveBreakOrContinue)
                haveReturn = analyseStmt(insideWhile, returnType, loopLoc, breakList)[0];
            else {
                boolean[] b = analyseStmt(insideWhile, returnType, loopLoc, breakList);
                haveReturn = b[0];
                haveBreakOrContinue = b[1];
            }
        }
        expect(TokenType.R_BRACE);
        if (returnSize > 0)
            instructions.subList(returnSize, instructions.size()).clear();
        if (breakOrContinueSize > 0)
            instructions.subList(breakOrContinueSize, instructions.size()).clear();
//        if (isFunction && !haveReturn)
//            throw new AnalyzeError(ErrorCode.MissingReturnStatement, RBrace.getStartPos());
        int endIndex = index.pop();
        for (int i = symbolTable.size() - 1; i >= endIndex; i--) {
            SymbolEntry tmpSymbol = symbolTable.pop();
            if (tmpSymbol.lastaddr == -1) {
                hash.remove(tmpSymbol.name);
                System.out.println();
            } else {
                hash.put(tmpSymbol.name, tmpSymbol.lastaddr);
            }
        }
        return new boolean[]{haveReturn, haveBreakOrContinue};
    }
    /**
     * stmt -> expr_stmt | decl_stmt | if_stmt | while_stmt | break_stmt |
     * continue_stmt | return_stmt | block_stmt | empty_stmt
     */
    private boolean[] analyseStmt(boolean insideWhile, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList)
            throws CompileError {
                if (check(TokenType.CONST_KW) || check(TokenType.LET_KW))
                analyseDecl_stmt(SymbolRange.Local);
            else if (check(TokenType.IF_KW))
                return analyseIfStmt(insideWhile, returnType, loopLoc, breakList);
            else if (check(TokenType.WHILE_KW))
            {
                expect(TokenType.WHILE_KW);
                ArrayList<Integer> breakList_ = new ArrayList<>();
                int looploc = instructions.size() - 1;
                analysebasicexpr(false);
                instructions.add(new Instruction(Operation.brtrue, 1));
                int brLoc = instructions.size();
                instructions.add(new Instruction(Operation.br));
                boolean haveBreakOrContinue = analyseBlock_stmt(false, true, returnType, loopLoc, breakList_)[1];
                if (!haveBreakOrContinue)
                    instructions.add(new Instruction(Operation.br, looploc - instructions.size()));
                instructions.get(brLoc).x=instructions.size() - brLoc - 1;
                for (Integer breakNum : breakList_) {
                    instructions.get(breakNum).x=instructions.size() - breakNum - 1;
                }
            }
            else if (check(TokenType.BREAK_KW)) {
                if (insideWhile)
                {
                    expect(TokenType.BREAK_KW);
                    expect(TokenType.SEMICOLON);
                    breakList.add(instructions.size());
                    instructions.add(new Instruction(Operation.br));
                }
                else
                    throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
                return new boolean[]{false, true};
            } else if (check(TokenType.CONTINUE_KW)) {
                if (insideWhile)
                {
                    expect(TokenType.CONTINUE_KW);
                    expect(TokenType.SEMICOLON);
                    instructions.add(new Instruction(Operation.br, loopLoc - instructions.size()));
                }
                else
                    throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
                return new boolean[]{false, true};
            } else if (check(TokenType.RETURN_KW)) 
            {
                Token expect = expect(TokenType.RETURN_KW);
                if (returnType != SymbolType.Void)
                    instructions.add(new Instruction(Operation.arga, 0));
                SymbolType type = SymbolType.Void;
                if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL)||
                        check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN)) {
                    type = analysebasicexpr(false);
                }
                expect(TokenType.SEMICOLON);
                if (type != returnType)
                    throw new AnalyzeError(ErrorCode.InvalidInput, expect.getStartPos());
                if (type != SymbolType.Void)
                    instructions.add(new Instruction(Operation.store64));
                instructions.add(new Instruction(Operation.ret));
                return new boolean[]{true, false};
            } else if (check(TokenType.L_BRACE))
                return analyseBlock_stmt(false, insideWhile, returnType, loopLoc, breakList);
            else if (check(TokenType.SEMICOLON))
                expect(TokenType.SEMICOLON);
            else
            {
                SymbolType t = analysebasicexpr(false);
                expect(TokenType.SEMICOLON);
                if (t != SymbolType.Void)
                    instructions.add(new Instruction(Operation.pop));
            }
            return new boolean[]{false, false};
    }

    private boolean[] analyseIfStmt(boolean insideWhile, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        boolean haveReturn;
        boolean haveBreakOrContinue;
        boolean haveElse = false;
        ArrayList<Integer> brToEnds = new ArrayList<>();
        expect(TokenType.IF_KW);
        SymbolType t = analysebasicexpr(false);
        if (t == SymbolType.Void)
            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0,0));
        instructions.add(new Instruction(Operation.brtrue, 1));
        instructions.add(new Instruction(Operation.br));
        int brLoc = instructions.size() - 1;
        boolean[] b = analyseBlock_stmt(false, insideWhile, returnType, loopLoc, breakList);
        haveReturn = b[0];
        haveBreakOrContinue = b[1];
        brToEnds.add(instructions.size());
        instructions.add(new Instruction(Operation.br));
        instructions.get(brLoc).x=instructions.size() - brLoc - 1;
        if (check(TokenType.ELSE_KW)) {
            while (nextIf(TokenType.ELSE_KW) != null) {
                if (nextIf(TokenType.IF_KW) != null) {
                    t = analysebasicexpr(false);
                    if (t == SymbolType.Void)
                        throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
                    instructions.add(new Instruction(Operation.brtrue, 1));
                    instructions.add(new Instruction(Operation.br));
                    brLoc = instructions.size() - 1;
                    b = analyseBlock_stmt(false, insideWhile, returnType, loopLoc, breakList);
                    haveReturn &= b[0];
                    haveBreakOrContinue &= b[1];
                    brToEnds.add(instructions.size());
                    instructions.add(new Instruction(Operation.br));
                    instructions.get(brLoc).x=instructions.size() - brLoc - 1;
                } else {
                    b = analyseBlock_stmt(false, insideWhile, returnType, loopLoc, breakList);
                    haveReturn &= b[0];
                    haveBreakOrContinue &= b[1];
                    haveElse = true;
                    break;
                }
            }
        }
        if (!haveElse) {
            haveReturn = false;
            haveBreakOrContinue = false;
        }
        for (Integer brToEnd : brToEnds) {
            instructions.get(brToEnd).x=instructions.size() - brToEnd - 1;
        }
        return new boolean[]{haveReturn, haveBreakOrContinue};
    }


    /**
     * expr -> operator_expr | negate_expr | assign_expr | as_expr | call_expr |
     * literal_expr | ident_expr | group_expr
     */
    private SymbolType analysebasicexpr(boolean globalflag) throws CompileError {
        Stack<TokenType> symbolStack = new Stack<>();
        Stack<SymbolType> exprStack = new Stack<>();
        //因为stack是TokenType类型的，因此用EOF代替OPG的#
        if (symbolStack.empty()) {
            symbolStack.push(TokenType.EOF);
            exprStack.push(analyseExpr(globalflag));
        }
        while (!symbolStack.empty()) {
//            if (check(TokenType.PLUS) || check(TokenType.MINUS) || check(TokenType.MUL) || check(TokenType.DIV) ||
//                    check(TokenType.EQ) || check(TokenType.NEQ) || check(TokenType.LT) || check(TokenType.GT) ||
//                    check(TokenType.GE) || check(TokenType.LE) || check(TokenType.AS_KW)) {
            TokenType nextType = peek().getTokenType();
            int x = terminals.indexOf(symbolStack.peek());
            int y = terminals.indexOf(nextType);
            if (x == -1 && y == -1) break;
            else if (x == -1 || y != -1 && matrix[x][y] == 1) {
                symbolStack.push(nextType);
                next();
                if (nextType == TokenType.AS_KW) {
                    SymbolType type = analyseType();
                    exprStack.push(type);
                } else
                    exprStack.push(analyseExpr(globalflag));

            } else if (y == -1 || matrix[x][y] == 2) {
                huisu(symbolStack, exprStack, globalflag);
            }
//            }
        }
        return exprStack.peek();
    }

    private SymbolType analyseType() throws CompileError {
        if (nextIf(TokenType.INT_KW) != null)
            return SymbolType.Int;
        else if (nextIf(TokenType.DOUBLE_KW) != null)
            return SymbolType.Double;
        else if (nextIf(TokenType.VOID_KW) != null)
            return SymbolType.Void;
        else {
            List<TokenType> list = Arrays.asList(TokenType.INT_KW, TokenType.DOUBLE_KW, TokenType.VOID_KW);
            throw new ExpectedTokenError(list, peek());
        }

    }

    private SymbolType analyseExpr(boolean globalflag) throws CompileError 
    {
        Token token;
        ArrayList<Instruction> chosenInstruction;
        if (globalflag)
            chosenInstruction = start;
        else
            chosenInstruction = instructions;
        if (check(TokenType.UINT_LITERAL)) {
            token = expect(TokenType.UINT_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, token.getValue()));
            return SymbolType.Int;
        } else if (check(TokenType.DOUBLE_LITERAL)) {
            token = expect(TokenType.DOUBLE_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double) token.getValue())));
            return SymbolType.Double;
        } else if (check(TokenType.STRING_LITERAL)) {
            token = expect(TokenType.STRING_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, (long) globalOffset++));
            Globals.add(token.getValueString());
            return SymbolType.Int;
        } else if (check(TokenType.CHAR_LITERAL)) {
            token = expect(TokenType.CHAR_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, (long) (char) token.getValue()));
            return SymbolType.Int;
        } else if (check(TokenType.IDENT)) {
            token = expect(TokenType.IDENT);
            Integer currentIndex = this.hash.get(token.getValueString());
            SymbolEntry symbol = null;
            if (currentIndex != null) {
                symbol = this.symbolTable.get(currentIndex);
            }

            if (check(TokenType.ASSIGN)) {
                if (symbol == null)
                    throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                switch (symbol.symbolRange) {
                    case Global:
                        instructions.add(new Instruction(Operation.globa, symbol.stackoffset));
                        break;
                    case Param:
                        instructions.add(new Instruction(Operation.arga, symbol.stackoffset));
                        break;
                    case Local:
                        instructions.add(new Instruction(Operation.loca, symbol.stackoffset));
                        break;
                }
                Token assign = next();
                SymbolType SymbolType = analysebasicexpr(false);
                declareSymbol(token.getValueString(), token.getStartPos());
                if (SymbolType != symbol.symbolType)
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, new Pos(00,0));
                instructions.add(new Instruction(Operation.store64));
                return SymbolType.Void;
            } else if (nextIf(TokenType.L_PAREN) != null) {
                SymbolType funcReturnType;
                ArrayList<SymbolType> params;
                int callnameOffset = -1;
                if (symbol == null) {
                    switch (token.getValueString()) {
                        case "getint":
                        case "getchar":
                            funcReturnType = SymbolType.Int;
                            params = new ArrayList<>();
                            break;
                        case "getdouble":
                            funcReturnType = SymbolType.Double;
                            params = new ArrayList<>();
                            break;
                        case "putint":
                        case "putchar":
                        case "putstr":
                            funcReturnType = SymbolType.Void;
                            params = new ArrayList<SymbolType>() {{
                                add(SymbolType.Int);
                            }};
                            break;
                        case "putdouble":
                            funcReturnType = SymbolType.Void;
                            params = new ArrayList<SymbolType>() {{
                                add(SymbolType.Double);
                            }};
                            break;
                        case "putln":
                            funcReturnType = SymbolType.Void;
                            params = new ArrayList<>();
                            break;
//                        case "main":
//                            funcReturnType = SymbolType.VOID;
//                            params = new ArrayList<>();
//                            haveMain = true;
//                            break;
                        default:
                            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                    }
                    Globals.add(token.getValueString());
                    callnameOffset = globalOffset++;
                } else {
                    funcReturnType = symbol.symbolType;
                    params = symbol.params;
                }

                int stackSize;
                if (funcReturnType == SymbolType.Void)
                    stackSize = 0;
                else
                    stackSize = 1;
                chosenInstruction.add(new Instruction(Operation.stackalloc, stackSize));
                int paramsSize = params.size();
                int i = 0;
                if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN)) {
                    SymbolType element = analysebasicexpr(globalflag);
                    if (i + 1 > paramsSize || element != params.get(i++))
                        throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0,0));
                    while (nextIf(TokenType.COMMA) != null) {
                        element = analysebasicexpr(globalflag);
                        if (i + 1 > paramsSize || element != params.get(i++))
                            throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0,0));
                    }
                }
                expect(TokenType.R_PAREN);
                if (symbol == null)
                    chosenInstruction.add(new Instruction(Operation.callname, callnameOffset));
                else
                    chosenInstruction.add(new Instruction(Operation.call, symbol.funcOffset));
                return funcReturnType;
            } else {
                if (symbol == null)
                    throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                switch (symbol.symbolRange) {
                    case Global:
                        chosenInstruction.add(new Instruction(Operation.globa, symbol.stackoffset));
                        break;
                    case Param:
                        instructions.add(new Instruction(Operation.arga, symbol.stackoffset));
                        break;
                    case Local:
                        instructions.add(new Instruction(Operation.loca, symbol.stackoffset));
                        break;
                }
                chosenInstruction.add(new Instruction(Operation.load64));
                return symbol.symbolType;
            }
        } else if (check(TokenType.MINUS)) {
            return analyseNegateExpr(globalflag);
        } else if (check(TokenType.L_PAREN)) {
            expect(TokenType.L_PAREN);
            SymbolType element = analysebasicexpr(globalflag);
            expect(TokenType.R_PAREN);
            return element;
        } else
            throw new ExpectedTokenError(Arrays.asList(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL, TokenType.IDENT, TokenType.MINUS), peek());
//        }else if (check(TokenType.MINUS)){
//            analyseNegateExpr();
//        } else if (check(TokenType.L_PAREN)) {
//            analyseGroupExpr();
//        }
//        while (check(TokenType.PLUS) || check(TokenType.MINUS) || check(TokenType.MUL) || check(TokenType.DIV) || check(TokenType.EQ) || check(TokenType.NEQ) || check(TokenType.LT) || check(TokenType.GT) || check(TokenType.GE) || check(TokenType.LE) || check(TokenType.AS_KW)) {
//            if (nextIf(TokenType.AS_KW) != null) {
//                SymbolType type = analyseType();
//            } else {
//                Token operator = next();
//                switch (operator.getTokenType()) {
//                    case PLUS:
//                }
//            }
//        }
    }


    private SymbolType analyseNegateExpr(boolean isGlobal) throws CompileError {
        expect(TokenType.MINUS);
        SymbolType t = analyseExpr(isGlobal);
        if (t== SymbolType.Int) {
            if (isGlobal)
                start.add(new Instruction(Operation.negi));
            else
                instructions.add(new Instruction(Operation.negi));
        } else {
            if (isGlobal)
            start.add(new Instruction(Operation.negf));
            else
                instructions.add(new Instruction(Operation.negf));
        }
        return t;
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
        funcs.add(start);
        cutFunction(instructions);
        printint(out,funcs.size());
        for (ArrayList<Instruction> funcins : funcs) {
            for (Instruction ain : funcins) {
                if (ain.opt==Operation.func) {
                    FunctionEntry afunc = (FunctionEntry) ain;
                    printint(out,afunc.offset);
                    printint(out,afunc.returnum);
                    printint(out,afunc.paramnum);
                    printint(out,afunc.localnum);
                    printint(out,funcins.size() - 1);
                } else {
                    out.write(ain.opt.getValue());
                    if (ain.x != null) {
                        if (ain.opt == Operation.push) {
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

    public static void cutFunction(ArrayList<Instruction> instructions) {
        int first = 0;
        for (int i = 1; i < instructions.size(); i++) {
            if (instructions.get(i).opt == Operation.func) {
                funcs.add(new ArrayList<>(instructions.subList(first, i)));
                first = i;
            }
        }
        funcs.add(new ArrayList<>(instructions.subList(first, instructions.size())));
    }


}