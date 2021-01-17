package symbol;

import java.util.ArrayList;

public class Symbol {
    private String name;
    private boolean isConstant = false;
    private boolean isInitialized = false;
    private boolean isFunction = false;
    private SymbolType symbolType;
    private ArrayList<SymbolType> params = null;
    private StorageType storageType;
    private int offset;
    private int funcOffset;
    private int chain = -1;

    public Symbol(String name, boolean isFunction, StorageType storageType, int offset, int funcOffset) {
        this.name = name;
        this.isFunction = isFunction;
        this.storageType = storageType;
        this.offset = offset;
        this.funcOffset=funcOffset;
        this.params=new ArrayList<>();
    }

    public Symbol(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, StorageType storageType, int offset) {
        this.name = name;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.symbolType = symbolType;
        this.storageType = storageType;
        this.offset = offset;
    }

    public Symbol(String name, boolean isConstant, boolean isInitialized, SymbolType symbolType, int chain, StorageType storageType, int offset) {
        this.name = name;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.symbolType = symbolType;
        this.storageType = storageType;
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

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }
}
