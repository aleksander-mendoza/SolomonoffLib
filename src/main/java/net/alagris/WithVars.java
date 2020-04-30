package net.alagris;

import java.util.HashMap;

import net.alagris.MealyParser.AST;
import net.alagris.Simple.A;

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
