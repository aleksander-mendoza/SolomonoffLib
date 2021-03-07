package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolStringFile implements Kolmogorov,Church,Solomonoff {

    @Override
    public int compositionHeight() {
        return 1;
    }

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
    }

    final String filePath;
    final int inputCol;
    final int outputCol;

    @Override
    public Kolmogorov clearOutput() {
        return new KolStringFile(filePath,inputCol,-1);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public boolean producesOutput() {
        return outputCol>=0;
    }

    @Override
    public boolean readsInput() {
        return inputCol>=0;
    }
    public KolStringFile(String filePath) {
        this(filePath,0,1);
    }
    public KolStringFile(String filePath,int inputCol,int outputCol) {
       this.filePath = filePath;
       this.inputCol = inputCol;
       this.outputCol = outputCol;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        return this;
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return IntSeq.Epsilon;
    }

    @Override
    public Kolmogorov inv() {
        return new KolStringFile(filePath,outputCol,inputCol);
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        return this;
    }

    @Override
    public void toString(StringBuilder sb) {
        if(inputCol<0){
            sb.append("''");
        }else {
            sb.append("stringFile!('path':'").append(filePath).append("','inputColumn':'").append(inputCol).append("','outputColumn':");
            if(outputCol>=0) {
                sb.append("'").append(outputCol).append("'");
            }else{
                sb.append("[]");
            }
            sb.append(")");
        }

    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {
        toString(sb);
        return Weights.str();
    }

    @Override
    public PushedBack toKolmogorov(TranslationQueries queries) {
        return PushedBack.wrap(this);
    }

    @Override
    public Church substituteCh(Function<ChVar, Church> argMap) {
        return this;
    }

    @Override
    public <Y> Y walk(SolWalker<Y> walker) {
        return null;
    }

    @Override
    public int validateSubmatches(VarQuery query) {
        return -1;
    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Kolmogorov identity() {
        return new KolIdentity(this);
    }
}
