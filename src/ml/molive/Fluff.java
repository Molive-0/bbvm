package ml.molive;

import java.util.Arrays;

public class Fluff extends Statement {
  public static final String[] STATEMENTS = {
    "do", "not", "to",
  };

  public static boolean identify(String ident) {
    return Arrays.asList(Fluff.STATEMENTS).contains(ident.toLowerCase());
  }

  @Override
  public String toString() {
    return "Fluff{" + "statement='" + statement + '\'' + '}';
  }

  public Fluff(String ident) {
    super(ident);
  }
}
