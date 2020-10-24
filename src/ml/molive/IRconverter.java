package ml.molive;

// General stuff
import java.util.ArrayList;
import java.util.Stack;
import org.bytedeco.javacpp.*;

// Headers required by LLVM
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

public class IRconverter {
  private final LLVMModuleRef mod;
  private final LLVMValueRef main;
  private LLVMBasicBlockRef block;
  private final LLVMBuilderRef builder;
  private final Stack<LLVMValueRef[]> variables;
  private BytePointer error;

  public IRconverter(int variableCount) {
    BytePointer error = new BytePointer((Pointer) null); // Used to retrieve messages from functions
    LLVMLinkInMCJIT();
    LLVMInitializeNativeAsmPrinter();
    LLVMInitializeNativeAsmParser();
    LLVMInitializeNativeDisassembler();
    LLVMInitializeNativeTarget();
    mod = LLVMModuleCreateWithName("bbvm"); // Create a new top level module
    main =
        LLVMAddFunction(
            mod,
            "main",
            LLVMFunctionType(
                // LLVMPointerType(LLVMArrayType(LLVMInt64Type(),variableCount),0),
                LLVMVoidType(),
                (LLVMTypeRef) null,
                0,
                0)); // Create a new main function, which returns an array of longs.
    LLVMSetFunctionCallConv(main, LLVMCCallConv);
    block = LLVMAppendBasicBlock(main, "entry"); // Create an entry point
    builder = LLVMCreateBuilder();
    LLVMPositionBuilderAtEnd(builder, block);

    // allocate space on the stack for all the variables
    variables = new Stack<>();
    variables.add(new LLVMValueRef[variableCount]);
    for (int i = 0; i < variableCount; i++) {
      variables.peek()[i] = LLVMConstInt(LLVMInt64Type(), 0, 0);
    }
  }

  public void addIncr(int index) {
    LLVMValueRef[] vars = variables.pop();
    vars[index] = LLVMBuildAdd(builder, vars[index], LLVMConstInt(LLVMInt64Type(), 1, 0), "incr");
    variables.push(vars);
  }

  public void addDecr(int index) {
    LLVMValueRef[] vars = variables.pop();
    LLVMValueRef cmp =
        LLVMBuildICmp(
            builder, LLVMIntEQ, vars[index], LLVMConstInt(LLVMInt64Type(), 0, 0), "cmp_to_0");
    LLVMBasicBlockRef skip = LLVMAppendBasicBlock(main, "alreadyZero");
    LLVMBasicBlockRef noSkip = LLVMAppendBasicBlock(main, "notZero");
    LLVMBuildCondBr(builder, cmp, skip, noSkip);

    LLVMPositionBuilderAtEnd(builder, noSkip);
    LLVMValueRef newVar =
        LLVMBuildNUWSub(builder, vars[index], LLVMConstInt(LLVMInt64Type(), 1, 0), "decr");
    LLVMBuildBr(builder, skip);

    LLVMPositionBuilderAtEnd(builder, skip);
    LLVMValueRef res = LLVMBuildPhi(builder, LLVMInt64Type(), "result");
    LLVMValueRef[] phi_vals = {vars[index], newVar};
    LLVMBasicBlockRef[] phi_blocks = {block, noSkip};
    LLVMAddIncoming(res, new PointerPointer<>(phi_vals), new PointerPointer<>(phi_blocks), 2);

    block = skip;
    vars[index] = res;
    variables.push(vars);
  }

  public void addClear(int index) {
    LLVMValueRef[] vars = variables.pop();
    vars[index] = LLVMConstInt(LLVMInt64Type(), 0, 0);
    variables.push(vars);
  }

  public void addCopy(int from, int to) {
    LLVMValueRef[] vars = variables.pop();
    vars[to] = vars[from];
    variables.push(vars);
  }

  public WhileLabel addWhile(int index, long check) {
    LLVMBasicBlockRef loop = LLVMAppendBasicBlock(main, "loop");
    LLVMBuildBr(builder, loop);
    LLVMPositionBuilderAtEnd(builder, loop);

    LLVMValueRef[] oldVars = variables.pop();
    LLVMValueRef[] phis = new LLVMValueRef[oldVars.length];
    LLVMValueRef[] newVars = new LLVMValueRef[oldVars.length];
    for (int i = 0; i < oldVars.length; i++) {
      LLVMValueRef ref = LLVMBuildPhi(builder, LLVMInt64Type(), "whilePhi");
      LLVMAddIncoming(ref, oldVars[i], block, 1);
      phis[i] = ref;
      newVars[i] = ref;
    }

    variables.push(phis);
    variables.push(newVars);

    LLVMValueRef cmp =
        LLVMBuildICmp(
            builder,
            LLVMIntEQ,
            phis[index],
            LLVMConstInt(LLVMInt64Type(), check, 0),
            "exitCondition");
    LLVMBasicBlockRef innerLoop = LLVMAppendBasicBlock(main, "innerLoop");
    LLVMBasicBlockRef exit = LLVMAppendBasicBlock(main, "loopExit");
    LLVMBuildCondBr(builder, cmp, exit, innerLoop);
    LLVMPositionBuilderAtEnd(builder, innerLoop);

    block = innerLoop;

    return new WhileLabel(loop, exit);
  }

  public void addEnd(WhileLabel labels) {
    LLVMBuildBr(builder, labels.start);
    LLVMPositionBuilderAtEnd(builder, labels.end);
    LLVMValueRef[] newVars = variables.pop();
    LLVMValueRef[] phis = variables.pop();
    for (int i = 0; i < phis.length; i++) {
      LLVMAddIncoming(phis[i], newVars[i], block, 1);
    }
    variables.push(phis);
    block = labels.end;
  }

  public void addEOF(ArrayList<String> variableList) {

    LLVMTypeRef[] funcArgs = {LLVMPointerType(LLVMInt8Type(), 0), LLVMInt64Type()};
    LLVMValueRef printf =
        LLVMAddFunction(
            mod, "printf", LLVMFunctionType(LLVMVoidType(), new PointerPointer<>(funcArgs), 2, 0));
    LLVMSetFunctionCallConv(main, LLVMCCallConv);

    for (int i = 0; i < variableList.size(); i++) {
      LLVMValueRef fmt = LLVMBuildGlobalStringPtr(builder, variableList.get(i) + ": %lld\n", "");

      LLVMValueRef[] funcArgsTrue = {fmt, variables.peek()[i]};

      LLVMBuildCall(builder, printf, new PointerPointer<>(funcArgsTrue), 2, "");
    }

    LLVMBuildRetVoid(builder);

    LLVMVerifyModule(mod, LLVMAbortProcessAction, error);
    LLVMDisposeMessage(error);

    LLVMDisposeBuilder(builder);
  }

  private void runOptimiser() {
    LLVMPassManagerRef pass = LLVMCreatePassManager();
    LLVMAddConstantMergePass(pass);
    LLVMAddFunctionInliningPass(pass);
    LLVMAddGlobalOptimizerPass(pass);
    LLVMAddDeadArgEliminationPass(pass);
    LLVMAddConstantPropagationPass(pass);
    LLVMAddAggressiveInstCombinerPass(pass);
    LLVMAddPromoteMemoryToRegisterPass(pass);
    // LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
    LLVMAddLoopRotatePass(pass);
    // LLVMAddLoopUnrollPass(pass);
    LLVMAddGVNPass(pass);
    LLVMAddCFGSimplificationPass(pass);
    LLVMAddLoopDeletionPass(pass);
    // LLVMAddLoopVectorizePass(pass);
    LLVMAddAggressiveDCEPass(pass);
    LLVMAddDeadStoreEliminationPass(pass);
    LLVMAddGVNPass(pass);
    LLVMAddCFGSimplificationPass(pass);

    LLVMRunPassManager(pass, mod);
    // LLVMRunPassManager(pass, mod);
    // LLVMRunPassManager(pass, mod);
    LLVMDisposePassManager(pass);
  }

  public void run() {
    LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();
    if (LLVMCreateJITCompilerForModule(engine, mod, 2, error) != 0) {
      System.err.println(error.getString());
      LLVMDisposeMessage(error);
      System.exit(-1);
    }

    runOptimiser();
    // LLVMDumpModule(mod);

    System.out.println();
    System.out.println("Running main...");
    LLVMRunFunction(engine, main, 0, (PointerPointer<Pointer>) null);

    LLVMDisposeExecutionEngine(engine);
  }

  public void dumpCode() {
    runOptimiser();

    var triple = LLVMGetDefaultTargetTriple();
    LLVMInitializeNativeTarget();
    var target = LLVMGetFirstTarget();
    var cpu = LLVMGetHostCPUName();
    var feature = LLVMGetHostCPUFeatures();
    var target_machine =
        LLVMCreateTargetMachine(
            target,
            triple,
            cpu,
            feature,
            LLVMCodeGenLevelAggressive,
            LLVMRelocDefault,
            LLVMCodeModelDefault);
    LLVMTargetMachineEmitToFile(
        target_machine, mod, new BytePointer("./out.o"), LLVMObjectFile, error);
    LLVMDisposeErrorMessage(error);
  }
}
