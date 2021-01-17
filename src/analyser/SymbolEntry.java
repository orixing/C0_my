package analyser;

import java.util.ArrayList;

public class SymbolEntry {
    public String name;
    public boolean isConstant = false;
    public boolean isInitialized = false;
    public boolean isFunction = false;
    public SymbolType symbolType;
    public ArrayList<SymbolType> params = null;
    public SymbolRange symbolRange;
    public int offset;
    public int funcOffset;
    public int chain = -1;

    public SymbolEntry(String name, boolean isFunction, SymbolRange symbolRange, int offset, int funcOffset) {
        this.name = name;
        this.isFunction = isFunction;
        this.symbolRange = symbolRange;
        this.offset = offset;
        this.funcOffset=funcOffset;
        this.params=new ArrayList<>();
    }

    public SymbolEntry(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, SymbolRange symbolRange, int offset) {
        this.name = name;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.symbolType = symbolType;
        this.symbolRange = symbolRange;
        this.offset = offset;
    }

    public SymbolEntry(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, int chain, SymbolRange symbolRange, int offset) {
        this.name = name;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.symbolType = symbolType;
        this.symbolRange = symbolRange;
        this.offset = offset;
        this.chain = chain;
    }

    public int getFuncOffset() {
        return funcOffset;
    }

    public void setFuncOffset(int funcOffset) {
        this.funcOffset = funcOffset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<SymbolType> getParams() {
        return params;
    }

    public void setParams(ArrayList<SymbolType> params) {
        this.params = params;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public void setConstant(boolean constant) {
        isConstant = constant;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public SymbolType getSymbolType() {
        return symbolType;
    }

    public void setSymbolType(SymbolType symbolType) {
        this.symbolType = symbolType;
    }

    public int getChain() {
        return chain;
    }

    public void setChain(int chain) {
        this.chain = chain;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public void setFunction(boolean function) {
        isFunction = function;
    }

    public SymbolRange getStorageType() {
        return symbolRange;
    }

    public void setStorageType(SymbolRange storageType) {
        this.symbolRange = storageType;
    }
}
