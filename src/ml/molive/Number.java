package ml.molive;

public class Number extends Token {
  protected long value;

  public static boolean identify(String ident) {
    return ident.matches("\\d+");
  }

  public Number(String ident) {
    value = Long.decode(ident);
  }

  @Override
  public String toString() {
    return "Number{" + "value=" + value + '}';
  }
}
