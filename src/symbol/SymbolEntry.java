package symbol;

import java.util.ArrayList;

public class SymbolEntry {
    public String name;
    public boolean constflag = false;
    public boolean initflag = false;
    public boolean isFunction = false;
    public SymbolType symbolType;
    public ArrayList<SymbolType> params = null;
    public SymbolRange symbolrange;
    public int offset;
    public int funcOffset;
    public int lastaddr = -1;

    public SymbolEntry(String name, boolean isFunction, SymbolRange symbolrange, int offset, int funcOffset) {
        this.name = name;
        this.isFunction = isFunction;
        this.symbolrange = symbolrange;
        this.offset = offset;
        this.funcOffset=funcOffset;
        this.params=new ArrayList<>();
    }

    public SymbolEntry(String name, boolean constflag, boolean initflag, SymbolType symbolType, SymbolRange symbolrange, int offset) {
        this.name = name;
        this.constflag = constflag;
        this.initflag = initflag;
        this.symbolType = symbolType;
        this.symbolrange = symbolrange;
        this.offset = offset;
    }

    public SymbolEntry(String name, boolean constflag, boolean initflag, SymbolType symbolType, int lastaddr, SymbolRange symbolrange, int offset) {
        this.name = name;
        this.constflag = constflag;
        this.initflag = initflag;
        this.symbolType = symbolType;
        this.symbolrange = symbolrange;
        this.offset = offset;
        this.lastaddr = lastaddr;
    }
}
