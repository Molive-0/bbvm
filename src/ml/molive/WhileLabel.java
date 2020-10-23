package ml.molive;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;

public class WhileLabel {
  public LLVMBasicBlockRef start;
  public LLVMBasicBlockRef end;

  public WhileLabel(LLVMBasicBlockRef start, LLVMBasicBlockRef end) {
    this.start = start;
    this.end = end;
  }
}
