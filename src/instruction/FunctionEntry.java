package instruction;

public class FunctionEntry extends Instruction{
    public int localnum;
    public int returnnum;
    public int paramnum;
    public int offset;

    public FunctionEntry(Operation operation) {
        super(operation);
    }

    public FunctionEntry(Operation operation, int localnum, int returnnum, int paramnum, int offset) {
        super(operation);
        this.localnum = localnum;
        this.returnnum = returnnum;
        this.paramnum = paramnum;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return  operation+"["+offset+"] "+localnum +" "+paramnum+"->"+returnnum;
    }
}
