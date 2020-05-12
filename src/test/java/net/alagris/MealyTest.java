package net.alagris;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import net.alagris.MealyParser.AAA;
import net.alagris.MealyParser.Funcs;
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
    static TestCase a(String regex, Positive[] positive, String... negative) {
        return new TestCase(regex, positive, negative);
    }

    @Test
    void test() throws Exception {

        TestCase[] testCases = {
                
                t("(\"a\":\"x\" 3 | (\"a\":\"y\" 5) -3 )",ps("a;x")),
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
                t("1 \"a\":\"x\"",ps("a;x"),"","aa","b"," "),
                t("1 \"a\":\"x\" 2 ",ps("a;x"),"","aa","b"," "),
                t("(1 \"a\":\"x\" 2 | 2 \"a\":\"y\" 3) ",ps("a;y"),"","aa","b"," "),
                t("(1 \"a\":\"x\" 2 | 2 \"\":\"y\" 3) ",ps("a;x",";y"),"aa","b"," "),
                t("(1 \"a\":\"x\" 2 | 2 \"a\":\"y\" 3)( \"a\":\"x\" 2 |\"a\":\"y\"3) ",ps("aa;yy"),"","a","b"," "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( \"a\":\"x\" 2 |\"a\":\"y\"3) ",ps("aa;xy"),"","a","b"," "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 |\"a\":\"y\"3) ",ps("aa;xy"),"","a","b"," "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)(  \"a\":\"x\" 2 | 1000 \"a\":\"y\"3) ",ps("aa;xy"),"","a","b"," "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 | 1000 \"a\":\"y\"3) ",ps("aa;xy"),"","a","b"," "),
                t("(\"a\":\"a\"|\"b\":\"b\" | \"aaa\":\"3\"1)*",ps("aa;aa",";","a;a","aaa;3","ab;ab","abbba;abbba")),
                t("\"\":\"#\" [a-z]",ps("a;a","b;b","c;c","d;d","z;z"),"","aa","ab","bb","1","\t"),
                t("(\"\":\"#\" [a-z])*",ps("abcdefghijklmnopqrstuvwxyz;abcdefghijklmnopqrstuvwxyz","a;a","b;b","c;c","d;d","z;z","aa;aa","zz;zz","rr;rr",";")),
                t("[a-z]*",ps("abcdefghijklmnopqrstuvwxyz;","a;","b;","c;","d;","z;","aa;","zz;","rr;",";","abc;","jbebrgebcbrjbabcabcabc;")),
                t("(\"\":\"#\" [a-z] | \"\":\"3\" \"abc\" 1)*",ps("abcdefghijklmnopqrstuvwxyz;3defghijklmnopqrstuvwxyz","a;a","b;b","c;c","d;d","z;z","aa;aa","zz;zz","rr;rr",";","abc;3","jbebrgebcbrjbabcabcabcadfe;jbebrgebcbrjb333adfe")),
                t("(\"a\":\"x\" 3 | \"a\":\"y\" 5)",ps("a;y")),
                a("f:UNICODE->UNICODE ; f()=(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 | 1000 \"a\":\"y\"3);",ps("aa;xy"),"","a","b"," "),
                a("A=[01];f:A->A;f()=\"01\":\"10\";", ps("01;10"),"","1","0","01"),
                a("A=[01];B=[1947026];f:B->A;f()=[9-0]:\"10\";", ps("9;10","4;10","7;10","0;10"),"","1","2","6"),
                a("struct S{x:UNICODE*} f:UNICODE->S;f()=\"01\":{x=\"10\"};", ps("01;10<EOF><EOS>"),"","1","0","01"),
        };

        int i = 0;
        for (TestCase testCase : testCases) {
            String input=null;
            try {
                final Funcs funcs = MealyParser.parse(testCase.regex);
                final HashMap<String, AAA>  ctx =  MealyParser.eval(funcs);
                final AAA ast = ctx.get("f");
                Simple.Ptr<Integer> ptr = new Simple.Ptr<>(0);
                final Eps epsilonFree = ast.ast.removeEpsilons(ptr);
                final Mealy automaton = epsilonFree.glushkov(ptr,ast.in.asAlph(),ast.out.asAlph());
                automaton.checkForNondeterminism();
                for (Positive pos : testCase.positive) {
                    input = pos.input;
                    IntArrayList out = automaton.evaluate(ast.in.asAlph().map(pos.input));
                    IntArrayList exp = ast.out.asAlph().map(pos.output);
                    assertEquals(i + "[" + testCase.regex + "];" + pos.input, exp.toString(), out.toString());
                    
                }
                ptr.v = 0;
                final Eps otherEps = ast.ast.removeEpsilons(ptr);
                final Mealy otherAut = otherEps.glushkov(ptr,ast.in.asAlph(),ast.out.asAlph());
                assertEquals(otherAut, automaton);
            } catch (Exception e) {
                throw new Exception(i+"{" + testCase.regex + "}\""+input+"\";" + e.getClass() + " " + e.getMessage(), e);
            }
            i++;
        }

    }
}
