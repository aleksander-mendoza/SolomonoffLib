package net.alagris.cli;

import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.core.Pipeline;
import net.alagris.core.Pos;
import net.alagris.core.Specification;
import net.alagris.lib.LearnLibCompatibility;
import org.github.jamm.MemoryMeter;

public class CommandsFromJamm {
	private CommandsFromJamm() {
	}

	private static MemoryMeter METER_SINGLETONE;

	private static MemoryMeter getMeter() {
		if (METER_SINGLETONE == null) {
			METER_SINGLETONE = new MemoryMeter();
		}
		return METER_SINGLETONE;
	}

	public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> ReplCommand<N, G, String> replMem() {
		return (compiler, logs, debug, args) -> {
			args = args.trim();
			if (args.startsWith("@")) {
				final Pipeline<Pos, Integer, E, P, N, G> r = compiler.getPipeline(args.substring(1));
				if (r == null) {
					return "No such pipeline!";
				} else {
					final long size = +getMeter().measureDeep(r);
					return size + " bytes";
				}
			} else {
				final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> r = compiler
						.getOptimisedTransducer(args);
				if (r == null) {
					return "No such function!";
				} else {
					final long size = +getMeter().measureDeep(r);
					return size + " bytes";
				}
			}
		};
	}
}
