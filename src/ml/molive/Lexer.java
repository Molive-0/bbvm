package ml.molive;

import static java.lang.String.format;

import java.text.ParseException;
import java.util.Scanner;

public class Lexer {
  private final Scanner code;

  public Lexer(Scanner input) {
    code = input;
    code.useDelimiter("\\s+");
  }

  public Token getNotFluff() throws ParseException {
    Token t;
    do {
      t = getTok();
    } while (t.getClass() == Fluff.class);
    return t;
  }

  public Token getTok() throws ParseException {
    if (!code.hasNext()) return new EOF();
    String token = code.next();
    while (token.startsWith("#")) { // handle comments
      code.useDelimiter("[\n\r]");
      code.next();
      code.useDelimiter("\\s+");
      token = code.next();
    }

    if (token.endsWith(";")) { // ignore the semicolons, they add nothing to the language.
      token = token.substring(0, token.length() - 1);
    }

    if (STwoParam.identify(token)) {
      return parseSTwoParam(token);
    } else if (SOneParam.identify(token)) {
      return parseSOneParam(token);
    } else if (SWhile.identify(token)) {
      return parseSWhile(token);
    } else if (Fluff.identify(token)) {
      return new Fluff(token);
    } else if (Statement.identify(token)) {
      return new Statement(token);
    } else if (Number.identify(token)) {
      return new Number(token);
    } else if (Identifier.identify(token)) {
      return new Identifier(token);
    }

    throw new ParseException("Token " + token + " is not valid", 0);
  }

  private STwoParam parseSTwoParam(String token) throws ParseException {
    Token paramOne = getNotFluff();
    if (paramOne.getClass() != Identifier.class) {
      throw new ParseException(
          format("Token %s should be followed by identifier, not %s", token, paramOne.toString()),
          0);
    }
    Token paramTwo = getNotFluff();
    if (paramTwo.getClass() != Identifier.class) {
      throw new ParseException(
          format("Token %s should be followed by identifier, not %s", token, paramTwo.toString()),
          0);
    }
    return new STwoParam(token, (Identifier) paramOne, (Identifier) paramTwo);
  }

  private SOneParam parseSOneParam(String token) throws ParseException {
    Token paramOne = getNotFluff();
    if (paramOne.getClass() != Identifier.class) {
      throw new ParseException(
          format("Token %s should be followed by identifier, not %s", token, paramOne.toString()),
          0);
    }
    return new SOneParam(token, (Identifier) paramOne);
  }

  private SWhile parseSWhile(String token) throws ParseException {
    Token paramOne = getNotFluff();
    if (paramOne.getClass() != Identifier.class) {
      throw new ParseException(
          format("Token %s should be followed by identifier, not %s", token, paramOne.toString()),
          0);
    }
    Token number = getNotFluff();
    if (number.getClass() != Number.class) {
      throw new ParseException(
          format("Token %s should be followed by number, not %s", token, number.toString()), 0);
    }
    return new SWhile(token, (Identifier) paramOne, (Number) number);
  }
}
