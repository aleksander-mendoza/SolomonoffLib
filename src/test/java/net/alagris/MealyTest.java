package net.alagris;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

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
        private final String regex;
        private final Positive[] positive;
        private final String[] negative;
        private final Class<? extends Throwable> exception;
        private final int numStates;
        private final int numStatesAfterMin;

        public TestCase(String regex, Positive[] positive, String[] negative, Class<? extends Throwable> exception,
                        int numStates, int numStatesAfterMin) {
            this.regex = regex;
            this.positive = positive;
            this.negative = negative;
            this.exception = exception;
            this.numStates = numStates;
            this.numStatesAfterMin = numStatesAfterMin;
        }
    }

    static TestCase ex(String regex, Class<? extends Throwable> exception) {
        return new TestCase(regex, null, null, exception, -1, -1);
    }

    static TestCase t(String regex, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, positive, negative, null, -1, -1);
    }

    static TestCase t(String regex, int states,int statesAfterMin, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, positive, negative, null, states, statesAfterMin);
    }

    static TestCase a(String regex, Positive[] positive, String... negative) {
        return new TestCase(regex, positive, negative, null, -1, -1);
    }
    static TestCase a(String regex,int states,int statesAfterMin, Positive[] positive, String... negative) {
        return new TestCase(regex, positive, negative, null, states, statesAfterMin);
    }

    @Test
    void test() throws Exception {

        TestCase[] testCases = {

                t("\"a\"",3,3, ps("a;"), "b", "c", "", " "),
                t("\"\"",2,2, ps(";"), "a", "b", "c", "aa", " "),
                t("\"\":\"\"", 2,2,ps(";"), "a", "b", "c", "aa", " "),
                t("\"a\":\"\"", 3,3,ps("a;"), "aa", "b", "c", "", " "),
                t("\"a\":\"a\"", 3,3,ps("a;a"), "b", "c", "", " "),
                t("\"\":\"a\" \"a\"", 3,3,ps("a;a"), "b", "c", "", " "),
                t("\"a\":\"aa\"", 3,3,ps("a;aa"), "b", "c", "", " "),
                t("\"a\":\" \"",3,3, ps("a; "), "b", "c", "", " "),
                t("\"\":\"   \"",2,2, ps(";   "), "a", "b", "c", " "),
                t("\"a\":\"TeSt Yo MaN\"",3,3, ps("a;TeSt Yo MaN"), "b", "c", "", " "),
                t("(\"a\")*",3,3, ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("\"ab\":\"xx\"", 4,4,ps("ab;xx"), "a", "b", "c", "", " "),
                t("(\"a\"|\"b\"):\"a\"",4,3, ps("a;a", "b;a"), "e", "c", "", " "),
                t("\"abc\":\"rte ()[]te\"",5,5, ps("abc;rte ()[]te"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb",
                        " abc", "abc "),
                t("\"a(b|e)c\":\"abc\"", 9,9,ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("(\"a\"(\"b\"|\"e\")\"c\"):\"tre\"",6,5, ps("abc;tre", "aec;tre"), "a", "b", "c", "", " ", "ab", "bb",
                        "cb", "abb", " abc", "abc "),
                t("((\"a\"(\"b\"|\"e\"|\"f\")\"c\")):\"tre\"",7,5, ps("abc;tre", "aec;tre", "afc;tre"), "a", "b", "c", "",
                        " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("(((\"a\"(\"b\"|\"e\"|\"f\")*\"c\"))):\"tre\"",7,4,
                        ps("abc;tre", "aec;tre", "afc;tre", "abbc;tre", "aeec;tre", "affc;tre", "abec;tre", "aefc;tre",
                                "afbc;tre", "abbbeffebfc;tre"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("\"a\":\"x\" \"b\":\"y\"",4,4, ps("ab;xy"), "a", "b", "c", "", " "),
                t("\"a\":\"x\" | \"b\":\"y\" | \"c\":\"x\"| \"d\":\"y\"| \"e\":\"x\"| \"f\":\"y\"",8,4, ps("a;x","b;y","c;x","d;y","e;x","f;y"), "g", "h", "i", "", "aa"),
                t("\"k\"(\"a\":\"x\" | \"b\":\"y\" | \"c\":\"x\"| \"d\":\"y\"| \"e\":\"x\"| \"f\":\"y\")\"l\"",10,6, ps("kal;x","kbl;y","kcl;x","kdl;y","kel;x","kfl;y"), "g", "h", "i", "", "a","b","kl","kgl"),
                t("\"ax\":\"x\" | \"bx\":\"y\" |\"cx\":\"z\"", 8,8,ps("ax;x","bx;y","cx;z"), "a", "b", "c", "xx", "axax","xa",""),
                t("\"a\":\"x\" \"x\"| \"b\":\"y\" \"x\"|\"c\":\"z\" \"x\"", 8,6,ps("ax;x","bx;y","cx;z"), "a", "b", "c", "xx", "axax","xa",""),
                t("\"\":\"x\" \"a\" \"x\"| \"\":\"y\" \"b\" \"x\"|\"\":\"z\" \"c\" \"x\"", 8,4,ps("ax;x","bx;y","cx;z"), "a", "b", "c", "xx", "axax","xa",""),
                t("\"abcdefgh\" | \"abcdefg\" | \"abcdef\" | \"abcde\" | \"abcd\" | \"abc\" | \"ab\" | \"a\" | \"\"", 38,9,
                        ps("abcdefgh;","abcdefg;","abcdef;","abcde;","abcd;","abc;","ab;","a;",";"),
                        "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh","bcdefgh","abcdefghabcdefgh"),
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
                t("\"\":\"a\" \"\":\"b\" \"\":\"c\" \"\":\"d\" \"\":\"e\"", ps(";abcde"), "a", "b"),
                t("\"a\":\"z\" (\"b\":\"x\" (\"c\":\"y\")*)*", ps("a;z", "ab;zx", "abc;zxy", "abbbcbbc;zxxxyxxy"), "c", "b", ""),
                t("1 \"a\":\"x\"", ps("a;x"), "", "aa", "b", " "),
                t("1 \"a\":\"x\" 2 ", ps("a;x"), "", "aa", "b", " "),
                t("\"a\":\"a\" 2 | \"a\":\"b\" 3", 4,3,ps("a;b"), "b", "c", "", " "),
                t("\"a\":\"x\"2|\"a\":\"y\"3",4,3, ps("a;y"), "b", "c", "", "aa","`"),
                t("(1 \"a\":\"x\" 2 | 2 \"a\":\"y\" 3) ", ps("a;y"), "", "aa", "b", " "),
                t("(1 \"a\":\"x\" 2 | 2 \"\":\"y\" 3) ", ps("a;x", ";y"), "aa", "b", " "),
                t("(\"a\":\"a\" 2 | \"a\":\"b\" 3) \"a\":\"a\"", ps("aa;ba"), "a", "ab", "b", "ba", "bb", "c", "", " "),
                t("(1 \"a\":\"x\" 2 | 2 \"a\":\"y\" 3)( \"a\":\"x\" 2 |\"a\":\"y\"3) ", ps("aa;yy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( \"a\":\"x\" 2 |\"a\":\"y\"3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 |\"a\":\"y\"3) ", 6,6,ps("aa;xy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)(  \"a\":\"x\" 2 | 1000 \"a\":\"y\"3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 | 1000 \"a\":\"y\"3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(\"a\":\"a\"|\"b\":\"b\" | \"aaa\":\"3\"1)*", ps("aa;aa", ";", "a;a", "aaa;3", "ab;ab", "abbba;abbba")),
                t("[a-z]:\"#\"", ps("a;", "b;", "c;", "d;", "z;"), "", "aa", "ab", "bb", "1", "\t"),
                t("\"\":\"#\" [a-z]", ps("a;a", "b;b", "c;c", "d;d", "z;z"), "", "aa", "ab", "bb", "1", "\t"),
                t("(\"\":\"#\" [a-z])*", ps("abcdefghijklmnopqrstuvwxyz;abcdefghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";")),
                t("([a-z]:\"#\")*", ps("abcdefghijklmnopqrstuvwxyz;bcdefghijklmnopqrstuvwxyz", "a;", "b;", "c;", "d;", "z;", "aa;a", "zz;z", "rr;r", "ab;b", ";")),
                t("[a-z]*", ps("abcdefghijklmnopqrstuvwxyz;", "a;", "b;", "c;", "d;", "z;", "aa;", "zz;", "rr;", ";", "abc;", "jbebrgebcbrjbabcabcabc;")),
                t("(\"\":\"#\" [a-z] | \"\":\"3\" \"abc\" 1)*", ps("abcdefghijklmnopqrstuvwxyz;3defghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3", "jbebrgebcbrjbabcabcabcadfe;jbebrgebcbrjb333adfe")),
                t("(\"a\":\"x\" 3 | \"a\":\"y\" 5)", ps("a;y")),
                t("(\"a\":\"x\" 3 | (\"a\":\"y\" 5) -3 )", ps("a;x")),
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
                t(" 1 \"a\"| 2 \"b\"|\"c\"| \"d\"", ps("a;","b;","c;","d;"), "e", "f", "", " "),
                t("\"a\"(\"b\"|\"e\"|\"f\")*\"c\"",
                        ps("abc;", "aec;", "afc;", "abbc;", "aeec;", "affc;", "abec;", "aefc;", "afbc;",
                                "abbbeffebfc;"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                ex("f=\"a\":\"a\"|\"a\":\"b\"", CompilationError.WeightConflictingFinal.class),
                a("f::\"a\"->\"\" f=\"a\"", ps("a;"), "b", "c", "", " "),
                a("f::\"\"->\"\" f=\"\"", ps(";"), "a", "b", "c", "aa", " "),
                ex("f::\"\"->\"\" f=\"a\"", CompilationError.TypecheckException.class),
                ex("f::\"a\"->\"a\" f=\"a\"", CompilationError.TypecheckException.class),
                ex("f::\"b\"->\"\" f=\"a\"", CompilationError.TypecheckException.class),
                ex("f::\"aa\"->\"\" f=\"a\"", CompilationError.TypecheckException.class),
                ex("f::\"\"*->\"\" f=\"a\"", CompilationError.TypecheckException.class),
                a("f::\"a\"*->\"\" f=\"a\"", ps("a;"), "b", "c", "", " "),
                a("f::\"\"|\"e\"|\"r\"->\"\"|\"\" f=\"\":\"\"", ps(";"), "a", "b", "c", "aa", " "),
                a("f::\"a\" \"b\"*->\"e\"* f=\"a\":\"\"", ps("a;"), "aa", "b", "c", "", " "),
                a("f::\"a\"|\"c\"->\"rre\"|\"a\"* f=\"a\":\"a\"", ps("a;a"), "b", "c", "", " "),
                a("f::\"a\"* -> \"a\"* f=\"\":\"a\" \"a\"", ps("a;a"), "b", "c", "", " "),
                a("f::\"b\"* \"a\" -> \"aa\"* f=\"a\":\"aa\"", ps("a;aa"), "b", "c", "", " "),
                a("f::.*->.* " +
                        "f=(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 | 1000 \"a\":\"y\"3);", ps("aa;xy"), "", "a", "b", " "),
                a("A=[0-1] " +
                        "f::A*->A* " +
                        "f=\"01\":\"10\"", ps("01;10"), "", "1", "0", "010"),
                a("A=[0-1]" +
                        "B=[0-9]" +
                        "f::B*->A*" +
                        "f=[9-0]:\"10\"", ps("9;10", "4;10", "7;10", "0;10"), "", "10", "20", "65"),
                a("g=\"abc\":\"0\"|\"def\":\"1\" f= g g g",
                        ps("abcdefabc;010", "abcabcabc;000", "defdefdef;111", "defabcdef;101"),
                        "", "abc", "def", "abcabc","abcabcabcabc", "defdefdefdef"),
                a("g=\"aab\" h= g g g g f::h->\"\" f=\"aabaabaabaab\"",
                        ps("aabaabaabaab;"),"", "abc", "def", "aa","aabaabaabaaba", "aabaabaabaa"),
                a("f=(\"\":\"#\" .)*",ps(";","a;a","afeee;afeee",
                        "\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"|ZXCVBNM<>?;\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"|ZXCVBNM<>?")),
                a("f=#",ps(), "","a","b","aa"," ","1","0","!","\"",",",";",")",".","#","$","\n"),
                t("[a-a]:\"a\" | [b-c]:\"b\" | [d-e]:\"d\" | [f-g]:\"f\" | [h-i]:\"h\"",
                        ps("a;a","b;b","c;b","d;d","e;d","f;f","g;f","h;h","i;h"), "`", "j", "", " "),
                t("[a-a]:\"a\" |  [d-e]:\"d\" | [f-g]:\"f\" | [h-i]:\"h\"",
                        ps("a;a","d;d","e;d","f;f","g;f","h;h","i;h"), "`", "j", "b", "c"),
                t("[a-a]:\"a\" | [b-c]:\"b\" |  [f-g]:\"f\" | [h-i]:\"h\"",
                        ps("a;a","b;b","c;b","f;f","g;f","h;h","i;h"), "`", "j", "d", "e"),
                t("[a-a]:\"a\" | [b-e]:\"b\" |  [f-g]:\"f\" | [h-i]:\"h\"",
                        ps("a;a","b;b","c;b","d;b","e;b","f;f","g;f","h;h","i;h"), "`", "j", "", " "),
                t("[a-h]:\"a\" 1 | [e-m]:\"b\" 2 |  [k-m]:\"f\" 3",
                        ps("a;a","b;a","c;a","d;a","e;b","f;b","g;b","h;b","i;b","j;b","k;f","l;f","m;f"), "`", "n", "o", "p"),
                t(".:\"a\" 2 | [b-e]:\"b\" |  [f-g]:\"f\" | [h-i]:\"h\"",
                        ps("a;a","b;a","c;a","d;a","e;a","f;a","g;a","h;a","i;a","`;a", "j;a"), "", "  ","ee"),
                t("\"abcd\" \"efgh\" \"ijkl\" \"mnop\"", ps("abcdefghijklmnop;"), "", "  ","ee"),
                t("\"abcd\":\"1\" \"efgh\":\"2\" \"ijkl\":\"3\" \"mnop\":\"4\"", ps("abcdefghijklmnop;1234"), "", "  ","ee"),
                t("\"\":\"1\" \"\":\"2\" \"\":\"3\" \"\":\"4\"", ps(";1234"), "a", "  ","ee"),
                t("\"a\":\"1\" \"b\":\"2\" | \"c\":\"3\" \"de\":\"4\"", ps("ab;12", "cde;34"), "a", "  ","ee"),
                t("\"x\" (\"a\":\"1\" \"b\":\"2\" | \"c\":\"3\" \"de\":\"4\")", ps("xab;12", "xcde;34"), "a", "  ","ee"),
                t("\"x\" (\"a\":\"1\" \"b\":\"2\" | \"c\":\"3\" \"de\":\"4\") \"y\"", ps("xaby;12", "xcdey;34"), "a", "  ","ee"),
                a("f::[a-z]->[a-z] f=\"\":\"#\" [a-b]", ps("a;a", "b;b"), "`","c", "d","e"),
                a("f::[a-b]->[a-b] f=\"\":\"#\" [a-b]", ps("a;a", "b;b"), "`","c", "d","e"),
                a("f::[a-b]->\"a\"|\"b\" f=\"\":\"#\" [a-b]", ps("a;a", "b;b"), "`","c", "d","e"),
                ex("f::[a-z]-># f=\"\":\"#\" [a-b]", CompilationError.TypecheckException.class),
                ex("f::[a-z]->\"a\" f=\"\":\"#\" [a-b]", CompilationError.TypecheckException.class),
                ex("f::[a-z]->\"b\" f=\"\":\"#\" [a-b]", CompilationError.TypecheckException.class),
                ex("f::\"a\"->[a-b] f=\"\":\"#\" [a-b]", CompilationError.TypecheckException.class),
                ex("f::([a-z]|[d-e])->([a-z]|[d-e]) f=\"\":\"#\" ([a-b] | [d-e])",CompilationError.NondeterminismException.class),
                a("f::([a-b]|[d-e])->([a-b]|[d-e]) f=\"\":\"#\" ([a-b] | [d-e])", ps("a;a", "b;b","d;d","e;e"), "`","c", "f","g"),
                ex("f::([a-z]|[d-e])->([a-e]) f=\"\":\"#\" ([a-b] | [d-e])",CompilationError.NondeterminismException.class),
                a("f::([a-b]|[d-e])->([a-e]) f=\"\":\"#\" ([a-b] | [d-e])", ps("a;a", "b;b","d;d","e;e"), "`","c", "f","g"),
                a("f::([a-b]|[d-e])->([a-b]|[d-e]) f=\"\":\"#\" ([a-b] | [d-e])", ps("a;a", "b;b","d;d","e;e"), "`","c", "f","g"),
                ex("f::([a-e])->([a-z]|[d-e]) f=\"\":\"#\" ([a-b] | [d-e])", CompilationError.NondeterminismException.class),
                a("f::([a-e])->([a-b]|[d-e]) f=\"\":\"#\" ([a-b] | [d-e])", ps("a;a", "b;b","d;d","e;e"), "`","c", "f","g"),
                a("f::.->. f=\"\":\"#\" ([a-b] | [d-e])", ps("a;a", "b;b","d;d","e;e"), "`","c", "f","g"),
                a("f::.->(\"a\"|\"b\"|\"d\"|\"e\"|\"\") f=\"\":\"#\" ([a-b] | [d-e])", ps("a;a", "b;b","d;d","e;e"), "`","c", "f","g"),
                a("f::.->(\"a\"|\"b\"|\"d\"|\"e\"|#) f=\"\":\"#\" ([a-b] | [d-e])", ps("a;a", "b;b","d;d","e;e"), "`","c", "f","g"),
                ex("f=\"\\0\":\"a\"", CompilationError.IllegalCharacter.class),
                ex("f=\"a\":\"\\0\"", CompilationError.IllegalCharacter.class),
                a("f::\"a\"->\"xyz\" f=\"a\":\"xyz\"", ps("a;xyz"), "`","c", "f","g"),
                a("f::\"\"->\"xyz\" f=\"\":\"xyz\"", ps(";xyz"), "`","c", "f","g"),
        };

        int i = 0;
        for (TestCase testCase : testCases) {
            String input = null;
            try {
                CLI.OptimisedHashLexTransducer tr = new CLI.OptimisedHashLexTransducer(testCase.regex);
                assertNull(i + "[" + testCase.regex + "];", testCase.exception);
                if(testCase.numStates>-1){
                    assertEquals(i + "[" + testCase.regex + "];",testCase.numStates, tr.optimised.get("f").graph.size());
                }
                for (Positive pos : testCase.positive) {
                    input = pos.input;
                    final String out = tr.run("f", pos.input);
                    final String exp = pos.output;
                    assertEquals(i + "[" + testCase.regex + "];" + pos.input, exp, out);
                }
                for (String neg : testCase.negative) {
                    input = neg;
                    final String out = tr.run("f", neg);
                    assertNull(i +  "[" + testCase.regex + "];" + input, out);
                }
                tr.pseudoMinimize("f");
                if(testCase.numStatesAfterMin>-1){
                    assertEquals(i + "[" + testCase.regex + "];",testCase.numStatesAfterMin,
                            tr.optimised.get("f").graph.size());
                }
                for (Positive pos : testCase.positive) {
                    input = pos.input;
                    final String out = tr.run("f", pos.input);
                    final String exp = pos.output;
                    assertEquals(i + "min[" + testCase.regex + "];" + pos.input, exp, out);
                }
                for (String neg : testCase.negative) {
                    input = neg;
                    final String out = tr.run("f", neg);
                    assertNull(i + "min[" + testCase.regex + "];" + input, out);
                }
            } catch (Exception e) {
                if(testCase.exception!=null) {
                    assertEquals(i +"[" + testCase.regex + "];", testCase.exception, e.getClass());
                }else {
                    throw new Exception(i + "{" + testCase.regex + "}\"" + input + "\";" + e.getClass() + " " + e.getMessage(), e);
                }
            }
            i++;
        }

    }
}
