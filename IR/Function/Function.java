package IR.Function;

import IR.SSAIR.SSAIR;

import java.util.ArrayList;
import java.util.List;

public class Function {

    private int functionId;          // id of function identifier token
    private boolean isVoid;             // true if function is void. Default is false.
    private List<Integer> params;       // list of identifier id of parameters
    private final SSAIR cfg;            // control flow graph of this function

    public Function(SSAIR cfg) {
        isVoid = false;
        params = new ArrayList<>();
        this.cfg = cfg;
    }

    public int getFunctionId() {
        return functionId;
    }

    public void setFunctionId(int functionId) {
        this.functionId = functionId;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public void setIsVoid() {
        this.isVoid = true;
    }

    public void addParam(int id) {
        params.add(id);
    }

    public List<Integer> getParams() {
        return params;
    }
}
