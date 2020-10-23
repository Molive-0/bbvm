package ml.molive;

public class Identifier extends Token {
  protected String identifier;

  public static boolean identify(String ident) {
    return ident.matches("[a-zA-Z]\\w*");
  }

  public Identifier(String ident) {
    identifier = ident;
  }

  @Override
  public String toString() {
    return "Identifier{" + "identifier='" + identifier + '\'' + '}';
  }
}
