package IR.Function;

import IR.SSAIR.SSAIR;

import java.util.List;

public class Function {

    private int functionIdent;          // id of function identifier token
    private boolean isVoid;             // true if function is void
    private List<Integer> params;       // list of identifier id of parameters
    private SSAIR cfg;                  // control flow graph of this function

    public Function() {}

    public void setFunctionIdent(int functionIdent) {
        this.functionIdent = functionIdent;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public void setIsVoid(boolean isVoid) {
        this.isVoid = isVoid;
    }
}
