package net.alagris.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        public Iterable<Pair<O, O>> filterOutNegative() {
            return ()->this.stream().filter(p->p.r()!=null).iterator();
        }
    }
    static int getExpectSingleInt(Pos pos,HashMap<String,ArrayList<String>> args, String key, int defaultVal) throws CompilationError.ParseException{
        final String val = getExpectSingleString(pos,args,key,null);
        if(val==null)return defaultVal;
        try{
            return Integer.parseInt(val);
        }catch (NumberFormatException e){
            throw new CompilationError.ParseException(pos,"Invalid number "+val+" for key "+key);
        }
    }
    static long getExpectSingleLong(Pos pos,HashMap<String,ArrayList<String>> args, String key, long defaultVal) throws CompilationError.ParseException{
        final String val = getExpectSingleString(pos,args,key,null);
        if(val==null)return defaultVal;
        try{
            return Long.parseLong(val);
        }catch (NumberFormatException e){
            throw new CompilationError.ParseException(pos,"Invalid number "+val+" for key "+key);
        }
    }
    static double getExpectSingleDouble(Pos pos,HashMap<String,ArrayList<String>> args, String key, double defaultVal) throws CompilationError.ParseException{
        final String val = getExpectSingleString(pos,args,key,null);
        if(val==null)return defaultVal;
        try{
            return Double.parseDouble(val);
        }catch (NumberFormatException e){
            throw new CompilationError.ParseException(pos,"Invalid floating point "+val+" for key "+key);
        }
    }
    static boolean getExpectSingleBoolean(Pos pos,HashMap<String,ArrayList<String>> args, String key, boolean defaultVal) throws CompilationError.ParseException{
        final String val = getExpectSingleString(pos,args,key,null);
        if(val==null)return defaultVal;
        try{
            return Boolean.parseBoolean(val);
        }catch (NumberFormatException e){
            throw new CompilationError.ParseException(pos,"Invalid bool "+val+" for key "+key);
        }
    }
    static int getExpectSingleCodepoint(Pos pos,HashMap<String,ArrayList<String>> args, String key, int defaultVal) throws CompilationError.ParseException{
        final String val = getExpectSingleString(pos,args,key,null);
        if(val==null)return defaultVal;
        if(val.codePointCount(0,val.length())!=1){
            throw new CompilationError.ParseException(pos,"Only a single symbol expected for key "+key+" but got '"+val+"'");
        }
        return val.codePointAt(0);
    }
    static String getExpectSingleString(Pos pos,HashMap<String,ArrayList<String>> args, String key, String defaultVal) throws CompilationError.ParseException{
        final ArrayList<String> val = args.get(key);
        if(val==null||val.isEmpty())return defaultVal;
        if(val.size()>1){
            throw new CompilationError.ParseException(pos,"Duplicate key "+key);
        }
        final String v = val.get(0);
        assert v!=null;
        return v;
    }
    static <G> HashMap<String,ArrayList<String>> assertOnWhitelist(Pos pos,HashMap<String,ArrayList<String>> args, String... whitelist) throws CompilationError.ParseException {
        for(String key: args.keySet()){
            if(!Util.exists(whitelist, key::equals)){
                throw new CompilationError.ParseException(pos,"Unrecognized key "+key+", expected one of "+whitelist);
            }
        }
        return args;
    }
    static <G> HashMap<String,ArrayList<String>> parseArgsFromInformant(Pos pos, Informant<G, IntSeq> informant,String... whitelist) throws CompilationError.ParseException {
        return assertOnWhitelist(pos,parseArgsFromInformant(informant),whitelist);
    }
    static <G> HashMap<String,ArrayList<String>> parseArgsFromInformant(Informant<G, IntSeq> informant) {
        final HashMap<String,ArrayList<String>> args = new HashMap<>();
        for(Pair<IntSeq, IntSeq> i:informant){
            final String key = IntSeq.toUnicodeString(i.l());
            final String val = IntSeq.toUnicodeString(i.r());
            args.computeIfAbsent(key,k->new ArrayList<>()).add(val);
        }
        return args;
    }
    public static <G, O> G expectReference(Pos pos, int operandIndex, List<FuncArg<G, O>> arg) throws CompilationError{
        if(operandIndex>=arg.size()){
            throw new CompilationError.NotEnoughOperands(pos,operandIndex,FuncArg.VarRef.class,arg.size());
        }
        return FuncArg.expectReference(pos,operandIndex,arg.get(operandIndex));
    }
    public static <G, O> G expectReference(Pos pos, int operandIndex, FuncArg<G, O> arg) throws CompilationError{
        if (arg instanceof FuncArg.VarRef) {
            return ((VarRef<G, O>) arg).reference;
        } else {
            throw new CompilationError.IllegalOperandType(pos, operandIndex, FuncArg.VarRef.class, arg.getClass());
        }
    }
    public static <G, O> G expectAutomaton(Pos pos, int operandIndex, List<FuncArg<G, O>> arg) throws CompilationError {
        if(operandIndex>=arg.size()){
            throw new CompilationError.NotEnoughOperands(pos,operandIndex,FuncArg.Expression.class,arg.size());
        }
        return FuncArg.expectAutomaton(pos,operandIndex,arg.get(operandIndex));
    }
    public static <G, O> G expectAutomaton(Pos pos, int operandIndex, FuncArg<G, O> arg) throws CompilationError {
        if (arg instanceof FuncArg.Expression) {
            return ((FuncArg.Expression<G, O>) arg).expr;
        } else {
            throw new CompilationError.IllegalOperandType(pos, operandIndex, FuncArg.Expression.class, arg.getClass());
        }
    }
    public static <G, O> FuncArg.Informant<G, O> expectInformant(Pos pos, int operandIndex, List<FuncArg<G, O>> arg) throws CompilationError {
        if(operandIndex>=arg.size()){
            throw new CompilationError.NotEnoughOperands(pos,operandIndex,FuncArg.Informant.class,arg.size());
        }
        return FuncArg.expectInformant(pos,operandIndex,arg.get(operandIndex));
    }
    public static <G, O> FuncArg.Informant<G, O> expectInformant(Pos pos, int operandIndex, FuncArg<G, O> arg) throws CompilationError {
        if (arg instanceof FuncArg.Informant) {
            return (Informant<G, O>) arg;
        } else {
            throw new CompilationError.IllegalOperandType(pos, operandIndex, FuncArg.Informant.class, arg.getClass());
        }
    }

    public static <G, O> FuncArg.Informant<G, O> unaryInformantFunction(Pos pos, ArrayList<FuncArg<G, O>> args) throws CompilationError {
        if (args.size() != 1) {
            throw new CompilationError.TooManyOperands(pos, args, 1);
        }
        if (args.get(0) instanceof FuncArg.Informant) {
            return (FuncArg.Informant<G, O>) args.get(0);
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.Informant.class, args.get(0).getClass());
        }
    }

    public static <G, O> G unaryAutomatonFunction(Pos pos, ArrayList<FuncArg<G, O>> args) throws CompilationError {
        if (args.size() != 1) {
            throw new CompilationError.TooManyOperands(pos, args, 1);
        }
        if (args.get(0) instanceof FuncArg.Expression) {
            return ((FuncArg.Expression<G, O>) args.get(0)).expr;
        } else {
            throw new CompilationError.IllegalOperandType(pos, 0, FuncArg.Expression.class, args.get(0).getClass());
        }
    }

    public static <G, O> G unaryBorrowedAutomatonFunction(Pos pos, int operandIndex,ArrayList<FuncArg<G, O>> args) throws CompilationError {
        if (args.size() != 1) {
            throw new CompilationError.TooManyOperands(pos, args, 1);
        }
        if (args.get(0) instanceof FuncArg.VarRef) {
            return ((FuncArg.VarRef<G, O>) args.get(0)).reference;
        } else {
            throw new CompilationError.IllegalOperandType(pos, operandIndex, FuncArg.VarRef.class, args.get(0).getClass());
        }
    }

    public static <G> FuncArg.Informant<G,IntSeq> loadInformantFromFile(File f,String separator) throws FileNotFoundException {
        final Informant<G,IntSeq> a = new Informant<>();
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNextLine()) {
                a.add(parseDatasetLine(sc.nextLine(),separator));
            }
        }
        return a;
    }

    public static Pair<IntSeq,IntSeq> parseDatasetLine(String line,String separator){
        final int sep = line.indexOf(separator);
        if(sep==-1){
            return Pair.of(new IntSeq(line), null);
        }else{
            final IntSeq in = new IntSeq(line.substring(0,sep));
            final IntSeq out =new IntSeq(line.substring(sep+separator.length()));
            return Pair.of(in, out);
        }

    }


}
