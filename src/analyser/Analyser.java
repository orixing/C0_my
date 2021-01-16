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
import util.Pos;

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

    public void analyse(File args) throws CompileError, IOException {
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
        output(args);
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
    ArrayList<TokenType> options = new ArrayList<>(
            Arrays.asList(TokenType.GT, TokenType.LT, TokenType.GE, TokenType.LE, TokenType.EQ, TokenType.NEQ,
                    TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.AS_KW));

    public boolean[][] map = {
            // > < >= <= == != + - * / as
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
                type1 = SymbolType.Int;
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
                type1 = SymbolType.Int;
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
                type1 = SymbolType.Int;
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
                type1 = SymbolType.Int;
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
                type1 = SymbolType.Int;
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
                type1 = SymbolType.Int;
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
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, SymbolType symbolType,
            SymbolRange symbolRange, Pos curPos) throws AnalyzeError {
        if (this.hash.get(name) == null)// 没有出现过
        {
            if (symbolRange == SymbolRange.Global) {
                this.symbolTable.push(
                        new SymbolEntry(name, isConstant, isInitialized, symbolType, symbolRange, globalOffset++));
                if (isConstant)
                    Globals.add("1");
                else
                    Globals.add("0");
            } else if (symbolRange == SymbolRange.Local) {
                this.symbolTable
                        .push(new SymbolEntry(name, isConstant, isInitialized, symbolType, symbolRange, localOffset++));
            } else if (symbolRange == SymbolRange.Param) {
                this.symbolTable
                        .push(new SymbolEntry(name, isConstant, isInitialized, symbolType, symbolRange, paramOffset++));
            } else {
                throw new AnalyzeError(ErrorCode.InvalidIdentifier, curPos);
            }
        } else// 作用域嵌套
        {
            if (this.hash.get(name) >= this.index.peek())// 重复定义
            {
                throw new AnalyzeError(ErrorCode.InvalidIdentifier, curPos);
            }
            if (symbolRange == SymbolRange.Global) {
                this.symbolTable.push(
                        new SymbolEntry(name, isConstant, isInitialized, symbolType, symbolRange, globalOffset++));
                if (isConstant)
                    Globals.add("1");
                else
                    Globals.add("0");
            } else if (symbolRange == SymbolRange.Local) {
                this.symbolTable
                        .push(new SymbolEntry(name, isConstant, isInitialized, symbolType, symbolRange, localOffset++));
            } else if (symbolRange == SymbolRange.Param) {
                this.symbolTable
                        .push(new SymbolEntry(name, isConstant, isInitialized, symbolType, symbolRange, paramOffset++));
            } else {
                throw new AnalyzeError(ErrorCode.InvalidIdentifier, curPos);
            }
        }
        this.hash.put(name, symbolTable.size()-1);// 保存变量的最新地址
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
        analyseFunction_param(params);
        while (peek().getTokenType() == TokenType.COMMA) {
            expect(TokenType.COMMA);
            analyseFunction_param(params);
        }
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
        expect(TokenType.CONST_KW);
        boolean globalflag;
        if (range == SymbolRange.Global) {
            globalflag = true;
        } else {
            globalflag = false;
        }
        Token NAME = expect(TokenType.IDENT);// 变量名
        expect(TokenType.COLON);
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
        // 变量类型
        if (type == SymbolType.Void) {
            throw new AnalyzeError(ErrorCode.InvalidIdentifier, NAME.getStartPos());
        }
        addSymbol(NAME.getValueString(), false, false, type, range, NAME.getStartPos());
        SymbolEntry thissymbol = symbolTable.peek();
        expect(TokenType.ASSIGN);
        if (globalflag == true) {
            start.add(new Instruction(Operation.globa, thissymbol.stackoffset));
        } else {
            instructions.add(new Instruction(Operation.loca, thissymbol.stackoffset));
        }
        SymbolType t = analysebasicexpr(globalflag);
        if (type != t) {
            throw new AnalyzeError(ErrorCode.InvalidIdentifier, new Pos(0, 0));
        }
        if (globalflag == true) {
            start.add(new Instruction(Operation.store64));
        } else {
            instructions.add(new Instruction(Operation.store64));
        }
        expect(TokenType.SEMICOLON);
    }

    /**
     * block_stmt -> '{' stmt* '}'
     */
    private boolean[] analyseBlock_stmt(boolean isFunction, boolean isloop, SymbolType returntype, int loopaddr,
            ArrayList<Integer> breakaddrs) throws CompileError {
        expect(TokenType.L_BRACE);
        boolean returnflag = false;
        boolean breakcontinueflag = false;
        int returnlen = 0;
        int breakcontinuelen = 0;
        if (isFunction == false) {
            this.index.push(this.symbolTable.size());
        }
        while (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN) || check(TokenType.LET_KW) ||
        check(TokenType.CONST_KW) || check(TokenType.IF_KW) || check(TokenType.WHILE_KW) || check(TokenType.BREAK_KW) || check(TokenType.CONTINUE_KW) || check(TokenType.RETURN_KW) || check(TokenType.SEMICOLON) || check(TokenType.L_BRACE)) {
            if (returnflag == false && breakcontinueflag == false) {
                boolean[] b = analyseStmt(isloop, returntype, loopaddr, breakaddrs);
                returnflag = b[0];
                breakcontinueflag = b[1];
            } else if (returnflag == false && breakcontinueflag == true) {
                if (breakcontinuelen == 0) 
                {
                    breakcontinuelen = instructions.size();
                }
                boolean[] b = analyseStmt(isloop, returntype, loopaddr, breakaddrs);
                returnflag = b[0];
            } else if (returnflag == true && breakcontinueflag == false) {
                if (returnlen == 0) 
                {
                    returnlen = instructions.size();
                }
                boolean[] b = analyseStmt(isloop, returntype, loopaddr, breakaddrs);
                breakcontinueflag = b[1];
            } else if (returnflag == true && breakcontinueflag == true) {
                if (returnlen == 0) {
                    returnlen = instructions.size();
                }
                if (breakcontinuelen == 0) {
                    breakcontinuelen = instructions.size();
                }
                boolean[] b = analyseStmt(isloop, returntype, loopaddr, breakaddrs);
            }
        }
        expect(TokenType.R_BRACE);
        if (returnlen > 0)
        {
            instructions.subList(returnlen, instructions.size()).clear();
        }
        if (breakcontinuelen > 0)
        {
            instructions.subList(breakcontinuelen, instructions.size()).clear();
        }
        int endIndex = index.pop();
        for (int i = symbolTable.size(); i > endIndex; i--) {
            SymbolEntry tmp = symbolTable.pop();
            if (tmp.lastaddr != -1) {
                hash.put(tmp.name, tmp.lastaddr);
            } else if (tmp.lastaddr == -1) {
                hash.remove(tmp.name);
            }
        }
        if (isFunction)
        {
            paramOffset = 0;
        }
        paramOffset = 0;
        boolean[] b = { returnflag, breakcontinueflag };
        return b;
    }

    /**
     * stmt -> expr_stmt | decl_stmt | if_stmt | while_stmt | break_stmt |
     * continue_stmt | return_stmt | block_stmt | empty_stmt
     */
    private boolean[] analyseStmt(boolean isloop, SymbolType returntype, int loopaddr, ArrayList<Integer> breakaddrs)
            throws CompileError {
        // decl_stmt -> let_decl_stmt | const_decl_stmt
        if (peek().getTokenType() == TokenType.LET_KW || peek().getTokenType() == TokenType.CONST_KW) {
            analyseDecl_stmt(SymbolRange.Local);
        }
        // if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else'
        // block_stmt)?
        else if (peek().getTokenType() == TokenType.IF_KW) {
            expect(TokenType.IF_KW);
            SymbolType type = analysebasicexpr(false);
            if (type == SymbolType.Void) {
                throw new AnalyzeError(ErrorCode.InvalidIdentifier, new Pos(0, 0));
            }
            instructions.add(new Instruction(Operation.brtrue, 1));// 如果表达式是真就往下进行
            instructions.add(new Instruction(Operation.br));// 否则要计算跳转到哪
            int jpaddr = instructions.size() - 1;
            // 先处理if true的block
            boolean[] flags = analyseBlock_stmt(false, isloop, returntype, loopaddr, breakaddrs);
            boolean returnflag = flags[0];
            boolean breakcontinueflag = flags[1];
            boolean elseflag = false;
            ArrayList<Integer> brToEnds = new ArrayList<>();
            // 记录下处理完block的指令数
            brToEnds.add(instructions.size());
            // 添加跳转并为之前的跳转添加参数
            instructions.add(new Instruction(Operation.br));
            instructions.get(jpaddr).x = instructions.size() - jpaddr - 1;
            while (peek().getTokenType() == TokenType.ELSE_KW) {
                expect(TokenType.ELSE_KW);
                // 如果是最后的else
                if (peek().getTokenType() != TokenType.IF_KW) {
                    flags = analyseBlock_stmt(false, isloop, returntype, loopaddr, breakaddrs);
                    returnflag = returnflag & flags[0];
                    breakcontinueflag = breakcontinueflag & flags[1];
                    elseflag = true;
                    break;
                } else {
                    // 如果是if就重复之前的
                    expect(TokenType.IF_KW);
                    type = analysebasicexpr(false);
                    if (type == SymbolType.Void)
                        throw new AnalyzeError(ErrorCode.InvalidIdentifier, new Pos(0, 0));
                    instructions.add(new Instruction(Operation.brtrue, 1));
                    instructions.add(new Instruction(Operation.br));
                    jpaddr = instructions.size() - 1;
                    flags = analyseBlock_stmt(false, isloop, returntype, loopaddr, breakaddrs);
                    returnflag = returnflag & flags[0];
                    breakcontinueflag = breakcontinueflag & flags[1];
                    brToEnds.add(instructions.size());
                    instructions.add(new Instruction(Operation.br));
                    instructions.get(jpaddr).x = instructions.size() - jpaddr - 1;
                }
            }
            if (elseflag != true) // 如果没有else就代表之后的函数一定要有return
            {
                returnflag = false;
                breakcontinueflag = false;
            }
            for (int i : brToEnds) {
                instructions.get(i).x = instructions.size() - 1 - i;
            }
            boolean[] return_ = new boolean[] { returnflag, breakcontinueflag };
            return return_;
        }
        // while_stmt -> 'while' expr block_stmt
        else if (peek().getTokenType() == TokenType.WHILE_KW) {
            expect(TokenType.WHILE_KW);
            int thisloopaddr = instructions.size() - 1;
            ArrayList<Integer> thisbreakaddrs = new ArrayList<>();
            analysebasicexpr(false);
            instructions.add(new Instruction(Operation.brtrue, 1));
            int jpaddr = instructions.size();
            instructions.add(new Instruction(Operation.br));
            boolean[] b = analyseBlock_stmt(false, true, returntype, thisloopaddr, thisbreakaddrs);
            if (b[1] == false) 
            {
                Instruction I = new Instruction(Operation.br, thisloopaddr - instructions.size());
                instructions.add(I);
            }
            ((Instruction) instructions.get(jpaddr)).x = instructions.size() - 1 - jpaddr;
            for (Integer breakaddr : thisbreakaddrs) 
            {
                instructions.get(breakaddr).x = instructions.size() - 1 - breakaddr;
            }
        }
        // break_stmt -> 'break' ';'
        else if (peek().getTokenType() == TokenType.BREAK_KW) {
            expect(TokenType.BREAK_KW);
            expect(TokenType.SEMICOLON);
            if (isloop == false) 
            {
                throw new AnalyzeError(ErrorCode.AssignToConstant, new Pos(0, 0));
            }
            breakaddrs.add(instructions.size());
            instructions.add(new Instruction(Operation.br));
            boolean[] b = { false, true };
            return b;
        }
        // continue_stmt -> 'continue' ';'
        else if (peek().getTokenType() == TokenType.CONTINUE_KW) {
            expect(TokenType.CONTINUE_KW);
            expect(TokenType.SEMICOLON);
            if (isloop == false) 
            {
                throw new AnalyzeError(ErrorCode.AssignToConstant, new Pos(0, 0));
            }
            instructions.add(new Instruction(Operation.br, loopaddr - instructions.size()));
            boolean[] b = { false, true };
            return b;
        }
        // return_stmt -> 'return' expr? ';'
        else if (peek().getTokenType() == TokenType.RETURN_KW) {
            expect(TokenType.RETURN_KW);
            if (returntype != SymbolType.Void) {
                instructions.add(new Instruction(Operation.arga, 0));
            }
            SymbolType t = SymbolType.Void;
            if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) ||
            check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN)) {
                t = analysebasicexpr(false);
            }
            if (t != returntype) {
                throw new AnalyzeError(ErrorCode.InvalidInput, new Pos(0, 0));
            }
            if (t != SymbolType.Void) {
                instructions.add(new Instruction(Operation.store64));
            }
            instructions.add(new Instruction(Operation.ret));
            expect(TokenType.SEMICOLON);
            boolean[] b = { true, false };
            return b;
        }
        // block_stmt -> '{' stmt* '}'
        else if (peek().getTokenType() == TokenType.L_BRACE) {
            return analyseBlock_stmt(false, isloop, returntype, loopaddr, breakaddrs);
        }
        // empty_stmt -> ';'
        else if (peek().getTokenType() == TokenType.SEMICOLON) {
            expect(TokenType.SEMICOLON);
        }
        // expr_stmt -> expr ';'
        else {
            SymbolType t = analysebasicexpr(false);
            if (t != SymbolType.Void) {
                instructions.add(new Instruction(Operation.pop));
            }
            expect(TokenType.SEMICOLON);
        }
        boolean[] b = { false, false };
        return b;
    }

    /**
     * expr -> operator_expr | negate_expr | assign_expr | as_expr | call_expr |
     * literal_expr | ident_expr | group_expr
     */
    private SymbolType analysebasicexpr(boolean globalflag) throws CompileError {
        Stack<TokenType> symbolStack = new Stack<>();
        Stack<SymbolType> exprStack = new Stack<>();
        if (symbolStack.empty()==true) 
        {
            symbolStack.push(TokenType.EOF);
            exprStack.push(analyseExpr(globalflag));
        }
        while (symbolStack.empty() == false) {
            TokenType nextt = peek().getTokenType();
            int x = options.indexOf(symbolStack.peek());
            int y = options.indexOf(nextt);
            if (x == -1 && y == -1) 
            {
                break;
            } 
            else if (x == -1 || y != -1 && map[x][y] == false) 
            {
                symbolStack.push(nextt);
                next();
                if (nextt == TokenType.AS_KW) 
                {
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

    private SymbolType analyseExpr(boolean globalflag) throws CompileError 
    {
        // negate_expr -> '-' expr
        if (peek().getTokenType() == TokenType.MINUS) 
        {
            expect(TokenType.MINUS);
            SymbolType e = analyseExpr(globalflag);
            if (e == SymbolType.Int) 
            {
                if (globalflag == true) 
                {
                    start.add(new Instruction(Operation.negi));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.negi));
                }
            } 
            else if (e == SymbolType.Double) 
            {
                if (globalflag == true) 
                {
                    start.add(new Instruction(Operation.negf));
                } 
                else 
                {
                    instructions.add(new Instruction(Operation.negf));
                }
            }
            return e;
        }
        // literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL
        else if (peek().getTokenType() == TokenType.UINT_LITERAL) 
        {
            Token token = expect(TokenType.UINT_LITERAL);
            if (globalflag == true) 
            {
                start.add(new Instruction(Operation.push, Integer.getInteger(token.getValue().toString())));
            } 
            else 
            {
                instructions.add(new Instruction(Operation.push, Integer.getInteger(token.getValue().toString())));
            }
            return SymbolType.Int;
        } 
        else if (peek().getTokenType() == TokenType.DOUBLE_LITERAL) 
        {
            Token token = expect(TokenType.DOUBLE_LITERAL);
            if (globalflag == true) 
            {
                start.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double) token.getValue())));
            } 
            else 
            {
                instructions.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double) token.getValue())));
            }
            return SymbolType.Double;
        } else if (peek().getTokenType() == TokenType.STRING_LITERAL) 
        {
            Token token = expect(TokenType.STRING_LITERAL);
            if (globalflag == true) 
            {
                start.add(new Instruction(Operation.push, (long) globalOffset++));
            } 
            else 
            {
                instructions.add(new Instruction(Operation.push, (long) globalOffset++));
            }
            return SymbolType.Int;
        } else if (peek().getTokenType() == TokenType.CHAR_LITERAL) 
        {
            Token token = expect(TokenType.CHAR_LITERAL);
            if (globalflag == true) 
            {
                start.add(new Instruction(Operation.push, (int) (char) token.getValue()));
            } 
            else 
            {
                instructions.add(new Instruction(Operation.push, (int) (char) token.getValue()));
            }
            return SymbolType.Int;
        }
        // assign_expr -> l_expr '=' expr
        // l_expr -> IDENT
        // call_expr -> IDENT '(' call_param_list? ')'
        // call_param_list -> expr (',' expr)*
        // ident_expr -> IDENT
        else if (peek().getTokenType() == TokenType.IDENT) 
        {
            Token token = expect(TokenType.IDENT);
            Integer addr = this.hash.get(token.getValueString());
            SymbolEntry s = null;
            if (addr != null) 
            {
                s = this.symbolTable.get(addr);
            }
            // assign_expr -> l_expr '=' expr
            // l_expr -> IDENT
            if (peek().getTokenType() == TokenType.ASSIGN) 
            {
                expect(TokenType.ASSIGN);
                if (s == null) 
                {
                    throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                }
                if (s.symbolRange == SymbolRange.Global) 
                {
                    instructions.add(new Instruction(Operation.globa, s.stackoffset));
                } 
                else if (s.symbolRange == SymbolRange.Local) 
                {
                    instructions.add(new Instruction(Operation.loca, s.stackoffset));
                } 
                else if (s.symbolRange == SymbolRange.Param) 
                {
                    instructions.add(new Instruction(Operation.arga, s.stackoffset));
                }
                SymbolType t=analysebasicexpr(false);
                declareSymbol(token.getValueString(), token.getStartPos());
                if (t!=s.symbolType) 
                {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, new Pos(0, 0));
                }
                instructions.add(new Instruction(Operation.store64));
                return SymbolType.Void;
            }
            // call_expr -> IDENT '(' call_param_list? ')'
            // call_param_list -> expr (',' expr)*
            else if (peek().getTokenType() == TokenType.L_PAREN) {
                /**
                 * call_param_list -> expr (',' expr)* call_expr -> IDENT '(' call_param_list?
                 * ')'
                 */
                expect(TokenType.L_PAREN);
                int funcOffset = -1;
                ArrayList<SymbolType> params;
                SymbolType returntype;
                if (s != null) 
                {
                    params = s.params;
                    returntype = s.symbolType;
                } 
                else 
                {
                    if (token.getValueString().equals("getint") || token.getValueString().equals("getchar")) 
                    {
                        params = new ArrayList<>();
                        returntype = SymbolType.Int;
                    } 
                    else if (token.getValueString().equals("getdouble")) 
                    {
                        params = new ArrayList<>();
                        returntype = SymbolType.Double;
                    } 
                    else if (token.getValueString().equals("putchar") || token.getValueString().equals("putint")
                            || token.getValueString().equals("putstr")) 
                    {
                        returntype = SymbolType.Void;
                        params = new ArrayList<SymbolType>() 
                        {
                            {
                                add(SymbolType.Int);
                            }
                        };
                    } 
                    else if (token.getValueString().equals("putdouble")) 
                    {
                        returntype = SymbolType.Void;
                        params = new ArrayList<SymbolType>() 
                        {
                            {
                                add(SymbolType.Double);
                            }
                        };
                       
                    } 
                    else if (token.getValueString().equals("putln")) 
                    {
                        params = new ArrayList<SymbolType>();
                        returntype = SymbolType.Void;
                    } 
                    else 
                    {
                        System.out.println(token.getValueString());
                        throw new AnalyzeError(ErrorCode.InvalidAssignment, new Pos(0, 0));
                    }
                    Globals.add(token.getValueString());
                    funcOffset = globalOffset++;
                }
                if (returntype == SymbolType.Void) 
                {
                    if (globalflag == true) 
                    {
                        start.add(new Instruction(Operation.stackalloc, 0));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.stackalloc, 0));
                    }
                } 
                else 
                {
                    if (globalflag == true) 
                    {
                        start.add(new Instruction(Operation.stackalloc, 1));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.stackalloc, 1));
                    }
                }
                int paramnum = params.size();
                int i = 0;
                if (peek().getTokenType() == TokenType.MINUS || peek().getTokenType() == TokenType.IDENT
                        || peek().getTokenType() == TokenType.UINT_LITERAL
                        || peek().getTokenType() == TokenType.DOUBLE_LITERAL
                        || peek().getTokenType() == TokenType.STRING_LITERAL
                        || peek().getTokenType() == TokenType.CHAR_LITERAL
                        || peek().getTokenType() == TokenType.L_PAREN) 
                {
                    SymbolType type = analysebasicexpr(globalflag);
                    if (i + 1 > paramnum || type != params.get(i++)) 
                    {
                        throw new AnalyzeError(ErrorCode.InvalidAssignment, new Pos(paramnum, 0));
                    }
                    while (nextIf(TokenType.COMMA) != null) 
                    {
                        type = analysebasicexpr(globalflag);
                        if (i + 1 > paramnum || type != params.get(i++)) 
                        {
                            throw new AnalyzeError(ErrorCode.InvalidAssignment, new Pos(paramnum, 0));
                        }
                    }

                }
                expect(TokenType.R_PAREN);
                if (s != null) 
                {
                    if (globalflag == true) 
                    {
                        start.add(new Instruction(Operation.call, s.funcOffset));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.call, s.funcOffset));
                    }
                } 
                else 
                {
                    if (globalflag == true) 
                    {
                        start.add(new Instruction(Operation.callname, funcOffset));
                    } 
                    else 
                    {
                        instructions.add(new Instruction(Operation.callname, funcOffset));
                    }
                }
            }
            // ident_expr -> IDENT
            else 
            {
                if (s == null) 
                {
                    throw new AnalyzeError(ErrorCode.InvalidIdentifier, new Pos(0, 0));
                }
                if (globalflag == true)
                {
                    if (s.symbolRange == SymbolRange.Global) 
                    {
                        start.add(new Instruction(Operation.globa, s.stackoffset));
                    } 
                    else if (s.symbolRange == SymbolRange.Local) 
                    {
                        start.add(new Instruction(Operation.loca, s.stackoffset));
                    } 
                    else if (s.symbolRange == SymbolRange.Param) 
                    {
                        start.add(new Instruction(Operation.arga, s.stackoffset));
                    }
                    start.add(new Instruction(Operation.load64));
                } 
                else 
                {
                    if (s.symbolRange == SymbolRange.Global) 
                    {
                        instructions.add(new Instruction(Operation.globa, s.stackoffset));
                    } 
                    else if (s.symbolRange == SymbolRange.Local) 
                    {
                        instructions.add(new Instruction(Operation.loca, s.stackoffset));
                    } 
                    else if (s.symbolRange == SymbolRange.Param) 
                    {
                        instructions.add(new Instruction(Operation.arga, s.stackoffset));
                    }
                    instructions.add(new Instruction(Operation.load64));
                }
                return s.symbolType;
            }
        }

        // group_expr -> '(' expr ')'
        else if (peek().getTokenType() == TokenType.L_PAREN) 
        {
            expect(TokenType.L_PAREN);
            SymbolType s = analysebasicexpr(globalflag);
            expect(TokenType.R_PAREN);
            return s;
        } else 
        {
            throw new ExpectedTokenError(peek().getTokenType(), peek());
        }
        // operator_expr -> expr binary_operator expr
        // binary_operator -> '+' | '-' | '*' | '/' | '==' | '!=' | '<' | '>' | '<=' |
        // '>='
        // as_expr -> expr 'as' ty
        // if(peek().getTokenType() == TokenType.AS_KW)
        // {
        // expect(TokenType.AS_KW);
        // expect(TokenType.IDENT);
        // }
        // else if(peek().getTokenType() == TokenType.PLUS ||
        // peek().getTokenType() == TokenType.MINUS ||
        // peek().getTokenType() == TokenType.MUL ||
        // peek().getTokenType() == TokenType.DIV ||
        // peek().getTokenType() == TokenType.EQ ||
        // peek().getTokenType() == TokenType.NEQ ||
        // peek().getTokenType() == TokenType.LT ||
        // peek().getTokenType() == TokenType.GT ||
        // peek().getTokenType() == TokenType.LE ||
        // peek().getTokenType() == TokenType.GE)
        // {
        // next();
        // analyseExpr();
        // }
        return null;
    }

    public void output(File output) throws IOException {
        FileOutputStream out = new FileOutputStream(output);
        printint(0x72303b3e);
        printint(1);
        printint(Globals.size());
        for (String global : Globals) {
            if (global == "0") {
                out.write(0);
                printint(8);
                printlong(0L);
            } else if (global == "1") {
                out.write(1);
                printint(8);
                printlong(0L);
            } else {
                out.write(1);
                printint(global.length());
                out.write(global.getBytes());
            }
        }
        funcs.add(start);
        cutFunction(instructions);
        printint(funcs.size());
        for (ArrayList<Instruction> funcins : funcs) {
            for (Instruction ain : funcins) {
                if (ain.opt==Operation.func) {
                    FunctionEntry afunc = (FunctionEntry) ain;
                    printint(afunc.offset);
                    printint(afunc.returnum);
                    printint(afunc.paramnum);
                    printint(afunc.localnum);
                    printint(funcins.size() - 1);
                } else {
                    out.write(ain.opt.getValue());
                    if (ain.x != null) {
                        if (ain.opt == Operation.push) {
                            printlong(Long.valueOf(ain.x.toString()));
                        } else {
                            printint((int) ain.x);
                        }
                    }

                }
            }
        }
        out.close();
    }

    public static byte[] printlong(long val) {
        byte[] b = new byte[8];
        b[7] = (byte) (val & 0xff);
        b[6] = (byte) ((val >> 8) & 0xff);
        b[5] = (byte) ((val >> 16) & 0xff);
        b[4] = (byte) ((val >> 24) & 0xff);
        b[3] = (byte) ((val >> 32) & 0xff);
        b[2] = (byte) ((val >> 40) & 0xff);
        b[1] = (byte) ((val >> 48) & 0xff);
        b[0] = (byte) ((val >> 56) & 0xff);
        return b;
    }

    public static byte[] printint(int val) {
        byte[] b = new byte[4];
        b[3] = (byte) (val & 0xff);
        b[2] = (byte) ((val >> 8) & 0xff);
        b[1] = (byte) ((val >> 16) & 0xff);
        b[0] = (byte) ((val >> 24) & 0xff);
        return b;
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