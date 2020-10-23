package ml.molive;

import java.util.HashMap;

public class SWhile extends Statement {

  public Identifier param;
  public Number num;

  @Override
  public String[] getVariables() {
    return new String[] {param.identifier};
  }

  public WhileLabel sCompile(IRconverter conv, HashMap<String, Integer> map) {
    return conv.addWhile(map.get(param.identifier), num.value);
  }

  public static boolean identify(String ident) {
    return ident.equals("while");
  }

  public SWhile(String ident, Identifier identifier, Number num) {
    super(ident);
    param = identifier;
    this.num = num;
  }
}
