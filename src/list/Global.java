package list;

public class Global {
    boolean isconst;
    int length;
    String name;
    String type;
    String value;

    /**
     * @param isconst
     * @param length
     * @param type
     * @param value
     */
    public Global(boolean isconst, int length, String name, String type, String value) {
        this.isconst = isconst;
        this.length = length;
        this.type = type;
        this.value = value;
    }

    
}
