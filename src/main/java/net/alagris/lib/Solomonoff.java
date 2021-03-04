package net.alagris.lib;

import net.alagris.SolomonoffGrammarLexer;
import net.alagris.SolomonoffGrammarParser;
import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

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
    }

    public void addAllExternalFunctionsFromSolomonoff() {
        ExternalFunctionsFromSolomonoff.addExternalDict(specs);
        ExternalFunctionsFromSolomonoff.addExternalImport(specs);
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
        ExternalFunctionsFromSolomonoff.addExternalActiveLearningFromDataset(specs);
        ExternalFunctionsFromSolomonoff.addExternalOSTIAWithDomain(specs);
    }

    public Solomonoff(LexUnicodeSpecification<N, G> specs,Config config) throws CompilationError {
        this.specs = specs;
        listener = specs.makeParser();
        listener.addDotAndHashtag();
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
        final IntSeq out = run(name, new IntSeq(input));
        return out == null ? null : IntSeq.toUnicodeString(out);
    }

    public IntSeq run(String name, IntSeq input) {
        return specs.evaluate(getOptimalTransducer(name), input);
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
