package ml.molive;

import java.util.Arrays;
import java.util.HashMap;

public class Statement extends Token {
  public static final String[] STATEMENTS = {
    "clear", "copy", "decr", "do", "end", "incr", "init", "not", "to", "while",
  };
  public String statement;

  public String[] getVariables() {
    return new String[0];
  }

  public void compile(IRconverter conv, HashMap<String, Integer> map) {}

  public void sCompile(IRconverter conv, WhileLabel labels) {
    conv.addEnd(labels);
  }

  public static boolean identify(String ident) {
    return Arrays.asList(Statement.STATEMENTS).contains(ident);
  }

  public Statement(String ident) {
    statement = ident;
  }

  @Override
  public String toString() {
    return "Statement{" + "statement='" + statement + '\'' + '}';
  }
}
