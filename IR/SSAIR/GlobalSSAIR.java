package IR.SSAIR;

import IR.BasicBlock.BasicBlock;
import IR.Function.Function;
import IR.Instruction.FunctionCall;
import IR.Instruction.Instruction;
import IR.Instruction.RegisterInstr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** the global SSAIR, stores a list of Functions of function declarations and the current SSA CFG */
public class GlobalSSAIR
{
    private final SSAIR globalIR;
    private final List<Function> functions;

    private SSAIR currentIR;
    private Function currentFunction;

    public GlobalSSAIR() {
        functions = new ArrayList<>();
        globalIR = currentIR = new SSAIR();
    }

    public void restoreGlobalIR() {
        this.currentIR = globalIR;
    }

    // ---------------------------- FUNCTION METHODS ----------------------------- //

    /** create new function object and add to list of Function, set currentIr to funcIR */
    public void enterFunctionDef() {
        SSAIR funcIR = new SSAIR();
        Function thisFunc = new Function(funcIR);
        functions.add(thisFunc);
        currentIR = funcIR;
        currentFunction = thisFunc;
    }

    /** set current function's isVoid to true */
    public void currentFunctionIsVoid() {
        currentFunction.setIsVoid();
    }

    /** sets current function's id */
    public void setCurrentFunctionIdent(int id) {
        currentFunction.setFunctionId(id);
    }

    /** adds parameter's token id to current function's list of params */
    public void addParamToCurrentFunction(int id) {
        currentFunction.addParam(id);
    }

    /** adds current function params into function's SSAIR's symbol table, values are represented as RegisterInstr */
    public void initializeParamsVarDecl() {
        for (int id: currentFunction.getParams()) {
            RegisterInstr reg = new RegisterInstr();
            currentIR.insertRegisterInstrToHead(reg);
            currentIR.assign(id, reg);
        }
    }

    /** calls current function by inserting call instruction */
    public Instruction callCurrentFunction(int id, String fname) {
        Instruction call = new FunctionCall(id, fname);
        currentIR.insertInstrToCurrentBlock(call);
        return call;

    }

    /** given the function id, returns true if the function is void */
    public boolean functionIsVoid(int id) {
        for (Function f : functions) {
            if (f.getFunctionId() == id) {
                return f.isVoid();
            }
        }
        return false;
    }

    // ---------- any IR method calls in Parser.java is called on the current IR ---------- //
    // ------------------------- CFG GENERATION METHODS --------------------------- //

    public BasicBlock getCurrentBlock() {
        return currentIR.getCurrentBlock();
    }

    public void setCurrentBlock(BasicBlock target) {
         currentIR.setCurrentBlock(target);
    }

    public BasicBlock generateFallThruBlock(BasicBlock.BlockType blockType) {
        return currentIR.generateFallThruBlock(blockType);
    }

    public BasicBlock enterIf() {
        return currentIR.enterIf();
    }

    public BasicBlock generateElseBlock(BasicBlock parent) {
        return currentIR.generateElseBlock(parent);
    }

    public BasicBlock findJoinBlock() {
        return currentIR.findJoinBlock();
    }

    public BasicBlock enterWhile() {
        return currentIR.enterWhile();
    }

    // ------------------------- SSA INSTRUCTION GENERATION METHODS --------------------------- //

    public Instruction addConstantIfNotExists(int c) {
        return currentIR.addConstantIfNotExists(c);
    }

    public void addVarDecl(int id) {
        currentIR.addVarDecl(id);
    }

    public Instruction insertInstrToCurrentBlock(Instruction i) {
        return currentIR.insertInstrToCurrentBlock(i);
    }

    public Instruction getIdentifierInstruction(int id) {
        return currentIR.getIdentifierInstruction(id);
    }

    public void assign(int id, Instruction value) {
        currentIR.assign(id, value);
    }

    public void propagateNestedIf(BasicBlock parentBlock) {
        currentIR.propagateNestedIf(parentBlock);
    }

    public void propagateNestedWhile(BasicBlock parentBlock) {
        currentIR.propagateNestedWhile(parentBlock);
    }

    public HashSet<Integer> getUninitializedVarErrors() {
        return currentIR.getUninitializedVarErrors();
    }

    public void setBranchInstr(BasicBlock parent) {
        currentIR.setBranchInstr(parent);
    }

    public void addBranchInstr(BasicBlock target) {
        currentIR.addBranchInstr(target);
    }

    // -------------------------------- CSE METHODS ---------------------------------- //

    public void propagateCommonSubexpr() {
        currentIR.propagateCommonSubexpr();
    }

    public boolean error() {
        return currentIR.error();
    }

    // ------------------------- VISUALIZATION METHODS --------------------------- //

    public void printCFG(Map<String, Integer> lexerMap, boolean showSymbolTable) {
        currentIR.printCFG(lexerMap, showSymbolTable);
    }
}
