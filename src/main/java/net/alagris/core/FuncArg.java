package net.alagris.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public interface FuncArg<G, O> {


    public static class VarRef<G, O> implements FuncArg<G, O> {
        public final G reference;

        public VarRef(G reference) {
            this.reference = reference;
        }
    }

    public static class Expression<G, O> implements FuncArg<G, O> {
        public final G expr;

        public Expression(G expr) {
            this.expr = expr;
        }
    }

    public static class Informant<G, O> extends ArrayList<Pair<O, O>> implements FuncArg<G, O> {
        public Informant(int size) {
            super(size);
        }
        public Informant() {
            super();
        }
    }


    static <G> HashMap<String,String> parseArgsFromInformant(Pos pos,Informant<G, IntSeq> informant, String... args) throws CompilationError.ParseException {
        assert args.length%2==0;
        final HashMap<String,String> out = new HashMap<>(args.length/2);
        for(int i=0;i<args.length/2;i++){
            out.put(args[2*i],args[2*i+1]);
        }
        for(Pair<IntSeq, IntSeq> i:informant){
            final String key = IntSeq.toUnicodeString(i.l());
            final String val = IntSeq.toUnicodeString(i.r());
            if(out.containsKey(key)){
                out.put(key,val);
            }else{
                throw new CompilationError.ParseException(pos,"Unrecognized key "+key+", expected one of "+out.keySet());
            }
        }
        return out;
    }

    public static <G, O> G expectReference(Pos pos, FuncArg<G, O> arg) throws CompilationError.IllegalOperandsNumber, CompilationError.IllegalOperandType {
        if (arg instanceof FuncArg.VarRef) {
            return ((VarRef<G, O>) arg).reference;
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.VarRef.class, arg.getClass());
        }
    }

    public static <G, O> G expectAutomaton(Pos pos, FuncArg<G, O> arg) throws CompilationError.IllegalOperandsNumber, CompilationError.IllegalOperandType {
        if (arg instanceof FuncArg.Expression) {
            return ((FuncArg.Expression<G, O>) arg).expr;
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.Expression.class, arg.getClass());
        }
    }

    public static <G, O> FuncArg.Informant<G, O> expectInformant(Pos pos, FuncArg<G, O> arg) throws CompilationError.IllegalOperandsNumber, CompilationError.IllegalOperandType {
        if (arg instanceof FuncArg.Informant) {
            return (Informant<G, O>) arg;
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.Informant.class, arg.getClass());
        }
    }

    public static <G, O> FuncArg.Informant<G, O> unaryInformantFunction(Pos pos, ArrayList<FuncArg<G, O>> args) throws CompilationError.IllegalOperandsNumber, CompilationError.IllegalOperandType {
        if (args.size() != 1) {
            throw new CompilationError.IllegalOperandsNumber(pos, args, 1);
        }
        if (args.get(0) instanceof FuncArg.Informant) {
            return (FuncArg.Informant<G, O>) args.get(0);
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.Informant.class, args.get(0).getClass());
        }
    }

    public static <G, O> G unaryAutomatonFunction(Pos pos, ArrayList<FuncArg<G, O>> args) throws CompilationError.IllegalOperandsNumber, CompilationError.IllegalOperandType {
        if (args.size() != 1) {
            throw new CompilationError.IllegalOperandsNumber(pos, args, 1);
        }
        if (args.get(0) instanceof FuncArg.Expression) {
            return ((FuncArg.Expression<G, O>) args.get(0)).expr;
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.Expression.class, args.get(0).getClass());
        }
    }

    public static <G, O> G unaryBorrowedAutomatonFunction(Pos pos, ArrayList<FuncArg<G, O>> args) throws CompilationError.IllegalOperandsNumber, CompilationError.IllegalOperandType {
        if (args.size() != 1) {
            throw new CompilationError.IllegalOperandsNumber(pos, args, 1);
        }
        if (args.get(0) instanceof FuncArg.VarRef) {
            return ((FuncArg.VarRef<G, O>) args.get(0)).reference;
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.VarRef.class, args.get(0).getClass());
        }
    }

    public static <G> FuncArg.Informant<G,IntSeq> loadInformantFromFile(File f,String separator) throws FileNotFoundException {
        final Informant<G,IntSeq> a = new Informant<>();
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNextLine()) {
                final String[] parts = sc.nextLine().split(separator, 2);
                if (parts.length == 0) continue;
                final IntSeq in = new IntSeq(parts[0]);
                final IntSeq out = parts.length == 2 ? new IntSeq(parts[1]) : null;
                a.add(Pair.of(in, out));
            }
        }
        return a;
    }

}
