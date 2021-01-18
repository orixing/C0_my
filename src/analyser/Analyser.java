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


    //指令集
    ArrayList<Instruction> instructions;

    //start函数指令集
    ArrayList<Instruction> start;
    //全局变量
    ArrayList<String> Globals;
    //偏移量
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
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private SymbolEntry addSymbol(String name, boolean isConstant, boolean initflag, SymbolType symbolType, SymbolRange symbolrange, Pos curPos) throws AnalyzeError {
        Integer addr = this.hash.get(name);
        if (addr != null && addr >= this.index.peek()) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            if (addr != null) {
                switch (symbolrange) {
                    case global:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, initflag, symbolType, addr, symbolrange, globalOffset++));
                        if (isConstant)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case param:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, initflag, symbolType, addr, symbolrange, paramOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, initflag, symbolType, addr, symbolrange, localOffset++));
                        break;
                }
                System.out.println("add/dup:" + symbolTable.peek().name);
            } else {
                switch (symbolrange) {
                    case global:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, initflag, symbolType, symbolrange, globalOffset++));
                        if (isConstant)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case param:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, initflag, symbolType, symbolrange, paramOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new SymbolEntry(name, isConstant, initflag, symbolType, symbolrange, localOffset++));
                        break;
                }
                System.out.println("add:" + symbolTable.peek().name);
            }
            this.hash.put(name, symbolTable.size() - 1);
        }
        return this.symbolTable.peek();
    }

    private SymbolEntry addFuncSymbol(String name, Pos curPos) throws AnalyzeError {
        Integer location = this.hash.get(name);
        if (location != null && location >= this.index.peek()) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            SymbolEntry symbol = new SymbolEntry(name, true, SymbolRange.global, globalOffset++, funcOffset++);
            this.symbolTable.push(symbol);
            this.hash.put(name, symbolTable.size() - 1);
            this.index.push(symbolTable.size());
            Globals.add(name);
            System.out.println("add:" + symbolTable.peek().name);
            return symbol;
        }
    }

    private void addBlock() {
        this.index.push(this.symbolTable.size());
        System.out.println("add block");
    }

    private void changeInitialized(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry symbol = symbolTable.get(hash.get(name));
        if (symbol.constflag==true)
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        else {
            if (!symbol.initflag)
                symbol.initflag=true;
        }
    }

    private void removeBlockSymbols(boolean isFunction) {
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
//        SymbolEntry topSymbol = this.symbolTable.peek();
//        if (topSymbol.symbolrange() == SymbolRange.local)
//            localOffset = topSymbol.offset + 1;
//        else
//            localOffset = 0;
        if (isFunction)
            paramOffset = 0;
        System.out.println("remove block");
    }

    private void analyseProgram() throws CompileError {
        Globals.add("_start");
        start.add(new FunctionEntry(Operation.func, 0, 0, 0, globalOffset++));
        while (check(TokenType.FN_KW) || check(TokenType.LET_KW) || check(TokenType.CONST_KW)) {
            if (check(TokenType.FN_KW))
                analyseFunction();
            else
                analyseDeclStmt(SymbolRange.global);
        }
        Token eof = expect(TokenType.EOF);
        start.add(new Instruction(Operation.stackalloc, 0));
        start.add(new Instruction(Operation.call, this.symbolTable.get(this.hash.get("main")).funcOffset));
    }

    //need implement
    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);
        Token nameToken = expect(TokenType.IDENT);
        SymbolEntry funcSymbol = addFuncSymbol(nameToken.getValueString(), nameToken.getStartPos());
        localOffset = 0;
        FunctionEntry functionInstruction = new FunctionEntry(Operation.func);
        instructions.add(functionInstruction);
        expect(TokenType.L_PAREN);
        if (check(TokenType.IDENT))
            analyseFunctionParamList(funcSymbol.params);
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        SymbolType type = analyseType();
        funcSymbol.symbolType=type;
        functionInstruction.paramnum=paramOffset;
        if (type == SymbolType.VOID)
            functionInstruction.returnnum=0;
        else {
            functionInstruction.returnnum=1;
            int last = symbolTable.size() - 1;
            for (int i = 0; i < paramOffset; i++) {
                SymbolEntry symbol = this.symbolTable.get(last - i);
                symbol.offset=symbol.offset + 1;
            }
            //instructions.add(new Instruction(Operation.arga, 0));
        }
        functionInstruction.offset=funcSymbol.offset;
        boolean[] b = analyseBlockStmt(true, false, type, 0, null);
        if (type != SymbolType.VOID && !b[0]) {
            throw new AnalyzeError(ErrorCode.InvalidInput, nameToken.getStartPos());
        }
        if (type == SymbolType.VOID && !b[0])
            instructions.add(new Instruction(Operation.ret));
        functionInstruction.localnum=localOffset;
    }

    private void analyseFunctionParamList(ArrayList<SymbolType> params) throws CompileError {
        do {
            analyseFunctionParam(params);
        } while (nextIf(TokenType.COMMA) != null);
    }

    //need implement
    private void analyseFunctionParam(ArrayList<SymbolType> params) throws CompileError {
        boolean isConstant = false;
        if (nextIf(TokenType.CONST_KW) != null)
            isConstant = true;
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type = analyseType();
        addSymbol(nameToken.getValueString(), isConstant, true, type, SymbolRange.param, nameToken.getStartPos());
        params.add(type);
    }

    private boolean[] analyseBlockStmt(boolean isFunction, boolean insideWhile, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        boolean haveReturn = false;
        boolean haveBreakOrContinue = false;
        int returnSize = 0;
        int breakOrContinueSize = 0;
        expect(TokenType.L_BRACE);
        if (!isFunction)
            addBlock();
        while (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN) || check(TokenType.LET_KW) || check(TokenType.CONST_KW) || check(TokenType.IF_KW) || check(TokenType.WHILE_KW) || check(TokenType.BREAK_KW) || check(TokenType.CONTINUE_KW) || check(TokenType.RETURN_KW) || check(TokenType.SEMICOLON) || check(TokenType.L_BRACE)) {
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
        removeBlockSymbols(isFunction);
        return new boolean[]{haveReturn, haveBreakOrContinue};
    }

    private boolean[] analyseStmt(boolean insideWhile, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        if (check(TokenType.CONST_KW) || check(TokenType.LET_KW))
            analyseDeclStmt(SymbolRange.local);
        else if (check(TokenType.IF_KW))
            return analyseIfStmt(insideWhile, returnType, loopLoc, breakList);
        else if (check(TokenType.WHILE_KW))
            analyseWhileStmt(returnType);
        else if (check(TokenType.BREAK_KW)) {
            if (insideWhile)
                analyseBreakStmt(breakList);
            else
                throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
            return new boolean[]{false, true};
        } else if (check(TokenType.CONTINUE_KW)) {
            if (insideWhile)
                analyseContinueStmt(loopLoc);
            else
                throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
            return new boolean[]{false, true};
        } else if (check(TokenType.RETURN_KW)) {
            analyseReturnStmt(returnType);
            return new boolean[]{true, false};
        } else if (check(TokenType.L_BRACE))
            return analyseBlockStmt(false, insideWhile, returnType, loopLoc, breakList);
        else if (check(TokenType.SEMICOLON))
            expect(TokenType.SEMICOLON);
        else
            analyseExprStmt();
        return new boolean[]{false, false};
    }

    private void analyseDeclStmt(SymbolRange symbolrange) throws CompileError {
        if (check(TokenType.CONST_KW))
            analyseConstDeclStmt(symbolrange);
        else
            analyseLetDeclStmt(symbolrange);
    }

    private void analyseConstDeclStmt(SymbolRange symbolrange) throws CompileError {
        boolean isGlobal = symbolrange == SymbolRange.global;
        expect(TokenType.CONST_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type = analyseType();
        if (type == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidInput, nameToken.getStartPos());
        SymbolEntry symbol = addSymbol(nameToken.getValueString(), true, true, type, symbolrange, nameToken.getStartPos());
        expect(TokenType.ASSIGN);

        if (isGlobal)
            start.add(new Instruction(Operation.globa, symbol.offset));
        else
            instructions.add(new Instruction(Operation.loca, symbol.offset));

        SymbolType t = analyseExprOPG(isGlobal);
        if (type != t)
            throw new AnalyzeError(ErrorCode.InvalidInput);
        expect(TokenType.SEMICOLON);
        if (isGlobal)
            start.add(new Instruction(Operation.store64));
        else
            instructions.add(new Instruction(Operation.store64));
    }

    private void analyseLetDeclStmt(SymbolRange symbolrange) throws CompileError {
        boolean isGlobal = symbolrange == SymbolRange.global;
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type = analyseType();
        if (type == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidInput, nameToken.getStartPos());
        SymbolEntry symbol = addSymbol(nameToken.getValueString(), false, false, type, symbolrange, nameToken.getStartPos());
        if (nextIf(TokenType.ASSIGN) != null) {

            if (isGlobal)
                start.add(new Instruction(Operation.globa, symbol.offset));
            else
                instructions.add(new Instruction(Operation.loca, symbol.offset));

            SymbolType t = analyseExprOPG(isGlobal);
            if (type != t)
                throw new AnalyzeError(ErrorCode.InvalidInput);
            changeInitialized(nameToken.getValueString(), nameToken.getStartPos());
            if (isGlobal)
                start.add(new Instruction(Operation.store64));
            else
                instructions.add(new Instruction(Operation.store64));
        }
        expect(TokenType.SEMICOLON);
    }

    private boolean[] analyseIfStmt(boolean insideWhile, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        boolean haveReturn;
        boolean haveBreakOrContinue;
        boolean haveElse = false;
        ArrayList<Integer> brToEnds = new ArrayList<>();
        expect(TokenType.IF_KW);
        SymbolType t = analyseExprOPG(false);
        if (t == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidInput);
        instructions.add(new Instruction(Operation.brtrue, 1));
        instructions.add(new Instruction(Operation.br));
        int brLoc = instructions.size() - 1;
        boolean[] b = analyseBlockStmt(false, insideWhile, returnType, loopLoc, breakList);
        haveReturn = b[0];
        haveBreakOrContinue = b[1];
        brToEnds.add(instructions.size());
        instructions.add(new Instruction(Operation.br));
        instructions.get(brLoc).setParam1(instructions.size() - brLoc - 1);
        if (check(TokenType.ELSE_KW)) {
            while (nextIf(TokenType.ELSE_KW) != null) {
                if (nextIf(TokenType.IF_KW) != null) {
                    t = analyseExprOPG(false);
                    if (t == SymbolType.VOID)
                        throw new AnalyzeError(ErrorCode.InvalidInput);
                    instructions.add(new Instruction(Operation.brtrue, 1));
                    instructions.add(new Instruction(Operation.br));
                    brLoc = instructions.size() - 1;
                    b = analyseBlockStmt(false, insideWhile, returnType, loopLoc, breakList);
                    haveReturn &= b[0];
                    haveBreakOrContinue &= b[1];
                    brToEnds.add(instructions.size());
                    instructions.add(new Instruction(Operation.br));
                    instructions.get(brLoc).setParam1(instructions.size() - brLoc - 1);
                } else {
                    b = analyseBlockStmt(false, insideWhile, returnType, loopLoc, breakList);
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
            instructions.get(brToEnd).setParam1(instructions.size() - brToEnd - 1);
        }
        return new boolean[]{haveReturn, haveBreakOrContinue};
    }

    private void analyseWhileStmt(SymbolType returnType) throws CompileError {
        expect(TokenType.WHILE_KW);
        ArrayList<Integer> breakList = new ArrayList<>();
        int loopLoc = instructions.size() - 1;
        analyseExprOPG(false);
        instructions.add(new Instruction(Operation.brtrue, 1));
        int brLoc = instructions.size();
        instructions.add(new Instruction(Operation.br));
        boolean haveBreakOrContinue = analyseBlockStmt(false, true, returnType, loopLoc, breakList)[1];
        if (!haveBreakOrContinue)
            instructions.add(new Instruction(Operation.br, loopLoc - instructions.size()));
        instructions.get(brLoc).setParam1(instructions.size() - brLoc - 1);
        for (Integer breakNum : breakList) {
            instructions.get(breakNum).setParam1(instructions.size() - breakNum - 1);
        }
    }

    private void analyseBreakStmt(ArrayList<Integer> breakList) throws CompileError {
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
        breakList.add(instructions.size());
        instructions.add(new Instruction(Operation.br));
    }

    private void analyseContinueStmt(int loopLoc) throws CompileError {
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.br, loopLoc - instructions.size()));
    }

    private void analyseReturnStmt(SymbolType returnType) throws CompileError {
        Token expect = expect(TokenType.RETURN_KW);
        if (returnType != SymbolType.VOID)
            instructions.add(new Instruction(Operation.arga, 0));
        SymbolType type = SymbolType.VOID;
        if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN)) {
            SymbolType t = analyseExprOPG(false);
            type = t;
        }
        expect(TokenType.SEMICOLON);
        if (type != returnType)
            throw new AnalyzeError(ErrorCode.InvalidInput, expect.getStartPos());
        if (type != SymbolType.VOID)
            instructions.add(new Instruction(Operation.store64));
        instructions.add(new Instruction(Operation.ret));
    }

    private void analyseExprStmt() throws CompileError {
        SymbolType t = analyseExprOPG(false);
        expect(TokenType.SEMICOLON);
        if (t != SymbolType.VOID)
            instructions.add(new Instruction(Operation.pop));
    }

    //expr->
    private SymbolType analyseExprOPG(boolean isGlobal) throws CompileError {
        Stack<TokenType> symbolStack = new Stack<>();
        Stack<SymbolType> exprStack = new Stack<>();
        //因为stack是TokenType类型的，因此用EOF代替OPG的#
        if (symbolStack.empty()) {
            symbolStack.push(TokenType.EOF);
            exprStack.push(analyseOtherExpr(isGlobal));
        }
        while (!symbolStack.empty()) {
//            if (check(TokenType.PLUS) || check(TokenType.MINUS) || check(TokenType.MUL) || check(TokenType.DIV) ||
//                    check(TokenType.EQ) || check(TokenType.NEQ) || check(TokenType.LT) || check(TokenType.GT) ||
//                    check(TokenType.GE) || check(TokenType.LE) || check(TokenType.AS_KW)) {
            TokenType nextType = peek().getTokenType();
            int x = terminals.indexOf(symbolStack.peek());
            int y = terminals.indexOf(nextType);
            if (x == -1 && y == -1) break;
            else if (x == -1 || y != -1 && map[x][y] == false) {
                symbolStack.push(nextType);
                next();
                if (nextType == TokenType.AS_KW) {
                    SymbolType type = analyseType();
                    exprStack.push(type);
                } else
                    exprStack.push(analyseOtherExpr(isGlobal));

            } else if (y == -1 || map[x][y] == true) {
                huisu(symbolStack, exprStack, isGlobal);
            }
//            }
        }
        return exprStack.peek();
    }

    private void huisu(Stack<TokenType> symbols, Stack<SymbolType> Exprs, boolean isGlobal) throws CompileError {
        if (Exprs.size() > 1) {
            SymbolType t2 = Exprs.pop();
            SymbolType t1 = Exprs.peek();
            TokenType type = symbols.pop();
            if (TokenType.AS_KW == type) {
                if (t1 == SymbolType.VOID || t2 == SymbolType.VOID)
                    throw new AnalyzeError(ErrorCode.InvalidInput);
                else {
                    if (t1 == SymbolType.INT && t2 == SymbolType.DOUBLE) {
                        t1=t2;
                        if (isGlobal)
                            start.add(new Instruction(Operation.itof));
                        else
                            instructions.add(new Instruction(Operation.itof));
                    } else if (t1 == SymbolType.DOUBLE && t2 == SymbolType.INT) {
                        t1=t2;
                        if (isGlobal)
                            start.add(new Instruction(Operation.ftoi));
                        else
                            instructions.add(new Instruction(Operation.ftoi));
                    }
                }
            } else {
                if (t1 == t2) {
                    switch (type) {
                        case GT:
                            if (t1 == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setgt));
                            t1=SymbolType.BOOL;
                            break;
                        case LT:
                            if (t1 == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setlt));
                            t1=SymbolType.BOOL;
                            break;
                        case GE:
                            if (t1 == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setlt));
                            instructions.add(new Instruction(Operation.not));
                            t1=SymbolType.BOOL;
                            break;
                        case LE:
                            if (t1 == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setgt));
                            instructions.add(new Instruction(Operation.not));
                            t1=SymbolType.BOOL;
                            break;
                        case EQ:
                            if (t1 == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.not));
                            t1=SymbolType.BOOL;
                            break;
                        case NEQ:
                            if (t1 == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            t1=SymbolType.BOOL;
                            break;
                        case PLUS:
                            if (t1 == SymbolType.INT) {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.addi));
                                else
                                    instructions.add(new Instruction(Operation.addi));
                            } else {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.addf));
                                else
                                    instructions.add(new Instruction(Operation.addf));
                            }
                            break;
                        case MINUS:
                            if (t1 == SymbolType.INT) {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.subi));
                                else
                                    instructions.add(new Instruction(Operation.subi));
                            } else {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.subf));
                                else
                                    instructions.add(new Instruction(Operation.subf));
                            }
                            break;
                        case MUL:
                            if (t1 == SymbolType.INT) {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.muli));
                                else
                                    instructions.add(new Instruction(Operation.muli));
                            } else {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.mulf));
                                else
                                    instructions.add(new Instruction(Operation.mulf));
                            }
                            break;
                        case DIV:
                            if (t1 == SymbolType.INT) {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.divi));
                                else
                                    instructions.add(new Instruction(Operation.divi));

                            } else {
                                if (isGlobal)
                                    start.add(new Instruction(Operation.divf));
                                else
                                    instructions.add(new Instruction(Operation.divf));
                            }
                            break;
                    }
                } else throw new AnalyzeError(ErrorCode.InvalidInput);
            }
        } else throw new EmptyStackException();
    }

    private SymbolType analyseOtherExpr(boolean isGlobal) throws CompileError {
        Token token;
        ArrayList<Instruction> chosenInstruction;
        if (isGlobal)
            chosenInstruction = start;
        else
            chosenInstruction = instructions;
        if (check(TokenType.UINT_LITERAL)) {
            token = expect(TokenType.UINT_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, token.getValue()));
            return SymbolType.INT;
        } else if (check(TokenType.DOUBLE_LITERAL)) {
            token = expect(TokenType.DOUBLE_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double) token.getValue())));
            return SymbolType.DOUBLE;
        } else if (check(TokenType.STRING_LITERAL)) {
            token = expect(TokenType.STRING_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, (long) globalOffset++));
            Globals.add(token.getValueString());
            return SymbolType.INT;
        } else if (check(TokenType.CHAR_LITERAL)) {
            token = expect(TokenType.CHAR_LITERAL);
            chosenInstruction.add(new Instruction(Operation.push, (long) (char) token.getValue()));
            return SymbolType.INT;
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
                switch (symbol.symbolrange) {
                    case global:
                        instructions.add(new Instruction(Operation.globa, symbol.offset));
                        break;
                    case param:
                        instructions.add(new Instruction(Operation.arga, symbol.offset));
                        break;
                    case local:
                        instructions.add(new Instruction(Operation.loca, symbol.offset));
                        break;
                }
                Token assign = next();
                SymbolType t = analyseExprOPG(false);
                changeInitialized(token.getValueString(), token.getStartPos());
                if (t != symbol.symbolType)
                    throw new AnalyzeError(ErrorCode.InvalidAssignment);
                instructions.add(new Instruction(Operation.store64));
                return SymbolType.VOID;
            } else if (nextIf(TokenType.L_PAREN) != null) {
                SymbolType funcReturnType;
                ArrayList<SymbolType> params;
                int callnameOffset = -1;
                if (symbol == null) {
                    switch (token.getValueString()) {
                        case "getint":
                        case "getchar":
                            funcReturnType = SymbolType.INT;
                            params = new ArrayList<>();
                            break;
                        case "getdouble":
                            funcReturnType = SymbolType.DOUBLE;
                            params = new ArrayList<>();
                            break;
                        case "putint":
                        case "putchar":
                        case "putstr":
                            funcReturnType = SymbolType.VOID;
                            params = new ArrayList<SymbolType>() {{
                                add(SymbolType.INT);
                            }};
                            break;
                        case "putdouble":
                            funcReturnType = SymbolType.VOID;
                            params = new ArrayList<SymbolType>() {{
                                add(SymbolType.DOUBLE);
                            }};
                            break;
                        case "putln":
                            funcReturnType = SymbolType.VOID;
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
                if (funcReturnType == SymbolType.VOID)
                    stackSize = 0;
                else
                    stackSize = 1;
                chosenInstruction.add(new Instruction(Operation.stackalloc, stackSize));
                int paramsSize = params.size();
                int i = 0;
                if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL) || check(TokenType.DOUBLE_LITERAL) || check(TokenType.STRING_LITERAL) || check(TokenType.CHAR_LITERAL) || check(TokenType.L_PAREN)) {
                    SymbolType t = analyseExprOPG(isGlobal);
                    if (i + 1 > paramsSize || t != params.get(i++))
                        throw new AnalyzeError(ErrorCode.InvalidInput);
                    while (nextIf(TokenType.COMMA) != null) {
                        t = analyseExprOPG(isGlobal);
                        if (i + 1 > paramsSize || t != params.get(i++))
                            throw new AnalyzeError(ErrorCode.InvalidInput);
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
                switch (symbol.symbolrange) {
                    case global:
                        chosenInstruction.add(new Instruction(Operation.globa, symbol.offset));
                        break;
                    case param:
                        instructions.add(new Instruction(Operation.arga, symbol.offset));
                        break;
                    case local:
                        instructions.add(new Instruction(Operation.loca, symbol.offset));
                        break;
                }
                chosenInstruction.add(new Instruction(Operation.load64));
                return symbol.symbolType;
            }
        } else if (check(TokenType.MINUS)) {
            return analyseNegateExpr(isGlobal);
        } else if (check(TokenType.L_PAREN)) {
            expect(TokenType.L_PAREN);
            SymbolType element = analyseExprOPG(isGlobal);
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
        SymbolType t = analyseOtherExpr(isGlobal);
        if (t == SymbolType.INT) {
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


    private SymbolType analyseType() throws CompileError {
        if (nextIf(TokenType.INT_KW) != null)
            return SymbolType.INT;
        else if (nextIf(TokenType.DOUBLE_KW) != null)
            return SymbolType.DOUBLE;
        else if (nextIf(TokenType.VOID_KW) != null)
            return SymbolType.VOID;
        else {
            List<TokenType> list = Arrays.asList(TokenType.INT_KW, TokenType.DOUBLE_KW, TokenType.VOID_KW);
            throw new ExpectedTokenError(list, peek());
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