package analyser;

import error.*;
import instruction.FunctionInstruction;
import instruction.Instruction;
import instruction.Operation;
import symbol.StorageType;
import symbol.Symbol;
import symbol.SymbolType;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;
import util.Pos;
import util.WriteFile;

import java.io.IOException;
import java.util.*;

public class Analyser {
    //词法分析器
    Tokenizer tokenizer;
    Token peekedToken = null;

    //OPG矩阵
    ArrayList<TokenType> terminals = new ArrayList<>(Arrays.asList(TokenType.GT,
            TokenType.LT, TokenType.GE, TokenType.LE, TokenType.EQ, TokenType.NEQ,
            TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.AS_KW));
    //1=less,2=more
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

    //栈式符号表
    Stack<Symbol> symbolTable = new Stack<>();
    //分程序索引
    Stack<Integer> index = new Stack<>();
    //Hash查找
    HashMap<String, Integer> hashMap = new HashMap<>();

    //指令集
    ArrayList<Instruction> instructions;

    //start函数指令集
    ArrayList<Instruction> startInstructions;
    //全局变量
    ArrayList<String> Globals;
    //偏移量
    int globalOffset = 0;
    int argumentOffset = 0;
    int localOffset = 0;
    int funcOffset = 1;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
        this.startInstructions = new ArrayList<>();
        this.Globals = new ArrayList<>();
        this.index.push(0);
    }

    public void analyse(String fileName) throws CompileError, IOException {
        analyseProgram();
        for (String global : Globals) {
            System.out.println(global);
        }
        System.out.println();
        for (Instruction instruction : instructions) {
            System.out.println(instruction.toString());
        }
        System.out.println();
        for (Instruction startInstruction : startInstructions) {
            System.out.println(startInstruction.toString());
        }
        WriteFile.writeFile(fileName, Globals, instructions, startInstructions);
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
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private Symbol addSymbol(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, StorageType storageType, Pos curPos) throws AnalyzeError {
        Integer addr = this.hashMap.get(name);
        if (addr != null && addr >= this.index.peek()) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            if (addr != null) {
                switch (storageType) {
                    case global:
                        this.symbolTable.push(new Symbol(name, isConstant, isInitialized, symbolType, addr, storageType, globalOffset++));
                        if (isConstant)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case argument:
                        this.symbolTable.push(new Symbol(name, isConstant, isInitialized, symbolType, addr, storageType, argumentOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new Symbol(name, isConstant, isInitialized, symbolType, addr, storageType, localOffset++));
                        break;
                }
                System.out.println("add/dup:" + symbolTable.peek().getName());
            } else {
                switch (storageType) {
                    case global:
                        this.symbolTable.push(new Symbol(name, isConstant, isInitialized, symbolType, storageType, globalOffset++));
                        if (isConstant)
                            Globals.add("1");
                        else
                            Globals.add("0");
                        break;
                    case argument:
                        this.symbolTable.push(new Symbol(name, isConstant, isInitialized, symbolType, storageType, argumentOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new Symbol(name, isConstant, isInitialized, symbolType, storageType, localOffset++));
                        break;
                }
                System.out.println("add:" + symbolTable.peek().getName());
            }
            this.hashMap.put(name, symbolTable.size() - 1);
        }
        return this.symbolTable.peek();
    }

    private Symbol addFuncSymbol(String name, Pos curPos) throws AnalyzeError {
        Integer location = this.hashMap.get(name);
        if (location != null && location >= this.index.peek()) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            Symbol symbol = new Symbol(name, true, StorageType.global, globalOffset++, funcOffset++);
            this.symbolTable.push(symbol);
            this.hashMap.put(name, symbolTable.size() - 1);
            this.index.push(symbolTable.size());
            Globals.add(name);
            System.out.println("add:" + symbolTable.peek().getName());
            return symbol;
        }
    }

    private void addBlock() {
        this.index.push(this.symbolTable.size());
        System.out.println("add block");
    }

    private void changeInitialized(String name, Pos curPos) throws AnalyzeError {
        Symbol symbol = symbolTable.get(hashMap.get(name));
        if (symbol.isConstant())
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        else {
            if (!symbol.isInitialized())
                symbol.setInitialized(true);
        }
    }

    private void removeBlockSymbols(boolean isFunction) {
        int endIndex = index.pop();
        for (int i = symbolTable.size() - 1; i >= endIndex; i--) {
            Symbol tmpSymbol = symbolTable.pop();
            if (tmpSymbol.getChain() == -1) {
                hashMap.remove(tmpSymbol.getName());
                System.out.println();
            } else {
                hashMap.put(tmpSymbol.getName(), tmpSymbol.getChain());
            }
        }
//        Symbol topSymbol = this.symbolTable.peek();
//        if (topSymbol.getStorageType() == StorageType.local)
//            localOffset = topSymbol.getOffset() + 1;
//        else
//            localOffset = 0;
        if (isFunction)
            argumentOffset = 0;
        System.out.println("remove block");
    }

    private void analyseProgram() throws CompileError {
        Globals.add("_start");
        startInstructions.add(new FunctionInstruction(Operation.func, 0, 0, 0, globalOffset++));
        while (check(TokenType.FN_KW) || check(TokenType.LET_KW) || check(TokenType.CONST_KW)) {
            if (check(TokenType.FN_KW))
                analyseFunction();
            else
                analyseDeclStmt(StorageType.global);
        }
        Token eof = expect(TokenType.EOF);
        if (this.hashMap.get("main") == null)
            throw new AnalyzeError(ErrorCode.NeedMainFunction, eof.getStartPos());
        startInstructions.add(new Instruction(Operation.stackalloc, 0));
        startInstructions.add(new Instruction(Operation.call, this.symbolTable.get(this.hashMap.get("main")).getFuncOffset()));
    }

    //need implement
    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);
        Token nameToken = expect(TokenType.IDENT);
        Symbol funcSymbol = addFuncSymbol(nameToken.getValueString(), nameToken.getStartPos());
        localOffset = 0;
        FunctionInstruction functionInstruction = new FunctionInstruction(Operation.func);
        instructions.add(functionInstruction);
        expect(TokenType.L_PAREN);
        if (check(TokenType.IDENT))
            analyseFunctionParamList(funcSymbol.getParams());
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        SymbolType type = analyseType();
        funcSymbol.setSymbolType(type);
        functionInstruction.setParamCount(argumentOffset);
        if (type == SymbolType.VOID)
            functionInstruction.setReturnCount(0);
        else {
            functionInstruction.setReturnCount(1);
            int last = symbolTable.size() - 1;
            for (int i = 0; i < argumentOffset; i++) {
                Symbol symbol = this.symbolTable.get(last - i);
                symbol.setOffset(symbol.getOffset() + 1);
            }
            //instructions.add(new Instruction(Operation.arga, 0));
        }
        functionInstruction.setOffset(funcSymbol.getOffset());
        boolean[] b = analyseBlockStmt(true, false, type, 0, null);
        if (type != SymbolType.VOID && !b[0]) {
            throw new AnalyzeError(ErrorCode.MissingReturnStatement, nameToken.getStartPos());
        }
        if (type == SymbolType.VOID && !b[0])
            instructions.add(new Instruction(Operation.ret));
        functionInstruction.setLocalCount(localOffset);
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
        addSymbol(nameToken.getValueString(), isConstant, true, type, StorageType.argument, nameToken.getStartPos());
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
        while (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT) || check(TokenType.DOUBLE) || check(TokenType.STRING) || check(TokenType.CHAR) || check(TokenType.L_PAREN) || check(TokenType.LET_KW) ||
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
        removeBlockSymbols(isFunction);
        return new boolean[]{haveReturn, haveBreakOrContinue};
    }

    private boolean[] analyseStmt(boolean insideWhile, SymbolType returnType, int loopLoc, ArrayList<Integer> breakList) throws CompileError {
        if (check(TokenType.CONST_KW) || check(TokenType.LET_KW))
            analyseDeclStmt(StorageType.local);
        else if (check(TokenType.IF_KW))
            return analyseIfStmt(insideWhile, returnType, loopLoc, breakList);
        else if (check(TokenType.WHILE_KW))
            analyseWhileStmt(returnType);
        else if (check(TokenType.BREAK_KW)) {
            if (insideWhile)
                analyseBreakStmt(breakList);
            else
                throw new AnalyzeError(ErrorCode.BreakOutsideLoop, peek().getStartPos());
            return new boolean[]{false, true};
        } else if (check(TokenType.CONTINUE_KW)) {
            if (insideWhile)
                analyseContinueStmt(loopLoc);
            else
                throw new AnalyzeError(ErrorCode.ContinueOutsideLoop, peek().getStartPos());
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

    private void analyseDeclStmt(StorageType storageType) throws CompileError {
        if (check(TokenType.CONST_KW))
            analyseConstDeclStmt(storageType);
        else
            analyseLetDeclStmt(storageType);
    }

    private void analyseConstDeclStmt(StorageType storageType) throws CompileError {
        boolean isGlobal = storageType == StorageType.global;
        expect(TokenType.CONST_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type = analyseType();
        if (type == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidDeclaration, nameToken.getStartPos());
        Symbol symbol = addSymbol(nameToken.getValueString(), true, true, type, storageType, nameToken.getStartPos());
        expect(TokenType.ASSIGN);

        if (isGlobal)
            startInstructions.add(new Instruction(Operation.globa, symbol.getOffset()));
        else
            instructions.add(new Instruction(Operation.loca, symbol.getOffset()));

        OPGElement element = analyseExprOPG(isGlobal);
        if (type != element.getType())
            throw new AnalyzeError(ErrorCode.InvalidType, element.getStartPos());
        expect(TokenType.SEMICOLON);
        if (isGlobal)
            startInstructions.add(new Instruction(Operation.store64));
        else
            instructions.add(new Instruction(Operation.store64));
    }

    private void analyseLetDeclStmt(StorageType storageType) throws CompileError {
        boolean isGlobal = storageType == StorageType.global;
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        SymbolType type = analyseType();
        if (type == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidDeclaration, nameToken.getStartPos());
        Symbol symbol = addSymbol(nameToken.getValueString(), false, false, type, storageType, nameToken.getStartPos());
        if (nextIf(TokenType.ASSIGN) != null) {

            if (isGlobal)
                startInstructions.add(new Instruction(Operation.globa, symbol.getOffset()));
            else
                instructions.add(new Instruction(Operation.loca, symbol.getOffset()));

            OPGElement element = analyseExprOPG(isGlobal);
            if (type != element.getType())
                throw new AnalyzeError(ErrorCode.InvalidType, element.getStartPos());
            changeInitialized(nameToken.getValueString(), nameToken.getStartPos());
            if (isGlobal)
                startInstructions.add(new Instruction(Operation.store64));
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
        OPGElement element = analyseExprOPG(false);
        if (element.getType() == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType, element.getStartPos());
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
                    element = analyseExprOPG(false);
                    if (element.getType() == SymbolType.VOID)
                        throw new AnalyzeError(ErrorCode.InvalidType, element.getStartPos());
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
        if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT) || check(TokenType.DOUBLE) ||
                check(TokenType.STRING) || check(TokenType.CHAR) || check(TokenType.L_PAREN)) {
            OPGElement element = analyseExprOPG(false);
            type = element.getType();
        }
        expect(TokenType.SEMICOLON);
        if (type != returnType)
            throw new AnalyzeError(ErrorCode.InvalidType, expect.getStartPos());
        if (type != SymbolType.VOID)
            instructions.add(new Instruction(Operation.store64));
        instructions.add(new Instruction(Operation.ret));
    }

    private void analyseExprStmt() throws CompileError {
        OPGElement element = analyseExprOPG(false);
        expect(TokenType.SEMICOLON);
        if (element.getType() != SymbolType.VOID)
            instructions.add(new Instruction(Operation.pop));
    }

    //expr->
    private OPGElement analyseExprOPG(boolean isGlobal) throws CompileError {
        Stack<TokenType> symbolStack = new Stack<>();
        Stack<OPGElement> exprStack = new Stack<>();
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
            else if (x == -1 || y != -1 && matrix[x][y] == 1) {
                symbolStack.push(nextType);
                next();
                if (nextType == TokenType.AS_KW) {
                    SymbolType type = analyseType();
                    exprStack.push(new OPGElement(type, null));
                } else
                    exprStack.push(analyseOtherExpr(isGlobal));

            } else if (y == -1 || matrix[x][y] == 2) {
                reduction(symbolStack, exprStack, isGlobal);
            }
//            }
        }
        return exprStack.peek();
    }

    private void reduction(Stack<TokenType> symbols, Stack<OPGElement> Exprs, boolean isGlobal) throws CompileError {
        if (Exprs.size() > 1) {
            SymbolType secondType = Exprs.pop().getType();
            OPGElement first = Exprs.peek();
            TokenType type = symbols.pop();
            SymbolType firstType = first.getType();
            if (TokenType.AS_KW == type) {
                if (firstType == SymbolType.VOID || secondType == SymbolType.VOID)
                    throw new AnalyzeError(ErrorCode.InvalidType, first.getStartPos());
                else {
                    if (firstType == SymbolType.INT && secondType == SymbolType.DOUBLE) {
                        first.setType(secondType);
                        if (isGlobal)
                            startInstructions.add(new Instruction(Operation.itof));
                        else
                            instructions.add(new Instruction(Operation.itof));
                    } else if (firstType == SymbolType.DOUBLE && secondType == SymbolType.INT) {
                        first.setType(secondType);
                        if (isGlobal)
                            startInstructions.add(new Instruction(Operation.ftoi));
                        else
                            instructions.add(new Instruction(Operation.ftoi));
                    }
                }
            } else {
                if (firstType == secondType) {
                    switch (type) {
                        case GT:
                            if (firstType == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setgt));
                            first.setType(SymbolType.BOOL);
                            break;
                        case LT:
                            if (firstType == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setlt));
                            first.setType(SymbolType.BOOL);
                            break;
                        case GE:
                            if (firstType == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setlt));
                            instructions.add(new Instruction(Operation.not));
                            first.setType(SymbolType.BOOL);
                            break;
                        case LE:
                            if (firstType == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.setgt));
                            instructions.add(new Instruction(Operation.not));
                            first.setType(SymbolType.BOOL);
                            break;
                        case EQ:
                            if (firstType == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            instructions.add(new Instruction(Operation.not));
                            first.setType(SymbolType.BOOL);
                            break;
                        case NEQ:
                            if (firstType == SymbolType.INT)
                                instructions.add(new Instruction(Operation.cmpi));
                            else
                                instructions.add(new Instruction(Operation.cmpf));
                            first.setType(SymbolType.BOOL);
                            break;
                        case PLUS:
                            if (firstType == SymbolType.INT) {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.addi));
                                else
                                    instructions.add(new Instruction(Operation.addi));
                            } else {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.addf));
                                else
                                    instructions.add(new Instruction(Operation.addf));
                            }
                            break;
                        case MINUS:
                            if (firstType == SymbolType.INT) {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.subi));
                                else
                                    instructions.add(new Instruction(Operation.subi));
                            } else {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.subf));
                                else
                                    instructions.add(new Instruction(Operation.subf));
                            }
                            break;
                        case MUL:
                            if (firstType == SymbolType.INT) {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.muli));
                                else
                                    instructions.add(new Instruction(Operation.muli));
                            } else {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.mulf));
                                else
                                    instructions.add(new Instruction(Operation.mulf));
                            }
                            break;
                        case DIV:
                            if (firstType == SymbolType.INT) {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.divi));
                                else
                                    instructions.add(new Instruction(Operation.divi));

                            } else {
                                if (isGlobal)
                                    startInstructions.add(new Instruction(Operation.divf));
                                else
                                    instructions.add(new Instruction(Operation.divf));
                            }
                            break;
                    }
                } else throw new AnalyzeError(ErrorCode.InvalidType, first.getStartPos());
            }
        } else throw new EmptyStackException();
    }

    private OPGElement analyseOtherExpr(boolean isGlobal) throws CompileError {
        Token token;
        ArrayList<Instruction> chosenInstruction;
        if (isGlobal)
            chosenInstruction = startInstructions;
        else
            chosenInstruction = instructions;
        if (check(TokenType.UINT)) {
            token = expect(TokenType.UINT);
            chosenInstruction.add(new Instruction(Operation.push, token.getValue()));
            return new OPGElement(SymbolType.INT, token.getStartPos());
        } else if (check(TokenType.DOUBLE)) {
            token = expect(TokenType.DOUBLE);
            chosenInstruction.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double) token.getValue())));
            return new OPGElement(SymbolType.DOUBLE, token.getStartPos());
        } else if (check(TokenType.STRING)) {
            token = expect(TokenType.STRING);
            chosenInstruction.add(new Instruction(Operation.push, (long) globalOffset++));
            Globals.add(token.getValueString());
            return new OPGElement(SymbolType.INT, token.getStartPos());
        } else if (check(TokenType.CHAR)) {
            token = expect(TokenType.CHAR);
            chosenInstruction.add(new Instruction(Operation.push, (long) (char) token.getValue()));
            return new OPGElement(SymbolType.INT, token.getStartPos());
        } else if (check(TokenType.IDENT)) {
            token = expect(TokenType.IDENT);
            Integer currentIndex = this.hashMap.get(token.getValueString());
            Symbol symbol = null;
            if (currentIndex != null) {
                symbol = this.symbolTable.get(currentIndex);
            }

            if (check(TokenType.ASSIGN)) {
                if (symbol == null)
                    throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                switch (symbol.getStorageType()) {
                    case global:
                        instructions.add(new Instruction(Operation.globa, symbol.getOffset()));
                        break;
                    case argument:
                        instructions.add(new Instruction(Operation.arga, symbol.getOffset()));
                        break;
                    case local:
                        instructions.add(new Instruction(Operation.loca, symbol.getOffset()));
                        break;
                }
                Token assign = next();
                OPGElement opgElement = analyseExprOPG(false);
                changeInitialized(token.getValueString(), token.getStartPos());
                if (opgElement.getType() != symbol.getSymbolType())
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, opgElement.getStartPos());
                instructions.add(new Instruction(Operation.store64));
                return new OPGElement(SymbolType.VOID, assign.getStartPos());
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
                    funcReturnType = symbol.getSymbolType();
                    params = symbol.getParams();
                }

                int stackSize;
                if (funcReturnType == SymbolType.VOID)
                    stackSize = 0;
                else
                    stackSize = 1;
                chosenInstruction.add(new Instruction(Operation.stackalloc, stackSize));
                int paramsSize = params.size();
                int i = 0;
                if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT) || check(TokenType.DOUBLE) || check(TokenType.STRING) || check(TokenType.CHAR) || check(TokenType.L_PAREN)) {
                    OPGElement element = analyseExprOPG(isGlobal);
                    if (i + 1 > paramsSize || element.getType() != params.get(i++))
                        throw new AnalyzeError(ErrorCode.InvalidType, element.getStartPos());
                    while (nextIf(TokenType.COMMA) != null) {
                        element = analyseExprOPG(isGlobal);
                        if (i + 1 > paramsSize || element.getType() != params.get(i++))
                            throw new AnalyzeError(ErrorCode.InvalidType, element.getStartPos());
                    }
                }
                expect(TokenType.R_PAREN);
                if (symbol == null)
                    chosenInstruction.add(new Instruction(Operation.callname, callnameOffset));
                else
                    chosenInstruction.add(new Instruction(Operation.call, symbol.getFuncOffset()));
                return new OPGElement(funcReturnType, token.getStartPos());
            } else {
                if (symbol == null)
                    throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
                switch (symbol.getStorageType()) {
                    case global:
                        chosenInstruction.add(new Instruction(Operation.globa, symbol.getOffset()));
                        break;
                    case argument:
                        instructions.add(new Instruction(Operation.arga, symbol.getOffset()));
                        break;
                    case local:
                        instructions.add(new Instruction(Operation.loca, symbol.getOffset()));
                        break;
                }
                chosenInstruction.add(new Instruction(Operation.load64));
                return new OPGElement(symbol.getSymbolType(), token.getStartPos());
            }
        } else if (check(TokenType.MINUS)) {
            return analyseNegateExpr(isGlobal);
        } else if (check(TokenType.L_PAREN)) {
            expect(TokenType.L_PAREN);
            OPGElement element = analyseExprOPG(isGlobal);
            expect(TokenType.R_PAREN);
            return element;
        } else
            throw new ExpectedTokenError(Arrays.asList(TokenType.UINT, TokenType.DOUBLE, TokenType.STRING, TokenType.CHAR, TokenType.IDENT, TokenType.MINUS), peek());
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

    private OPGElement analyseNegateExpr(boolean isGlobal) throws CompileError {
        expect(TokenType.MINUS);
        OPGElement element = analyseOtherExpr(isGlobal);
        if (element.getType() == SymbolType.INT) {
            if (isGlobal)
                startInstructions.add(new Instruction(Operation.negi));
            else
                instructions.add(new Instruction(Operation.negi));
        } else {
            if (isGlobal)
                startInstructions.add(new Instruction(Operation.negf));
            else
                instructions.add(new Instruction(Operation.negf));
        }
        return element;
    }


    private SymbolType analyseType() throws CompileError {
        if (nextIf(TokenType.INT_TY) != null)
            return SymbolType.INT;
        else if (nextIf(TokenType.DOUBLE_TY) != null)
            return SymbolType.DOUBLE;
        else if (nextIf(TokenType.VOID_TY) != null)
            return SymbolType.VOID;
        else {
            List<TokenType> list = Arrays.asList(TokenType.INT_TY, TokenType.DOUBLE_TY, TokenType.VOID_TY);
            throw new ExpectedTokenError(list, peek());
        }

    }

}
