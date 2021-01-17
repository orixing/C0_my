package instruction;

public class FunctionInstruction extends Instruction{
    public int localnum;
    public int returnnum;
    public int paramnum;
    public int offset;

    public FunctionInstruction(Operation operation) {
        super(operation);
    }

    public FunctionInstruction(Operation operation, int localnum, int returnnum, int paramnum, int offset) {
        super(operation);
        this.localnum = localnum;
        this.returnnum = returnnum;
        this.paramnum = paramnum;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLocalCount() {
        return localnum;
    }

    public void setLocalCount(int localnum) {
        this.localnum = localnum;
    }

    public int getReturnCount() {
        return returnnum;
    }

    public void setReturnCount(int returnnum) {
        this.returnnum = returnnum;
    }

    public int getParamCount() {
        return paramnum;
    }

    public void setParamCount(int paramnum) {
        this.paramnum = paramnum;
    }

    @Override
    public String toString() {
        return  getOperation()+"["+offset+
                "] " + localnum +
                " " + paramnum +
                "->" + returnnum;
    }
}
