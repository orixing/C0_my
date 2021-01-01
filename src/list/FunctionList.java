package list;

import java.util.List;

public class FunctionList {
    List<Function> Functions;



    
    boolean isexist(String name){
        for(Function i : this.Functions)
        {
            if(i.name == name)
            {
                return true;
            }
        }
        return false;
    }
}
