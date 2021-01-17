package analyser;

public enum SymbolType {
    Int,
    Double,
    Void,
    Bool;


    @Override
    public String toString() {
        switch (this) {
            case Int:
                return "Int";
            case Double:
                return "Double";
            case Void:
                return "Void";
            default:
                return "InvalidType";
        }
    }
}

