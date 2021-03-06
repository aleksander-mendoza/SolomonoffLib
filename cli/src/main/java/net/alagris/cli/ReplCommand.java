package net.alagris.cli;

import net.alagris.core.*;
import net.alagris.lib.LearnLibCompatibility;
import net.alagris.lib.Solomonoff;
import org.antlr.v4.runtime.CharStreams;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

public interface ReplCommand<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>, Result> {
    Result run(Solomonoff<N, G> compiler, Consumer<String> log, Consumer<String> debug, String args) throws Exception;


}
