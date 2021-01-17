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

    public int getlocalnum() {
        return localnum;
    }

    public void setlocalnum(int localnum) {
        this.localnum = localnum;
    }

    public int getreturnnum() {
        return returnnum;
    }

    public void setreturnnum(int returnnum) {
        this.returnnum = returnnum;
    }

    public int getparamnum() {
        return paramnum;
    }

    public void setparamnum(int paramnum) {
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
