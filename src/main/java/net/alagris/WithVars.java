package net.alagris;

import java.util.HashMap;
import java.util.List;

import org.antlr.v4.parse.GrammarTreeVisitor.astOperand_return;

import net.alagris.MealyParser.AAA;
import net.alagris.MealyParser.AST;
import net.alagris.MealyParser.Alph;
import net.alagris.MealyParser.AlphOrStruct;
import net.alagris.MealyParser.Alphabet;
import net.alagris.MealyParser.StructDef;
import net.alagris.Simple.A;
import net.alagris.Simple.Atomic;
import net.alagris.Simple.BacktrackContext;
import net.alagris.Simple.Concat;
import net.alagris.Simple.Eps;
import net.alagris.Simple.Ptr;
import net.alagris.Simple.Union;

public class WithVars {

    public interface V extends AST{
        
        A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out);

    }

    public static class Union implements V {
        final V lhs, rhs;

        public Union(V lhs, V rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            return new Simple.Union(lhs.substituteVars(vars,in,out), rhs.substituteVars(vars,in,out));
        }

    }

    public static class Concat implements V {
        final V lhs, rhs;

        public Concat(V lhs, V rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
        
        @Override
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            return new Simple.Concat(lhs.substituteVars(vars,in,out), rhs.substituteVars(vars,in,out));
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
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            if(out instanceof Alph) {
                return new Simple.Product(nested.substituteVars(vars,in,out), ((Alph) out).map(output));
            }else {
                throw new IllegalStateException("Expected struct "+out+" as output but found string \""+output+"\"");
            }
        }
    }
    
    public static class ProductStruct implements V {
        final V nested;
        final StructLiteral output;

        public ProductStruct(V nested, StructLiteral output) {
            this.nested = nested;
            this.output = output;
        }
        
        @Override
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            if(out instanceof MealyParser.Struct) {
                return new Simple.Product(nested.substituteVars(vars,in,out), ((MealyParser.Struct) out).compileOutput(output.members));
            }else {
                throw new IllegalStateException("Expected struct "+out+" as output but found string \""+output+"\"");
            }
        }
    }
    
    public static class StructLiteral implements MealyParser.AST{
        final HashMap<String, String> members = new HashMap<>();

        public StructLiteral() {
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
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            return new Simple.WeightBefore(nested.substituteVars(vars,in,out), weight);
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
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            return new Simple.WeightAfter(nested.substituteVars(vars,in,out), weight);
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
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            return new Simple.Kleene(nested.substituteVars(vars,in,out));
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
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            
            if(in instanceof Alph) {
                return new Simple.Range(((Alph) in).map(from),((Alph) in).map(to));
            }else {
                throw new IllegalStateException("Expected struct "+in+" but found range ["+(char)from+"-"+(char)to+"]");
            }
            
        }
    }
    
    public static class Atomic implements V {

        private final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            if(in instanceof Alph) {
                return new Simple.Atomic(((Alph) in).map(literal));
            }else {
                throw new IllegalStateException("Expected struct "+in+" but found string \""+literal+"\"");
            }
            
        }
    }
    
    public static class Struct implements V {

        HashMap<String, V> members = new HashMap<>();

        public Struct() {
        }

        @Override
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            if(in instanceof MealyParser.Struct) {
                return ((MealyParser.Struct)in).compile(members,vars,out);
            }else {
                throw new IllegalStateException("Expected string from alphabet "+in+" but found struct "+members);
            }
        }
    }
    
    public static class LiteralStruct implements V {

        V mainMemeber;

        public LiteralStruct(V mainMemeber) {
            this.mainMemeber = mainMemeber;
        }

        @Override
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            if(in instanceof MealyParser.Struct) {
                return ((MealyParser.Struct)in).compile(mainMemeber,vars,out);
            }else {
                throw new IllegalStateException("Expected string from alphabet "+in+" but found struct {"+mainMemeber+"}");
            }
        }
        
        @Override
        public String toString() {
            return "{"+mainMemeber+"}";
        }
    }
    
    
    public static class Var implements V {
        final String id;

        public Var(String id) {
            this.id = id;
        }

        @Override
        public A substituteVars(HashMap<String, AAA> vars, AlphOrStruct in,AlphOrStruct out) {
            AAA value = vars.get(id);
            if(value==null)throw new IllegalStateException("Variable \""+id+"\" not found!");
            if(value.in!=in)throw new IllegalStateException("Function "+value+" cannot be called! Can only call functions "+in.name()+"->"+out.name()+" in this context!");
            return value.ast;
        }
    }
}
