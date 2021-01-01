package analyser;

public class FunctionEntry {
    int localnum;
    int returnum;// 至今不明白为啥这个会是num。。虽然说明里这么写。。
    // 难道可能是因为长度？
    int paramnum;
    int offset;
    public FunctionEntry() {

    }

    public FunctionEntry(int localCount, int returnCount, int paramCount, int offset) {
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
