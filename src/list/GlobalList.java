package list;

import java.util.List;

public class GlobalList {
    List<Global> Globals;


    boolean isexist(String name){
        for(Global i : this.Globals)
        {
            if(i.name == name)
            {
                return true;
            }
        }
        return false;
    }
}
