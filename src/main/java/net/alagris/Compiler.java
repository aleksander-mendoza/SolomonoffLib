package net.alagris;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import org.antlr.v4.runtime.CharStreams;

import net.alagris.MealyParser.AAA;
import net.alagris.MealyParser.Funcs;
import net.alagris.Simple.A;
import net.alagris.Simple.Eps;

public class Compiler {
    static Funcs funcs = null;
    static HashMap<String, AAA> ctx = null;
    static HashMap<String, Eps> eps = null;
    static HashMap<String, Mealy> automata = null;

    static Eps computeGlushkov(String funcName) {
        Eps e = eps.get(funcName);
        if (e == null && ctx.containsKey(funcName)) {
            compute(funcName);
            return eps.get(funcName);
        }
        return e;

    }

    static Mealy compute(String funcName) {
        return automata.computeIfAbsent(funcName, k -> {
            AAA ast = ctx.get(funcName);
            Simple.Ptr<Integer> ptr = new Simple.Ptr<>(0);
            final Eps epsilonFree = ast.ast.removeEpsilons(ptr);
            eps.put(funcName, epsilonFree);
            return epsilonFree.glushkov(ptr,ast.in,ast.out);
        });
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Welcome! Type \":load path/to/file\" to get started. Type \":help\" for help.");
        while (sc.hasNext()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty())
                continue;
            if (line.startsWith("#"))
                continue;

            String[] parts = line.split("\\s+", 2);
            if (parts[0].startsWith(":")) {

                switch (parts[0]) {
                case ":load":
                    try {
                        funcs = MealyParser.parse(CharStreams.fromFileName(parts[1]));
                        ctx = MealyParser.eval(funcs);
                        eps = new HashMap<>();
                        automata = new HashMap<>();
                        System.out.println("Success!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case ":t": {
                    AAA ast = ctx.get(parts[1]);
                    if (ast == null) {
                        System.err.println("undefined");
                    } else {
                        System.out.println(ast.ast);
                    }
                    break;
                }
                case ":ast": {
                    AAA ast = ctx.get(parts[1]);
                    if (ast == null) {
                        System.err.println("undefined");
                    } else {
                        System.out.println(ast.ast.toString());
                    }
                    break;
                }
                case ":ls":
                    System.out.println(ctx.keySet());
                    break;
                case ":help":
                    System.out.println("Commands\n"
                            + " :help\n"
                            + " :ls - lists defined functions\n"
                            + " :ast <functionName> - shows abstract syntax tree of function\n"
                            + " :g <functionName> - shows ast after glushkov algoritms\n"
                            + " :a <functionName> - shows produced automaton\n"
                            + "Usage\n"
                            + " <functionName> <stringLiteral>\n"
                            + "Example\n"
                            + " f \"sample string literal\"");
                    
                    break;
                case ":g": {
                    Eps e = computeGlushkov(parts[1]);
                    if (e == null) {
                        System.err.println("undefined");
                    } else {
                        System.out.println(e.toString());
                    }
                    break;
                }
                case ":a": {
                    Mealy e = compute(parts[1]);
                    if (e == null) {
                        System.err.println("undefined");
                    } else {
                        System.out.println(e.toString());
                    }
                    break;
                }
                }

            } else {
                if (automata == null) {
                    System.err.println("Firts load some file with \":load path/to/file\"!");
                    continue;
                }
                if (parts[1].startsWith("\"") && parts[1].endsWith("\"") && parts[1].length() >= 2) {
                    String literal = parts[1].substring(1, parts[1].length() - 1);
                    Mealy automaton = compute(parts[0]);
                    IntArrayList out = automaton.evaluate(literal);
                    System.out.println(out);
                    System.out.println(out.toArrString());

                }
            }
        }
    }
}
