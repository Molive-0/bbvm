package ml.molive;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;
import org.jetbrains.annotations.NotNull;

public class Main {

  public static void main(String @NotNull [] args) throws IOException {
    long startTime = System.nanoTime();

    if (args.length == 0) {
      System.err.println("No args!");
      System.exit(-1);
    }

    boolean compile = false;
    String filename;
    if (args.length > 1) {
      compile = args[0].equals("-c");
      filename = args[1];
    } else {
      filename = args[0];
    }
    Lexer l = new Lexer(new Scanner(new File(filename)));

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
    ArrayList<String> variableList = new ArrayList<>();
    for (Statement s : tokenList) {
      for (String v : s.getVariables()) {
        if (!variableSet.containsKey(v)) {
          variableSet.put(v, variableSet.size());
          variableList.add(v);
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
    irc.addEOF(variableList);

    long endTime1 = System.nanoTime();

    System.out.printf(
        "LLVM IR compile took %d nanoseconds (%d milliseconds).\n",
        endTime1 - startTime, (endTime1 - startTime) / 1000000);

    if (compile) {
      System.out.println("Running normal compiler...");

      irc.dumpCode();
      /*try {
        Runtime.getRuntime().exec(new String[] {"LINK.exe", "start"});
      } catch (Exception e) {
        System.err.println("Something went wrong there. Do you have LINK installed?");
        System.err.println("Go to https://visualstudio.microsoft.com/downloads/?q=build+tools");
        System.err.println("and run Tools for Visual Studio, and then install C++ tools.");
      }*/
      System.out.println("A file called a.out has been made.");
      System.out.println("...you'll have to link it yourself :(");
    } else {

      System.out.println("Running JIT compiler...");

      irc.run();

      long endTime2 = System.nanoTime();
      System.out.printf(
          "LLVM IR execution took %d nanoseconds (%d milliseconds).\n",
          endTime2 - endTime1, (endTime2 - endTime1) / 1000000);
    }
  }
}
