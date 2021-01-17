package analyser;

import symbol.SymbolType;
import util.Pos;

public class OPGElement {
    private SymbolType type;
    private Pos startPos;

    public OPGElement(SymbolType type, Pos startPos) {
        this.type = type;
        this.startPos = startPos;
    }

    public SymbolType getType() {
        return type;
    }

    public Pos getStartPos() {
        return startPos;
    }

    public void setType(SymbolType type) {
        this.type = type;
    }
}
