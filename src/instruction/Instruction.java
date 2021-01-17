package instruction;

public class Instruction {
    public Operation operation;
    public Object x;

    public Instruction(Operation operation) {
        this.operation = operation;
    }

    public Instruction(Operation operation, Object param1) {
        this.operation = operation;
        this.x = param1;
    }

    public Operation getOperation() {
        return operation;
    }

    public Object getParam1() {
        return x;
    }

    public void setParam1(Object param1) {
        this.x = param1;
    }

    @Override
    public String toString() {
        return operation +
                "(" + x +
                ')';
    }
}
