package net.alagris.lib;

import net.alagris.SolomonoffGrammarLexer;
import net.alagris.SolomonoffGrammarParser;
import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Arrays;

import static net.alagris.core.LexUnicodeSpecification.*;

/**
 * Simple implementation of command-line interface for the compiler
 */
public class Solomonoff<N, G extends IntermediateGraph<Pos, E, P, N>> {
    public final LexUnicodeSpecification<N, G> specs;
    public final ParserListener<Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> listener;
    public final SolomonoffGrammarParser parser;


    public void addAllExternalFunctionsFromLearnLib() {
        ExternalFunctionsFromLearnLib.addExternalRPNI(specs);
        ExternalFunctionsFromLearnLib.addExternalRPNI_EDSM(specs);
        ExternalFunctionsFromLearnLib.addExternalRPNI_EMDL(specs);
        ExternalFunctionsFromLearnLib.addExternalRPNI_Mealy(specs);
    }

    public void addAllExternalPipelineFunctionsFromSolomonoff() {
        ExternalFunctionsFromSolomonoff.addExternalExtractGroup(specs);
        ExternalFunctionsFromSolomonoff.addExternalLowercase(specs);
        ExternalFunctionsFromSolomonoff.addExternalUppercase(specs);
        ExternalFunctionsFromSolomonoff.addExternalReverse(specs);
        ExternalFunctionsFromSolomonoff.addExternalAdd(specs);
        ExternalFunctionsFromSolomonoff.addExternalPipelineImport(specs);
    }

    public void addAllExternalFunctionsFromSolomonoff() {
        ExternalFunctionsFromSolomonoff.addExternalDict(specs);
        ExternalFunctionsFromSolomonoff.addExternalImport(specs);
        ExternalFunctionsFromSolomonoff.addExternalImportATT(specs);
        ExternalFunctionsFromSolomonoff.addExternalParseATT(specs);
        ExternalFunctionsFromSolomonoff.addExternalStringFile(specs);
        ExternalFunctionsFromSolomonoff.addExternalDropEpsilon(specs);
        ExternalFunctionsFromSolomonoff.addExternalCompose(specs);
        ExternalFunctionsFromSolomonoff.addExternalCompress(specs);
        ExternalFunctionsFromSolomonoff.addExternalInverse(specs);
        ExternalFunctionsFromSolomonoff.addExternalSubtract(specs);
        ExternalFunctionsFromSolomonoff.addExternalSubtractNondet(specs);
        ExternalFunctionsFromSolomonoff.addExternalLongerMatchesHigherWeights(specs);
        ExternalFunctionsFromSolomonoff.addExternalReweight(specs);
        ExternalFunctionsFromSolomonoff.addExternalRandom(specs);
        ExternalFunctionsFromSolomonoff.addExternalClearOutput(specs);
        ExternalFunctionsFromSolomonoff.addExternalIdentity(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIA(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAMaxOverlap(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAMaxDeepOverlap(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAMaxCompatible(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAMaxCompatibleInputs(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAMaxCompatibleInputsAndOutputs(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAConservative(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIACompress(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIADeepCompress(specs);
        ExternalFunctionsFromSolomonoff.addExternalActiveLearningFromDataset(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAWithDomain(specs);
    }

    public Solomonoff(LexUnicodeSpecification<N, G> specs,Config config)  {
        this.specs = specs;
        listener = specs.makeParser();
        try {
            listener.addDotAndHashtag();
        } catch (CompilationError compilationError) {
            compilationError.printStackTrace();//should never happen
            assert false:"An exception was thrown that should never normally happen";
        }
        if(config.useStandardLibrary){
            addAllExternalFunctionsFromSolomonoff();
            addAllExternalPipelineFunctionsFromSolomonoff();
        }
        if(config.useLearnLib)addAllExternalFunctionsFromLearnLib();
        parser = ParserListener.makeParser(null);
    }

    public void setInput(CharStream source) {
        parser.setTokenStream(new CommonTokenStream(new SolomonoffGrammarLexer(source)));
    }

    public void parse(CharStream source) throws CompilationError {
        setInput(source);
        listener.runCompiler(parser);
    }

    public void parseREPL(CharStream source) throws CompilationError {
        setInput(source);
        listener.runREPL(parser);
    }

    public void checkStrongFunctionality() throws CompilationError {
        checkStrongFunctionalityOfVariables();
    }

    public void checkStrongFunctionalityOfVariables() throws CompilationError {
        for (Var<N, G> var : specs.variableAssignments.values()) {
            specs.checkFunctionality(specs.getOptimised(var), var.pos);
        }
    }

    public String run(String name, String input) {
        return IntSeq.toUnicodeString(run(name, new IntSeq(input)));
    }

    public String runPipeline(String name, String input) {
        return IntSeq.toUnicodeString(runPipeline(name, new IntSeq(input)));
    }

    public String runTabular(String name, String input, byte[] stateToIndex, int[] outputBuffer) {
        final RangedGraph<Pos, Integer, E, P> g = getOptimalTransducer(name);
        return specs.evaluateTabularReturnStr(g, stateToIndex, outputBuffer, g.initial, new IntSeq(input));
    }

    public String runTabularPipeline(String name, String input, byte[] stateToIndex, int[] outputBuffer) {
        return IntSeq.toUnicodeString(runTabularPipeline(name, new IntSeq(input), stateToIndex,outputBuffer));
    }

    public IntSeq runTabular(String name, IntSeq input, byte[] stateToIndex, int[] outputBuffer) {
        final RangedGraph<Pos, Integer, E, P> g = getOptimalTransducer(name);
        return specs.evaluateTabularReturnCopy(g, stateToIndex, outputBuffer, g.initial, input);
    }

    public Seq<Integer> runTabularPipeline(String name, IntSeq input, byte[] stateToIndex, int[] outputBuffer) {
        final Pipeline<Pos, Integer, E, P, N, G> g = getPipeline(name);
        return specs.evaluateTabular(g, input, stateToIndex, outputBuffer);
    }

    public IntSeq run(String name, IntSeq input) {
        return specs.evaluate(getOptimalTransducer(name), input);
    }

    public Seq<Integer> runPipeline(String name, IntSeq input) {
        return specs.evaluate(getPipeline(name), input);
    }

    public Var<N, G> getTransducer(String id) {
        // Parsing is already over, so the user might as well mutate it and nothing bad
        // will happen
        // All variables that were meant to be used as building blocks for other
        // transducers have
        // already been either copied or consumed. In the worst case, user might just
        // try to get consumed
        // variable and get null.
        return specs.borrowVariable(id);
    }

    public RangedGraph<Pos, Integer, E, P> getOptimalTransducer(String name) {
        final Var<N, G> v = specs.borrowVariable(name);
        return v == null ? null : v.getOptimal();
    }

    public RangedGraph<Pos, Integer, E, P> getOptimisedTransducer(String name)
            throws CompilationError {
        final Var<N, G> v = specs.borrowVariable(name);
        return v == null ? null : specs.getOptimised(v);
    }

    /**
     * @param name should not contain the @ sign as it is already implied by this
     *             methods
     */
    public Pipeline<Pos,Integer,E,P,N, G> getPipeline(String name) {
        return specs.getPipeline(name);
    }





}
