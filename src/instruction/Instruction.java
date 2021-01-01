package instruction;

public class Instruction {
    public Operation opt;
    public Object x;

    public Instruction(Operation opt) {
        this.opt = opt;
        this.x = null;
    }

    public Instruction(Operation opt, Object x) {
        this.opt = opt;
        this.x = x;
    }

    @Override
    public String toString() {
        return ""+opt.getValue() + " " + x;
    }
}
