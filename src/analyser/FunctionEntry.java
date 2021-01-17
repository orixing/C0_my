package analyser;

import instruction.Instruction;
import instruction.Operation;

public class FunctionEntry extends Instruction{
    public int localnum;
    public int returnum;// 至今不明白为啥这个会是num。。虽然说明里这么写。。
    // 难道可能是因为长度？
    public int paramnum;
    public int offset;
    public FunctionEntry(Operation operation) {
        super(operation);
    }

    public FunctionEntry(Operation operation, int localCount, int returnCount, int paramCount, int offset) {
        super(operation);
        this.localnum = localCount;
        this.returnum = returnCount;
        this.paramnum = paramCount;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return  "fn [" + offset + "] " + localnum + " " + paramnum + "->" + returnum;
    }

	public void add(FunctionEntry start) {
	}
    
}
