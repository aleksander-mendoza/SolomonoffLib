package net.alagris;

import net.alagris.MealyParser.AST;
import net.alagris.learn.__;
import net.alagris.learn.__.S;

public interface Regex extends AST {
    
    

    public static class Union implements Regex {
        final Regex lhs, rhs;

        public Union(Regex lhs, Regex rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

    }

    public static class Concat implements Regex {
        final Regex lhs, rhs;

        public Concat(Regex lhs, Regex rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
        
    }

    public static class Product implements Regex {
        final Regex nested;
        final S<Integer> output;

        public Product(Regex nested, S<Integer> output) {
            this.nested = nested;
            this.output = output;
        }
    }
    
    public static class WeightBefore implements Regex {

        private final Regex nested;
        private final int weight;

        public WeightBefore(Regex nested, int weight) {
            this.nested = nested;
            this.weight = weight;
        }


        @Override
        public String toString() {
            return " " + weight + " "
                    + (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString());
        }

    }

    public static class WeightAfter implements Regex {

        private final Regex nested;
        private final int weight;

        public WeightAfter(Regex nested, int weight) {
            this.nested = nested;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString()) + " "
                    + weight + " ";
        }

    }
    
    public static class Kleene implements Regex {
        final Regex nested;

        public Kleene(Regex nested) {
            this.nested = nested;
        }
        
    }
    
    public static class Atomic implements Regex {
        final private int from;
        final private int to;
        final private SourceCodePosition pos;

        public Atomic(int singleton, SourceCodePosition pos) {
            this(singleton,singleton,pos);
        }
        public Atomic(int from, int to, SourceCodePosition pos) {
            this.pos = pos;
            this.from = Math.min(from, to);
            this.to = Math.max(from, to);
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }

        public SourceCodePosition getPos() {
            return pos;
        }

        public static Regex fromString(S<Integer> str,SourceCodePosition pos) {
            return __.S.fold((Regex)eps, str, (ast,c)->new Concat(ast, new Atomic(c,pos)));
        }

    }
    public static class Epsilon implements Regex {
        private Epsilon() {
        }
    }
    public static Epsilon eps = new Epsilon();
    public static class Var implements Regex {
        final String id;
        final private SourceCodePosition pos;

        public Var(String id,SourceCodePosition pos) {
            this.pos = pos;
            this.id = id;
        }

        public SourceCodePosition getPos() {
            return pos;
        }

    }
}
