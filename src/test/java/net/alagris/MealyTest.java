package net.alagris;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import net.alagris.EpsilonFree.E;
import net.alagris.Glushkov;
import net.alagris.Mealy;
import net.alagris.MealyParser.Funcs;
import net.alagris.Simple;
import net.alagris.Simple.A;
import net.alagris.Simple.Eps;

public class MealyTest {

    static class Positive {
        String input, output;

        public Positive(String input, String output) {
            this.input = input;
            this.output = output;
        }
    }

    static Positive p(String input, String output) {
        return new Positive(input, output);
    }

    static Positive[] ps(String... inOut) {
        Positive[] out = new Positive[inOut.length];
        int i = 0;
        for (String s : inOut) {
            String[] parts = s.split(";", -1);
            out[i++] = p(parts[0], parts[1]);
        }
        return out;
    }

    static class TestCase {
        private String regex;
        private Positive[] positive;
        private String[] negative;

        public TestCase(String regex, Positive[] positive, String negative[]) {
            this.regex = regex;
            this.positive = positive;
            this.negative = negative;
        }
    }

    static TestCase t(String regex, Positive[] positive, String... negative) {
        return new TestCase("f()="+regex+";", positive, negative);
    }

    @Test
    void test() throws Exception {

        TestCase[] testCases = {
                
                
                
                
                
                
                
                
                t("\"a\"", ps("a;"), "b", "c", "", " "), t("(\"a\")", ps("a;"), "b", "c", "", " "),
                t("((\"a\"))", ps("a;"), "b", "c", "", " "), t("\"\"", ps(";"), "a", "b", "c", " "),
                t("\"a\"*", ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("\"ab\"", ps("ab;"), "a", "b", "c", "", " "),
                t("\"abc\"", ps("abc;"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("\"a(b|e)c\"", ps("a(b|e)c;"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc ", "abc",
                        "aec"),
                t("\"a\"(\"b\"|\"e\")\"c\"", ps("abc;", "aec;"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb",
                        " abc", "abc "),
                t("\"a\"(\"b\"|\"e\"|\"f\")\"c\"", ps("abc;", "aec;", "afc;"), "a", "b", "c", "", " ", "ab", "bb", "cb",
                        "abb", " abc", "abc "),
                t("\"a\"(\"b\"|\"e\"|\"f\")*\"c\"",
                        ps("abc;", "aec;", "afc;", "abbc;", "aeec;", "affc;", "abec;", "aefc;", "afbc;",
                                "abbbeffebfc;"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("\"a\":\"a\"", ps("a;a"), "b", "c", "", " "), t("\"a\":\"aa\"", ps("a;aa"), "b", "c", "", " "),
                t("\"a\":\" \"", ps("a; "), "b", "c", "", " "),
                t("\"a\":\"TeSt Yo MaN\"", ps("a;TeSt Yo MaN"), "b", "c", "", " "),
                t("\"\":\"   \"", ps(";   "), "a", "b", "c", " "),
                t("(\"a\")*", ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("\"ab\":\"xx\"", ps("ab;xx"), "a", "b", "c", "", " "),
                t("\"abc\":\"rte ()[]te\"", ps("abc;rte ()[]te"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb",
                        " abc", "abc "),
                t("\"a(b|e)c\":\"abc\"", ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("(\"a\"(\"b\"|\"e\")\"c\"):\"tre\"", ps("abc;tre", "aec;tre"), "a", "b", "c", "", " ", "ab", "bb",
                        "cb", "abb", " abc", "abc "),
                t("((\"a\"(\"b\"|\"e\"|\"f\")\"c\")):\"tre\"", ps("abc;tre", "aec;tre", "afc;tre"), "a", "b", "c", "",
                        " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("(((\"a\"(\"b\"|\"e\"|\"f\")*\"c\"))):\"tre\"",
                        ps("abc;tre", "aec;tre", "afc;tre", "abbc;tre", "aeec;tre", "affc;tre", "abec;tre", "aefc;tre",
                                "afbc;tre", "abbbeffebfc;tre"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("\"a\":\"x\" \"b\":\"y\"", ps("ab;xy"), "a", "b", "c", "", " "),
                t("\"a\":\"x\" \"b\":\"y\" \"c\":\"z\"", ps("abc;xyz"), "a", "b", "c", "", " "),
                t("\"a\":\"x\" \"b\":\"y\" \"c\":\"z\" \"de\":\"vw\"", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("(\"a\":\"x\" \"b\":\"y\") \"c\":\"z\" \"de\":\"vw\"", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("\"a\":\"x\" (\"b\":\"y\" \"c\":\"z\") \"de\":\"vw\"", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("\"a\":\"x\" (\"b\":\"y\" \"c\":\"z\" \"de\":\"vw\")", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("(\"a\":\"x\" \"b\":\"y\" \"c\":\"z\") \"de\":\"vw\"", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("\"a\":\"x\" (\"b\":\"y\" | \"c\":\"z\") \"de\":\"vw\"", ps("abde;xyvw", "acde;xzvw"), "a", "b", "c",
                        "", " "),
                t("\"a\":\"x\" (\"b\":\"y\" | \"c\":\"z\")* \"de\":\"vw\"",
                        ps("abbbde;xyyyvw", "acccde;xzzzvw", "abcde;xyzvw", "acbde;xzyvw", "abbde;xyyvw", "accde;xzzvw",
                                "abde;xyvw", "acde;xzvw", "ade;xvw"),
                        "a", "b", "c", "", " "),
                t("\"a\":\"x\" ( ( ( ( \"b\":\"y\" | \"c\":\"z\")*):\")\") \"de\":\"vw\")",
                        ps("abbbde;xyyy)vw", "acccde;xzzz)vw", "abcde;xyz)vw", "acbde;xzy)vw", "abbde;xyy)vw",
                                "accde;xzz)vw", "abde;xy)vw", "acde;xz)vw", "ade;x)vw"),
                        "a", "b", "c", "", " "),
                t("\"\":\"a\" \"\":\"b\" \"\":\"c\" \"\":\"d\" \"\":\"e\"",ps(";abcde"),"a","b"),
                t("\"a\":\"z\" (\"b\":\"x\" (\"c\":\"y\")*)*",ps("a;z","ab;zx","abc;zxy","abbbcbbc;zxxxyxxy"),"c","b",""),
        };

        int i = 0;
        for (TestCase testCase : testCases) {
            try {
                final Funcs funcs = MealyParser.parse(testCase.regex);
                final HashMap<String, A>  ctx =  MealyParser.eval(funcs);
                final A ast = ctx.get("f");
                final Eps epsilonFree = ast.removeEpsilons();
                final Mealy automaton = Glushkov.glushkov(epsilonFree);
                automaton.checkForNondeterminism();
                for (Positive pos : testCase.positive) {
                    String out = automaton.evaluate(pos.input);
                    assertEquals(i + "[" + testCase.regex + "];" + pos.input, pos.output, out);
                    
                }
            } catch (Exception e) {
                throw new Exception("{" + testCase.regex + "};" + e.getClass() + " " + e.getMessage(), e);
            }
            i++;
        }

    }
}
