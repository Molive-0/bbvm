package ml.molive;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;
import org.jetbrains.annotations.NotNull;

public class Main {

  public static void main(String @NotNull [] args) throws FileNotFoundException {
    Lexer l = new Lexer(new Scanner(new File(args[0])));

    System.out.println("Interpreting file...");
    Token t = new EOF();
    ArrayList<Statement> tokenList = new ArrayList<>();
    while (true) {
      try {
        t = l.getTok();
      } catch (ParseException e) {
        System.err.println(e.toString());
        System.exit(1);
      }
      if (EOF.class.isAssignableFrom(t.getClass())) {
        break;
      }
      if (Statement.class.isAssignableFrom(t.getClass())) {
        tokenList.add((Statement) t);
      } else {
        System.err.printf("Token %s should be a statement!%n", t.toString());
        System.exit(1);
      }
    }

    HashMap<String, Integer> variableSet = new HashMap<>();
    String firstVar = "";
    for (Statement s : tokenList) {
      for (String v : s.getVariables()) {
        variableSet.putIfAbsent(v, variableSet.size());
        if (firstVar.isEmpty()) {
          firstVar = v;
        }
      }
    }

    IRconverter irc = new IRconverter(variableSet.size());
    System.out.println("Generating LLVM IR...");
    Stack<WhileLabel> whileLabelStack = new Stack<>();
    for (Statement s : tokenList) {
      if (s.getClass() == SWhile.class) {
        whileLabelStack.push(((SWhile) s).sCompile(irc, variableSet));
      } else if (s.statement.equals("end")) {
        s.sCompile(irc, whileLabelStack.pop());
      } else {
        s.compile(irc, variableSet);
      }
    }
    irc.addEOF();

    System.out.println("Running JIT compiler...");

    System.out.println(firstVar + ": " + irc.run());
  }
}
