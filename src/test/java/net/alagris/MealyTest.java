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

        public TestCase(String regex, Positive[] positive, String[] negative, Class<? extends Throwable> exception) {
            this.regex = regex;
            this.positive = positive;
            this.negative = negative;
            this.exception = exception;
        }
    }

    static TestCase ex(String regex, Class<? extends Throwable> exception) {
        return new TestCase(regex, null, null, exception);
    }

    static TestCase t(String regex, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, positive, negative, null);
    }

    static TestCase a(String regex, Positive[] positive, String... negative) {
        return new TestCase(regex, positive, negative, null);
    }

    @Test
    void test() throws Exception {

        TestCase[] testCases = {


                t("\"a\"", ps("a;"), "b", "c", "", " "),
                t("\"\"", ps(";"), "a", "b", "c", "aa", " "),
                t("\"\":\"\"", ps(";"), "a", "b", "c", "aa", " "),
                t("\"a\":\"\"", ps("a;"), "aa", "b", "c", "", " "),
                t("\"a\":\"a\"", ps("a;a"), "b", "c", "", " "),
                t("\"\":\"a\" \"a\"", ps("a;a"), "b", "c", "", " "),
                t("\"a\":\"aa\"", ps("a;aa"), "b", "c", "", " "),
                t("\"a\":\" \"", ps("a; "), "b", "c", "", " "),
                t("\"\":\"   \"", ps(";   "), "a", "b", "c", " "),
                t("\"a\":\"TeSt Yo MaN\"", ps("a;TeSt Yo MaN"), "b", "c", "", " "),
                t("(\"a\")*", ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("\"ab\":\"xx\"", ps("ab;xx"), "a", "b", "c", "", " "),
                t("(\"a\"|\"b\"):\"a\"", ps("a;a", "b;a"), "e", "c", "", " "),
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
                t("\"\":\"a\" \"\":\"b\" \"\":\"c\" \"\":\"d\" \"\":\"e\"", ps(";abcde"), "a", "b"),
                t("\"a\":\"z\" (\"b\":\"x\" (\"c\":\"y\")*)*", ps("a;z", "ab;zx", "abc;zxy", "abbbcbbc;zxxxyxxy"), "c", "b", ""),
                t("1 \"a\":\"x\"", ps("a;x"), "", "aa", "b", " "),
                t("1 \"a\":\"x\" 2 ", ps("a;x"), "", "aa", "b", " "),
                t("\"a\":\"a\" 2 | \"a\":\"b\" 3", ps("a;b"), "b", "c", "", " "),
                t("(1 \"a\":\"x\" 2 | 2 \"a\":\"y\" 3) ", ps("a;y"), "", "aa", "b", " "),
                t("(1 \"a\":\"x\" 2 | 2 \"\":\"y\" 3) ", ps("a;x", ";y"), "aa", "b", " "),
                t("(\"a\":\"a\" 2 | \"a\":\"b\" 3) \"a\":\"a\"", ps("aa;ba"), "a", "ab", "b", "ba", "bb", "c", "", " "),
                t("(1 \"a\":\"x\" 2 | 2 \"a\":\"y\" 3)( \"a\":\"x\" 2 |\"a\":\"y\"3) ", ps("aa;yy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( \"a\":\"x\" 2 |\"a\":\"y\"3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 |\"a\":\"y\"3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)(  \"a\":\"x\" 2 | 1000 \"a\":\"y\"3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 \"a\":\"x\" 3 | 2 \"a\":\"y\" 2)( 1000 \"a\":\"x\" 2 | 1000 \"a\":\"y\"3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(\"a\":\"a\"|\"b\":\"b\" | \"aaa\":\"3\"1)*", ps("aa;aa", ";", "a;a", "aaa;3", "ab;ab", "abbba;abbba")),
                t("[a-z]:\"#\"", ps("a;a", "b;b", "c;c", "d;d", "z;z"), "", "aa", "ab", "bb", "1", "\t"),
                t("([a-z]:\"#\")*", ps("abcdefghijklmnopqrstuvwxyz;abcdefghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";")),
                t("[a-z]*", ps("abcdefghijklmnopqrstuvwxyz;", "a;", "b;", "c;", "d;", "z;", "aa;", "zz;", "rr;", ";", "abc;", "jbebrgebcbrjbabcabcabc;")),
                t("([a-z]:\"#\" | \"\":\"3\" \"abc\" 1)*", ps("abcdefghijklmnopqrstuvwxyz;3defghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3", "jbebrgebcbrjbabcabcabcadfe;jbebrgebcbrjb333adfe")),
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
                a("f=(.:\"#\")*",ps(";","a;a","afeee;afeee",
                        "\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"|ZXCVBNM<>?;\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"|ZXCVBNM<>?")),
                a("f=#",ps(), "","a","b","aa"," ","1","0","!","\"",",",";",")",".","#","$","\n")
        };

        int i = 0;
        for (TestCase testCase : testCases) {
            String input = null;
            try {

                CLI.OptimisedHashLexTransducer tr = new CLI.OptimisedHashLexTransducer(testCase.regex);
                assertNull(i + "[" + testCase.regex + "];", testCase.exception);


                for (Positive pos : testCase.positive) {
                    input = pos.input;
                    final String out = tr.run("f", pos.input);
                    final String exp = pos.output;
                    assertEquals(i + "[" + testCase.regex + "];" + pos.input, exp, out);
                }
                for (String neg : testCase.negative) {
                    input = neg;
                    final String out = tr.run("f", neg);
                    assertNull(i + "[" + testCase.regex + "];" + input, out);
                }
            } catch (Exception e) {
                if(testCase.exception!=null) {
                    assertEquals(i + "[" + testCase.regex + "];", testCase.exception, e.getClass());
                }else {
                    throw new Exception(i + "{" + testCase.regex + "}\"" + input + "\";" + e.getClass() + " " + e.getMessage(), e);
                }
            }
            i++;
        }

    }
}
