package net.alagris;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EmptyStackException;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import net.alagris.CLI.OptimisedHashLexTransducer;
import net.alagris.thrax.ThraxParser;

public class ThraxTest {

	static class Case {
		String thrax;
		String solomonoff;
	}

	static Case c(String thrax, String solomonoff) {
		Case c = new Case();
		c.solomonoff = solomonoff.trim();
		c.thrax = thrax;
		return c;
	}

	@Test
	public void test() {
		final Case[] sources = { 
				c("x = \"a\";", "x = 'a'"), 
				c("x = \"a\":\"b\";", "x = 'a':'b'"),
				c("x = (\"a\":\"b\") | (\"e\":\"f\");", "x = 'a':'b'|'e':'f'"),
				c("x = (\"a\":\"b\")* | (\"e\":\"f\");", "x = 'a':'b'*|'e':'f'"),
				c("x = (\"a\":\"b\")? | (\"e\":\"f\");", "x = 'a':'b'?|'e':'f'"),
				c("x = \"a\"|\"b\";", "x = [a-b]"),
				c("x = \"a\"|\"b\"|\"c\"|\"d\"|\"e\";", "x = [a-e]"),
				c("x = \"a\"|\"b\"|\"c\"|\"c\"|\"c\";", "x = [a-c]"),
				c("x = \"a\"|\"c\"|\"b\";", "x = [a-c]"),
				c("x = \"[97]\"|\"[0142]\"|\"[0x63]\";", "x = [a-c]"),
				c("x = \"[97][0142][0x63]\";", "x = 'abc'"),
				c("x = \"a\" \"b\";", "x = 'ab'"),
				c("x = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\") - (\"a\"|\"a\");", "x = [b-e]"),
				c("x = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\") - (\"a\"|\"d\");", "x = [b-c]|'e'"),
				c("y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\");"
						+ "x = y - (\"a\"|\"d\");", "y = [a-e]\nx = [b-c]|'e'"),
				c("y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\")\"xxx\";"
						+ "x = y - (\"a\"|\"d\");", "y = [a-e] 'xxx'\nx = subtract[y,'a'|'d']"),
				c("x = \"a\"; y = x x x ; z = y y y ;","x = 'a'\ny = 'aaa'\nz = 'aaaaaaaaa'\n"),
				c("x = \"a\"*; y = x x x ; z = y y y ;","x = 'a'*\ny = !!x !!x x\nz = !!y !!y y\n"),
				c("x = \"a\"{5} ;","x = 'aaaaa'\n"),
				c("x = \"a\"{4,5} ;","x = 'aaaa' 'a'?\n"),
				c("x = \"a\"{3,5} ;","x = 'aaa' 'a'? 'a'?\n"),
		};
		try {
			final OptimisedHashLexTransducer specs = new OptimisedHashLexTransducer();
			for (Case c : sources) {
				try {
					ThraxParser<?, ?> parser = ThraxParser.parse(CharStreams.fromString(c.thrax), specs.specs);
					assertEquals(c.solomonoff, parser.toSolomonoff().trim());
				} catch (CompilationError | EmptyStackException e) {
					e.printStackTrace();
					fail(c.thrax);
				}
			}
		} catch (CompilationError e1) {
			e1.printStackTrace();
		}
	}
}
