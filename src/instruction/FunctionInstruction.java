package instruction;

public class FunctionInstruction extends Instruction{
    private int localCount;
    private int returnCount;
    private int paramCount;
    private int offset;

    public FunctionInstruction(Operation operation) {
        super(operation);
    }

    public FunctionInstruction(Operation operation, int localCount, int returnCount, int paramCount, int offset) {
        super(operation);
        this.localCount = localCount;
        this.returnCount = returnCount;
        this.paramCount = paramCount;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLocalCount() {
        return localCount;
    }

    public void setLocalCount(int localCount) {
        this.localCount = localCount;
    }

    public int getReturnCount() {
        return returnCount;
    }

    public void setReturnCount(int returnCount) {
        this.returnCount = returnCount;
    }

    public int getParamCount() {
        return paramCount;
    }

    public void setParamCount(int paramCount) {
        this.paramCount = paramCount;
    }

    @Override
    public String toString() {
        return  getOperation()+"["+offset+
                "] " + localCount +
                " " + paramCount +
                "->" + returnCount;
    }
}
