package ml.molive;

import java.util.Arrays;
import java.util.HashMap;

public class SOneParam extends Statement {
  private final Identifier one;

  public static final String[] STATEMENTS = {
    "clear", "decr", "incr",
    // "init",
  };

  @Override
  public String[] getVariables() {
    return new String[] {one.identifier};
  }

  @Override
  public void compile(IRconverter conv, HashMap<String, Integer> map) {
    if (statement.equals("clear")) {
      conv.addClear(map.get(one.identifier));
    }
    if (statement.equals("incr")) {
      conv.addIncr(map.get(one.identifier));
    }
    if (statement.equals("decr")) {
      conv.addDecr(map.get(one.identifier));
    }
  }

  public static boolean identify(String ident) {
    return Arrays.asList(SOneParam.STATEMENTS).contains(ident.toLowerCase());
  }

  public SOneParam(String ident, Identifier one) {
    super(ident);
    this.one = one;
  }
}
