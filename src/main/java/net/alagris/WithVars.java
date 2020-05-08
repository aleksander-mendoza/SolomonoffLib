package net.alagris;

import java.util.HashMap;
import java.util.List;

import net.alagris.MealyParser.AST;
import net.alagris.Simple.A;
import net.alagris.Simple.BacktrackContext;
import net.alagris.Simple.Concat;
import net.alagris.Simple.Eps;
import net.alagris.Simple.Ptr;
import net.alagris.Simple.Union;

public class WithVars {

    public interface V extends AST{
        
        A substituteVars(HashMap<String, A> vars);
    }

    public static class Union implements V {
        final V lhs, rhs;

        public Union(V lhs, V rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.Union(lhs.substituteVars(vars), rhs.substituteVars(vars));
        }
    }

    public static class Concat implements V {
        final V lhs, rhs;

        public Concat(V lhs, V rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
        
        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.Concat(lhs.substituteVars(vars), rhs.substituteVars(vars));
        }
    }

    public static class Product implements V {
        final V nested;
        final String output;

        public Product(V nested, String output) {
            this.nested = nested;
            this.output = output;
        }
        
        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.Product(nested.substituteVars(vars), output);
        }
    }
    
    public static class WeightBefore implements V {

        private final V nested;
        private final int weight;

        public WeightBefore(V nested, int weight) {
            this.nested = nested;
            this.weight = weight;
        }


        @Override
        public String toString() {
            return " " + weight + " "
                    + (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString());
        }


        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.WeightBefore(nested.substituteVars(vars), weight);
        }

    }

    public static class WeightAfter implements V {

        private final V nested;
        private final int weight;

        public WeightAfter(V nested, int weight) {
            this.nested = nested;
            this.weight = weight;
        }

        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.WeightAfter(nested.substituteVars(vars), weight);
        }

        @Override
        public String toString() {
            return (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString()) + " "
                    + weight + " ";
        }

    }
    
    public static class Kleene implements V {
        final V nested;

        public Kleene(V nested) {
            this.nested = nested;
        }
        
        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.Kleene(nested.substituteVars(vars));
        }
    }
    
    public static class Range implements V {
        final private int from;
        final private int to;

        public Range(int from, int to) {
            this.from = Math.min(from, to);
            this.to = Math.max(from, to);
        }
        
        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.Range(from,to);
        }
    }
    
    public static class Atomic implements V {

        private final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        public A substituteVars(HashMap<String, A> vars) {
            return new Simple.Atomic(literal);
        }
    }
    
    
    public static class Var implements V {
        final String id;

        public Var(String id) {
            this.id = id;
        }

        @Override
        public A substituteVars(HashMap<String, A> vars) {
            A value = vars.get(id);
            if(value==null)throw new IllegalStateException("Variable \""+id+"\" not found!");
            return value;
        }
    }
}
