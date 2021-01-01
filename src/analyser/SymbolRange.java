package analyser;

public enum SymbolRange {
    Global,
    Local,
    Param;


    @Override
    public String toString() {
        switch (this) {
            case Global:
                return "Global";
            case Local:
                return "Local";
            case Param:
                return "Param";
            default:
                return "InvalidRange";
        }
    }
}

