package ml.molive;

import java.util.Arrays;
import java.util.HashMap;

public class STwoParam extends Statement {
  public Identifier one;
  public Identifier two;

  public static final String[] STATEMENTS = {
    "copy",
  };

  @Override
  public String[] getVariables() {
    return new String[] {one.identifier, two.identifier};
  }

  @Override
  public void compile(IRconverter conv, HashMap<String, Integer> map) {
    if (statement.equals("copy")) {
      conv.addCopy(map.get(one.identifier), map.get(two.identifier));
    }
  }

  public static boolean identify(String ident) {
    return Arrays.asList(STwoParam.STATEMENTS).contains(ident.toLowerCase());
  }

  public STwoParam(String ident, Identifier one, Identifier two) {
    super(ident);
    this.one = one;
    this.two = two;
  }
}
