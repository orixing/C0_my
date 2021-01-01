package analyser;

import java.util.ArrayList;

public class SymbolEntry {
    String name;
    boolean isConstant;
    boolean isInitialized;
    boolean isFunction;
    SymbolType symbolType;
    SymbolRange symbolRange;
    ArrayList<SymbolType> params = null;
    int stackoffset;
    int funcOffset;
    int lastaddr;
    //变量
    public SymbolEntry(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, SymbolRange symbolRange, int stackoffset) {
        this.name = name;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.symbolType = symbolType;
        this.symbolRange = symbolRange;
        this.stackoffset = stackoffset;
        this.lastaddr = -1;
    }
    //函数
    public SymbolEntry(String name, boolean isFunction, SymbolRange symbolRange, int stackoffset, int funcOffset) {
        this.name = name;
        this.isFunction = isFunction;
        this.symbolRange = symbolRange;
        this.stackoffset = stackoffset;
        this.funcOffset=funcOffset;
        this.params=new ArrayList<>();
        this.lastaddr = -1;
    }
    //作用域变量
    public SymbolEntry(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, SymbolRange symbolRange, int stackoffset, int lastaddr) {
        this.name = name;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.symbolType = symbolType;
        this.symbolRange = symbolRange;
        this.stackoffset = stackoffset;
        this.lastaddr = lastaddr;
    }
}


