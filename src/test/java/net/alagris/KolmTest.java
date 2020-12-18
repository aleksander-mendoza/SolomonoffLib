package net.alagris;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EmptyStackException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import net.alagris.CLI.OptimisedHashLexTransducer;
import net.alagris.ast.KolmogorovParser;
import net.alagris.ast.Solomonoff;
import net.alagris.ast.ThraxParser.InterV;

public class KolmTest {

	static class Case {
		String kolm;
		String solomonoff;
	}

	static Case c(String kolm, String solomonoff) {
		Case c = new Case();
		c.solomonoff = solomonoff.trim();
		c.kolm = kolm;
		return c;
	}

	@Test
	public void test() {
		final Case[] sources = {
				//
				c("f = 'x':'y'","oiof = 'x':'y'"),
				c("f = 'x':'y'|'y':'z'","oiof = 'x':'y'|'y':'z' 1"),
				c("f = 'x':'y'|'y':'z'|'u':'v'","oiof = 'x':'y'|'y':'z' 1|'u':'v' 2"),
				c("f = 'x':'y'|'x':'z'|'x':'x'","oiof = 'x':'y'|'x':'z' 1|'x':'x' 2"),
				c("f = 'x':'y'|('x':'z'|'x':'x')","oiof = 'x':'y'|'x':'z' 1|'x':'x' 2"),
				c("f = ('x':'y'|'x':'z')|'x':'x'","oiof = 'x':'y'|'x':'z' 1|'x':'x' 2"),
				c("f = ('x':'y'|'x':'z')|('x':'x'|'x':'u')","oiof = 'x':'y'|'x':'z' 1|'x':'x' 2|'x':'u' 3"),
				c("f = 'x':'y'|('x':'z'|('x':'x'|'x':'u'))","oiof = 'x':'y'|'x':'z' 1|'x':'x' 2|'x':'u' 3"),
				c("f = (('x':'y'|'x':'z')|'x':'x')|'x':'u'","oiof = 'x':'y'|'x':'z' 1|'x':'x' 2|'x':'u' 3"),
				c("f = 'a':'b' 'c':'d'","oiof = 'ac':'bd'"),
				c("f = [a-k]:'b' [l-z]:'d'","oiof = [a-k] [l-z]:'bd'"),
				c("f = [a-k]:'b' | [l-z]:'d'","oiof = [a-k]:'b'|[l-z]:'d' 1"),
				c("f = [a-k] | [l-z]","oinf = [a-z]"),
				c("f = [a-k] | [l-z]","oinf = [a-z]"),
				c("f = ([a-k] | [l-z])*","oinf = [a-z] 1*"),
				c("f = [a-k]* [a-k]* [a-k]*","oinf = [a-k] 1* -1 [a-k] 1* -1 [a-k] 1*"),
				c("g = 'x' f = g","oing = 'x'\noinf = 'x'"),
				c("g = 'x'* f = g",
						"oing = 'x' 1*\n" + 
						"oinf = !!oing"),
				c("g = 'x'* f = g h = f",
						"oing = 'x' 1*\n" + 
						"oinf = !!oing\n" + 
						"oinh = !!oing"),
				c("g = 'a':'b' h = g^-1",
						"oiog = 'a':'b'\n" + 
						"oioh = :'a' 'b'"),
				c("g = ('a':'b')* :('a'|'b'|[x-z]'y') h = g g^-1 ","oiog = ('a':'b') 1*:'xy'\n" + 
						"iiog = (:'a' 'b') 1* ([a-b]|[x-z] 'y' 1)\n" + 
						"oioh = !!oiog -1 iiog"),
				c("g = 'x' h = g^10",
						"oing = 'x'\n" + 
						"oinh = 'xxxxxxxxxx'"),
				c("g = 'x':'y' h = (g^-1)^-1",
						"oiog = 'x':'y'\n" + 
						"oioh = 'x':'y'"),
				
		};
		try {
			for (Case c : sources) {
				try {
					final KolmogorovParser specs = KolmogorovParser.parse(null, CharStreams.fromString(c.kolm));
					final Map<String, InterV<Solomonoff>> sol = specs.toSolomonoff();
					final String str = Solomonoff.toStringAutoWeightsAndAutoExponentials(sol);
					assertEquals(c.solomonoff, str.trim(), c.kolm);
				} catch (Throwable e) {
					e.printStackTrace();
					System.out.println(c.kolm+"\n"+e);
					throw e;
				}
			}
		} catch (CompilationError e1) {
			e1.printStackTrace();
		}
	}
//	
//	@Test
//	public void testFiles() throws Throwable {
//		System.out.println(new File(".").getAbsoluteFile());
//		String[] files= {"thrax1.grm;thrax1.mealy","thrax3.grm;thrax3.mealy","thrax4.grm;thrax4.mealy"};
//		try {
//			final OptimisedHashLexTransducer specs = new OptimisedHashLexTransducer();
//			for (String file : files) {
//				try {
//					final String[] inOut = file.split(";");
//					final String expected = new String(Files.readAllBytes(Paths.get("src/test/resources",inOut[1]))).trim();
//					final File in = new File("src/test/resources/",inOut[0]);
//					ThraxParser<?, ?> parser = ThraxParser.parse(in,CharStreams.fromFileName(in.getPath()), specs.specs);
//					assertEquals(expected, parser.toSolomonoff().trim(),file);
//				} catch (Throwable e) {
//					e.printStackTrace();
//					System.out.println(file+"\n"+e);
//					throw e;
//				}
//			}
//		} catch (CompilationError e1) {
//			e1.printStackTrace();
//		}
//	}
}
