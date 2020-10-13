package net.alagris;

import org.junit.ComparisonFailure;
import org.junit.jupiter.api.Test;

import net.alagris.LexUnicodeSpecification.E;
import net.alagris.LexUnicodeSpecification.P;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;

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
        private final Class<? extends Throwable> exceptionAfterMin;
        private final int numStates;
        private final int numStatesAfterMin;

        public TestCase(String regex, Positive[] positive, String[] negative, Class<? extends Throwable> exception,
                        Class<? extends Throwable> exceptionAfterMin, int numStates, int numStatesAfterMin) {
            this.regex = regex;
            this.positive = positive;
            this.negative = negative;
            this.exception = exception;
            this.exceptionAfterMin = exceptionAfterMin;
            this.numStates = numStates;
            this.numStatesAfterMin = numStatesAfterMin;

        }
    }

    static TestCase ex(String regex, Class<? extends Throwable> exception, Class<? extends Throwable> exceptionAfterMin) {
        assert exceptionAfterMin != null;
        assert exception != null;
        return new TestCase(regex, null, null, exception, exceptionAfterMin, -1, -1);
    }

    static TestCase exNoMin(String regex, Class<? extends Throwable> exception, Positive[] positive, String... negative) {
        assert exception != null;
        return new TestCase(regex, positive, negative, exception, null, -1, -1);
    }

    static TestCase ex2(String regex, Class<? extends Throwable> exception) {
        return new TestCase(regex, null, null, exception, exception, -1, -1);
    }

    static TestCase t(String regex, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, positive, negative, null, null, -1, -1);
    }

    static TestCase t(String regex, int states, int statesAfterMin, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, positive, negative, null, null, states, statesAfterMin);
    }

    static TestCase a(String regex, Positive[] positive, String... negative) {
        return new TestCase(regex, positive, negative, null, null, -1, -1);
    }

    static TestCase a(String regex, int states, int statesAfterMin, Positive[] positive, String... negative) {
        return new TestCase(regex, positive, negative, null, null, states, statesAfterMin);
    }

    @Test
    void test() throws Exception {

        TestCase[] testCases = {
                t("'ab' | 'ab' 1", 6, 4, ps("ab;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("#", 2, 2, ps(), "a", "b", "c", "", " "),
                t("'a'", 3, 3, ps("a;"), "b", "c", "", " "),
                t("'a'|#", 3, 3, ps("a;"), "b", "c", "", " "),
                t("#|'a'", 3, 3, ps("a;"), "b", "c", "", " "),
                t("#|'aa'", 4, 4, ps("aa;"), "b", "c", "", " "),
                t("'aa'|#", 4, 4, ps("aa;"), "b", "c", "", " "),
                t("'aa' #", 2, 2, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("# #", 2, 2, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("#*", 2, 2, ps(";"), "aa", "a", "aaa", "b", "c", " "),
                t("#* #", 2, 2, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("#* | #", 2, 2, ps(";"), "aa", "a", "aaa", "b", "c", " "),
                t("(#*) :'a'", 2, 2, ps(";a"), "aa", "a", "aaa", "b", "c", " "),
                t("<97>", 3, 3, ps("a;"), "b", "c", "", " "),
                t("''", 2, 2, ps(";"), "a", "b", "c", "aa", " "),
                t("'':''", 2, 2, ps(";"), "a", "b", "c", "aa", " "),
                t("'a':''", 3, 3, ps("a;"), "aa", "b", "c", "", " "),
                t("'a':'a'", 3, 3, ps("a;a"), "b", "c", "", " "),
                t("'':'a' 'a'", 3, 3, ps("a;a"), "b", "c", "", " "),
                t("'a':'aa'", 3, 3, ps("a;aa"), "b", "c", "", " "),
                t("'a':' '", 3, 3, ps("a; "), "b", "c", "", " "),
                t("'':'   '", 2, 2, ps(";   "), "a", "b", "c", " "),
                t("'a':'TeSt Yo MaN'", 3, 3, ps("a;TeSt Yo MaN"), "b", "c", "", " "),
                t("('a')*", 3, 3, ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("('a')+", 3, 3, ps("a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", "", " "),
                t("('a')?", 3, 3, ps("a;", ";"), "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("'a'*", 3, 3, ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("'a'+", 3, 3, ps("a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", "", " "),
                t("'a'?", 3, 3, ps("a;", ";"), "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("('abcd')*", 6, 6, ps(";", "abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;"), "a", "b", "c", " "),
                t("('abcd')+", 6, 6, ps("abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;"), "b", "c", "", " "),
                t("('abcd')?", 6, 6, ps("abcd;", ";"), "abcdabcd", "abcdabcdabcd", "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("('abcd'|'012')*", 9, 8, ps(";", "abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;",
                        "012;", "abcd012;", "abcd012abcd;", "abcd012abcd012abcd;", "012abcdabcdabcdabcd012012abcdabcdabcd;",
                        "012012;", "012abcd;", "abcd012012abcd;", "abcdabcdabcd012012;", "abcdabcd012abcdabcdabcd012012abcdabcd;"), "a", "b", "c", " "),
                t("('abcd'|'012')+", 9, 8, ps("abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;",
                        "012;", "abcd012;", "abcd012abcd;", "abcd012abcd012abcd;", "012abcdabcdabcdabcd012012abcdabcdabcd;",
                        "012012;", "012abcd;", "abcd012012abcd;", "abcdabcdabcd012012;", "abcdabcd012abcdabcdabcd012012abcdabcd;"), "b", "c", "", " "),
                t("('abcd'|'012')?", 9, 8, ps("abcd;", ";", "012;"), "012012", "abcd012", "abcd012abcd", "abcd012abcd012abcd", "012abcdabcdabcdabcd012012abcdabcdabcd",
                        "012012", "012abcd", "abcd012012abcd", "abcdabcdabcd012012", "abcdabcd012abcdabcdabcd012012abcdabcd", "abcdabcd", "abcdabcdabcd", "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("''*", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''?)*", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''+)*", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''*)*", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''?)+", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''+)+", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''*)+", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''?)?", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''+)?", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''*)?", 2, 2, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("'ab':'xx'", 4, 4, ps("ab;xx"), "a", "b", "c", "", " "),
                t("('a'|'b'):'a'", 4, 3, ps("a;a", "b;a"), "e", "c", "", " "),
                t("'ab' | 'ab' 1", 6, 4, ps("ab;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("'b' | 'b' 1", 4, 3, ps("b;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "c", "", " "),
                t("'a' | 'ab'", 5, 4, ps("a;", "ab;"), "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("'abc':'rte ()[]te'", 5, 5, ps("abc;rte ()[]te"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb",
                        " abc", "abc "),
                t("'a(b|e)c':'abc'", 9, 9, ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("<97 40 98 124 101 41 99>:<97 98 99>", 9, 9, ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("('a'('b'|'e')'c'):'tre'", 6, 5, ps("abc;tre", "aec;tre"), "a", "b", "c", "", " ", "ab", "bb",
                        "cb", "abb", " abc", "abc "),
                t("(('a'('b'|'e'|'f')'c')):'tre'", 7, 5, ps("abc;tre", "aec;tre", "afc;tre"), "a", "b", "c", "",
                        " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("((('a'('b'|'e'|'f')*'c'))):'tre'", 7, 4,
                        ps("abc;tre", "aec;tre", "afc;tre", "abbc;tre", "aeec;tre", "affc;tre", "abec;tre", "aefc;tre",
                                "afbc;tre", "abbbeffebfc;tre"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("'a':'x' 'b':'y'", 4, 4, ps("ab;xy"), "a", "b", "c", "", " "),
                t("'a':'x' | 'b':'y' | 'c':'x'| 'd':'y'| 'e':'x'| 'f':'y'", 8, 4, ps("a;x", "b;y", "c;x", "d;y", "e;x", "f;y"), "g", "h", "i", "", "aa"),
                t("'k'('a':'x' | 'b':'y' | 'c':'x'| 'd':'y'| 'e':'x'| 'f':'y')'l'", 10, 6, ps("kal;x", "kbl;y", "kcl;x", "kdl;y", "kel;x", "kfl;y"), "g", "h", "i", "", "a", "b", "kl", "kgl"),
                t("'ax':'x' | 'bx':'y' |'cx':'z'", 8, 8, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("'a':'x' 'x'| 'b':'y' 'x'|'c':'z' 'x'", 8, 6, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("'':'x' 'a' 'x'| '':'y' 'b' 'x'|'':'z' 'c' 'x'", 8, 4, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("'yxa' | 'yxab'", 9, 6, ps("yxa;", "yxab;"),
                        "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "",
                        "xaab", "xaaa", "xbbb", "xbb", "xabb", "xaa", "xb", "xc", "x",
                        "xxaab", "xxaaa", "xxbbb", "xxbb", "xxbb", "xxaa", "xxb", "xxc", "xx",
                        "yaab", "yaaa", "ybbb", "ybb", "yabb", "yaa", "yb", "yc", "",
                        "yxaab", "yxaaa", "yxbbb", "yxbb", "yxabb", "yxaa", "yxb", "yxc", "yx",
                        "yxxaab", "yxxaaa", "yxxbbb", "yxxbb", "yxxbb", "yxxaa", "yxxb", "yxxc", "yxx"),

                t("'xa' | 'xab'", 7, 5, ps("xa;", "xab;"), "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "",
                        "xaab", "xaaa", "xbbb", "xbb", "xabb", "xaa", "xb", "xc", "x", "xxaab", "xxaaa", "xxbbb", "xxbb", "xxbb", "xxaa", "xxb", "xxc", "xx"),

                t("'abcdefgh' | ''", 10, 10, ps("abcdefgh;", ";"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefgh' 1", 18, 10, ps("abcdefgh;"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefgh' 1 | 'abcdefgh' 2 | 'abcdefgh' 3", 34, 10, ps("abcdefgh;"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefg' | 'abcdef'", 23, 10,
                        ps("abcdefgh;", "abcdefg;", "abcdef;"),
                        "abcdefghh", "aa", "abcdefgha", "abcde", "abcd", "abc", "ab", "a", "", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefg' | 'abcdef' | 'abcde' | 'abcd' | 'abc' | 'ab' | 'a' | ''", 38, 10,
                        ps("abcdefgh;", "abcdefg;", "abcdef;", "abcde;", "abcd;", "abc;", "ab;", "a;", ";"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'a':'x' 'b':'y' 'c':'z'", ps("abc;xyz"), "a", "b", "c", "", " "),
                t("'a':'x' 'b':'y' 'c':'z' 'de':'vw'", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("('a':'x' 'b':'y') 'c':'z' 'de':'vw'", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("'a':'x' ('b':'y' 'c':'z') 'de':'vw'", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("'a':'x' ('b':'y' 'c':'z' 'de':'vw')", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("('a':'x' 'b':'y' 'c':'z') 'de':'vw'", ps("abcde;xyzvw"), "a", "b", "c", "", " "),
                t("'a':'x' ('b':'y' | 'c':'z') 'de':'vw'", ps("abde;xyvw", "acde;xzvw"), "a", "b", "c",
                        "", " "),
                t("'a':'x' ('b':'y' | 'c':'z')* 'de':'vw'",
                        ps("abbbde;xyyyvw", "acccde;xzzzvw", "abcde;xyzvw", "acbde;xzyvw", "abbde;xyyvw", "accde;xzzvw",
                                "abde;xyvw", "acde;xzvw", "ade;xvw"),
                        "a", "b", "c", "", " "),
                t("'a':'x' ( ( ( ( 'b':'y' | 'c':'z')*):')') 'de':'vw')",
                        ps("abbbde;xyyy)vw", "acccde;xzzz)vw", "abcde;xyz)vw", "acbde;xzy)vw", "abbde;xyy)vw",
                                "accde;xzz)vw", "abde;xy)vw", "acde;xz)vw", "ade;x)vw"),
                        "a", "b", "c", "", " "),
                t("'':'a' '':'b' '':'c' '':'d' '':'e'", ps(";abcde"), "a", "b"),
                t("'a':'z' ('b':'x' ('c':'y')*)*", ps("a;z", "ab;zx", "abc;zxy", "abbbcbbc;zxxxyxxy"), "c", "b", ""),
                t("1 'a':'x'", ps("a;x"), "", "aa", "b", " "),
                t("1 'a':'x' 2 ", ps("a;x"), "", "aa", "b", " "),
                t("'a':'a' 2 | 'a':'b' 3", 4, 3, ps("a;b"), "b", "c", "", " "),
                t("'a':'x'2|'a':'y'3", 4, 3, ps("a;y"), "b", "c", "", "aa", "`"),
                t("(1 'a':'x' 2 | 2 'a':'y' 3) ", ps("a;y"), "", "aa", "b", " "),
                t("(1 'a':'x' 2 | 2 '':'y' 3) ", ps("a;x", ";y"), "aa", "b", " "),
                t("('a':'a' 2 | 'a':'b' 3) 'a':'a'", ps("aa;ba"), "a", "ab", "b", "ba", "bb", "c", "", " "),
                t("(1 'a':'x' 2 | 2 'a':'y' 3)( 'a':'x' 2 |'a':'y'3) ", ps("aa;yy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)( 'a':'x' 2 |'a':'y'3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)( 1000 'a':'x' 2 |'a':'y'3) ", 6, 4, ps("aa;xy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)(  'a':'x' 2 | 1000 'a':'y'3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)( 1000 'a':'x' 2 | 1000 'a':'y'3) ", ps("aa;xy"), "", "a", "b", " "),
                t("('a':'a'|'b':'b' | 'aaa':'3'1)*", ps("aa;aa", ";", "a;a", "aaa;3", "ab;ab", "abbba;abbba")),
                t("[a-z]:<0>", ps("a;", "b;", "c;", "d;", "z;"), "", "aa", "ab", "bb", "1", "\t"),
                t("'':<0> [a-z]", ps("a;a", "b;b", "c;c", "d;d", "z;z"), "", "aa", "ab", "bb", "1", "\t"),
                t("('':<0> [a-z])*", ps("abcdefghijklmnopqrstuvwxyz;abcdefghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";")),
                t("([a-z]:<0>)*", ps("abcdefghijklmnopqrstuvwxyz;bcdefghijklmnopqrstuvwxyz", "a;", "b;", "c;", "d;", "z;", "aa;a", "zz;z", "rr;r", "ab;b", ";")),
                t("[a-z]*", ps("abcdefghijklmnopqrstuvwxyz;", "a;", "b;", "c;", "d;", "z;", "aa;", "zz;", "rr;", ";", "abc;", "jbebrgebcbrjbabcabcabc;")),
                t("('':'\\0' [a-z] | '':'3' 'abc' 1)*", ps("abcdefghijklmnopqrstuvwxyz;3defghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3", "jbebrgebcbrjbabcabcabcadfe;jbebrgebcbrjb333adfe")),
                t("('':<0> [a-z] | '':'3' 'abc' 1)*", ps("abcdefghijklmnopqrstuvwxyz;3defghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3", "jbebrgebcbrjbabcabcabcadfe;jbebrgebcbrjb333adfe")),
                t("('a':'x' 3 | 'a':'y' 5)", ps("a;y")),
                t("('a':'x' 3 | ('a':'y' 5) -3 )", ps("a;x")),
                t("'a'", ps("a;"), "b", "c", "", " "), t("('a')", ps("a;"), "b", "c", "", " "),
                t("(('a'))", ps("a;"), "b", "c", "", " "), t("''", ps(";"), "a", "b", "c", " "),
                t("'a'*", ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("'ab'", ps("ab;"), "a", "b", "c", "", " "),
                t("'abc'", ps("abc;"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("'a(b|e)c'", ps("a(b|e)c;"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc ", "abc",
                        "aec"),
                t("'a'('b'|'e')'c'", ps("abc;", "aec;"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb",
                        " abc", "abc "),
                t("'a'('b'|'e'|'f')'c'", ps("abc;", "aec;", "afc;"), "a", "b", "c", "", " ", "ab", "bb", "cb",
                        "abb", " abc", "abc "),
                t(" 1 'a'| 2 'b'|'c'| 'd'", ps("a;", "b;", "c;", "d;"), "e", "f", "", " "),
                t("'a'('b'|'e'|'f')*'c'",
                        ps("abc;", "aec;", "afc;", "abbc;", "aeec;", "affc;", "abec;", "aefc;", "afbc;",
                                "abbbeffebfc;"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                ex2("f='a':'a'|'a':'b'", CompilationError.WeightConflictingFinal.class),
                ex2("f=('a':'a'|'a':'b')'c'", CompilationError.WeightConflictingToThirdState.class),
                ex2("f=('aa':'a'|'aa':'b')'c'", CompilationError.WeightConflictingToThirdState.class),
                ex2("f=('a':'a' 1 |'a':'b' 1)'c'", CompilationError.WeightConflictingToThirdState.class),
                exNoMin("f=('a':'a' 'b'|'a':'a' 'b')'c'", CompilationError.WeightConflictingToThirdState.class, ps("abc;a"), "ab", "a", "c", "bc", "", "abcc", "abca"),
                ex2("f=('a':'a'|'a':'b')'c'", CompilationError.WeightConflictingToThirdState.class),
                ex2("f=('a':'a' 'a':'a'|'a':'b' 'a':'a')'c'", CompilationError.WeightConflictingToThirdState.class),
                ex2("f=('a':'a' 3|'a':'b' 3)'c'", CompilationError.WeightConflictingToThirdState.class),
                exNoMin("f=('a':'a' 3|'a':'a' 3)'c'", CompilationError.WeightConflictingToThirdState.class, ps("ac;a")),
                exNoMin("f=('a':'' 3|'a':'' 3)'c'", CompilationError.WeightConflictingToThirdState.class, ps("ac;")),
                exNoMin("f=('a' 3|'a' 3)'c'", CompilationError.WeightConflictingToThirdState.class, ps("ac;")),
                a(" f='a' f<:'a'->''", ps("a;"), "b", "c", "", " "),
                a(" f='' f<:''->''", ps(";"), "a", "b", "c", "aa", " "),
                ex2("f='a' f<:''->'' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:'a'->'a' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:'b'->'' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:'aa'->'' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:''*->'' ", CompilationError.TypecheckException.class),
                ex2("f=missingFunc ! ('test', 'ter', 'otr')", CompilationError.UndefinedExternalFunc.class),
                ex2("f=missingFunc! ('test':'', 'ter':'re', 'otr':'te')", CompilationError.UndefinedExternalFunc.class),
                ex2(" f='a'  f<:'a'*->''", CompilationError.TypecheckException.class),
                a("f='a'*   f<:'a'*->'' ", ps(";", "a;", "aa;", "aaaaa;"), "b", "c", " "),
                a("f='a'*  f<:'a'->'' ", ps(";", "a;", "aa;", "aaaaa;"), "b", "c", " "),
                ex2("f='':''  f<:''|'e'|'r'->''|'' ", CompilationError.TypecheckException.class),
                a("f=(''|'e'|'r'):''  f<:''->''|'' ", ps(";", "e;", "r;"), "a", "b", "c", "aa", " "),
                ex2("f='a':''  f<:'a' 'b'*->'e'* ", CompilationError.TypecheckException.class),
                a("f=('a' 'b'*):''  f<:'a'->'e'* ", ps("a;", "ab;", "abb;"), "aa", "b", "c", "", " "),
                ex2("f='a':'a'  f<:'a'|'c'->'rre'|'a'* ", CompilationError.TypecheckException.class),
                a("f=('a'|'c'):'a'  f<:'a'->'rre'|'a'* ", ps("a;a", "c;a"), "b", "", " "),
                a("f=('a'|'c'):'a'  f<:'c'->'rre'|'a'* ", ps("a;a", "c;a"), "b", "", " "),
                ex2("f='':'a' 'a'  f<:'a'* -> 'a'* ", CompilationError.TypecheckException.class),
                a("f='':'a' 'a'*  f<:'a' -> 'a'* ", ps("a;a", "aaa;a", "aaaaaaa;a", ";a"), "b", "c", " "),
                ex2("f='a':'aa'  f<:'b'* 'a' -> 'aa'* ", CompilationError.TypecheckException.class),
                a("f=('b'* 'a'):'aa'  f<:'a' -> 'aa'* ", ps("a;aa", "ba;aa", "bbba;aa"), "b", "c", "", " "),
                ex2("" +
                        "f=(1 'a':'x' 3 | 2 'a':'y' 2)( 1000 'a':'x' 2 | 1000 'a':'y'3)" +
                        "f<:.*->.* ", CompilationError.TypecheckException.class),
                a("" +
                        "f=(1 'a':'x' 3 | 2 'a':'y' 2)( 1000 'a':'x' 2 | 1000 'a':'y'3) " +
                        "f<:#->.* ", ps("aa;xy"), "", "a", "b", " "),
                ex2("!!A=[0-1] " +
                        "f='01':'10' " +
                        "f<:A*->A* " , CompilationError.TypecheckException.class),
                a("A=[0-1] "  +
                        "f='01':'10' "+
                        "f<:'01'->A* ", ps("01;10"), "", "1", "0", "010"),
                a("A=[0-1] "  +
                        "f='01':'10' "+
                        "f<:#->A* ", ps("01;10"), "", "1", "0", "010"),
                ex2("A=[0-1]" +
                        "B=[0-9]" +
                        "f=[9-0]:'10' "+
                        "f<:B*->A*" , CompilationError.TypecheckException.class),
                a("A=[0-1]" +
                        "B=[0-9]" +
                        "f=[9-0]:'10' "+
                        "f<:[0-9]->A*" , ps("9;10", "4;10", "7;10", "0;10"), "", "10", "20", "65"),
                a("A=[0-1]" +
                        "B=[0-9]"  +
                        "f=[9-0]:'10' "+
                        "f<:[0-4]->A*", ps("9;10", "4;10", "7;10", "0;10"), "", "10", "20", "65"),
                a("A=[0-1]" +
                        "B=[0-9]" +
                        "f=[9-0]:'10' "+
                        "f<:[3-4]->A*" , ps("9;10", "4;10", "7;10", "0;10"), "", "10", "20", "65"),
                a("!!g='abc':'0'|'def':'1' f= g g g",
                        ps("abcdefabc;010", "abcabcabc;000", "defdefdef;111", "defabcdef;101"),
                        "", "abc", "def", "abcabc", "abcabcabcabc", "defdefdefdef"),
                a("!!g='aab' h= g g g g f='aabaabaabaab' f<:h->'' ",
                        ps("aabaabaabaab;"), "", "abc", "def", "aa", "aabaabaabaaba", "aabaabaabaa"),
                a("f=('':'\\0' .)*", ps(";", "a;a", "afeee;afeee",
                        "\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?;\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?")),
                a("f=('':<0> .)*", ps(";", "a;a", "afeee;afeee",
                        "\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?;\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?")),
                a("f=#", ps(), "", "a", "b", "aa", " ", "1", "0", "!", "'", ",", ";", ")", ".", "#", "$", "\n"),
                t("[a-a]:'a' | [b-c]:'b' | [d-e]:'d' | [f-g]:'f' | [h-i]:'h'",
                        ps("a;a", "b;b", "c;b", "d;d", "e;d", "f;f", "g;f", "h;h", "i;h"), "`", "j", "", " "),
                t("[a-a]:'a' |  [d-e]:'d' | [f-g]:'f' | [h-i]:'h'",
                        ps("a;a", "d;d", "e;d", "f;f", "g;f", "h;h", "i;h"), "`", "j", "b", "c"),
                t("[a-a]:'a' | [b-c]:'b' |  [f-g]:'f' | [h-i]:'h'",
                        ps("a;a", "b;b", "c;b", "f;f", "g;f", "h;h", "i;h"), "`", "j", "d", "e"),
                t("[a-a]:'a' | [b-e]:'b' |  [f-g]:'f' | [h-i]:'h'",
                        ps("a;a", "b;b", "c;b", "d;b", "e;b", "f;f", "g;f", "h;h", "i;h"), "`", "j", "", " "),
                t("[a-h]:'a' 1 | [e-m]:'b' 2 |  [k-m]:'f' 3",
                        ps("a;a", "b;a", "c;a", "d;a", "e;b", "f;b", "g;b", "h;b", "i;b", "j;b", "k;f", "l;f", "m;f"), "`", "n", "o", "p"),
                t(".:'a' 2 | [b-e]:'b' |  [f-g]:'f' | [h-i]:'h'",
                        ps("a;a", "b;a", "c;a", "d;a", "e;a", "f;a", "g;a", "h;a", "i;a", "`;a", "j;a"), "", "  ", "ee"),
                t("'abcd' 'efgh' 'ijkl' 'mnop'", ps("abcdefghijklmnop;"), "", "  ", "ee"),
                t("'abcd':'1' 'efgh':'2' 'ijkl':'3' 'mnop':'4'", ps("abcdefghijklmnop;1234"), "", "  ", "ee"),
                t("'':'1' '':'2' '':'3' '':'4'", ps(";1234"), "a", "  ", "ee"),
                t("'a':'1' 'b':'2' | 'c':'3' 'de':'4'", ps("ab;12", "cde;34"), "a", "  ", "ee"),
                t("'x' ('a':'1' 'b':'2' | 'c':'3' 'de':'4')", ps("xab;12", "xcde;34"), "a", "  ", "ee"),
                t("'x' ('a':'1' 'b':'2' | 'c':'3' 'de':'4') 'y'", ps("xaby;12", "xcdey;34"), "a", "  ", "ee"),
                ex2("f='':<0> [a-b] f<:[a-z]->[a-z] ", CompilationError.TypecheckException.class),
                a(" f='':<0> [a-b] f<:[a-z] && [a-z]", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a(" f='':<0> [a-b] f<:[a-b]->[a-z]", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a("f='':<0> [a-b]  f<:[a-a]->[a-z] ", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a(" f='':<0> [a-b]  f<:[a-b]->[a-b]", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a(" f='':<0> [a-b]  f<:[a-b]->'a'|'b'", ps("a;a", "b;b"), "`", "c", "d", "e"),
                ex2("f='':<0> [a-b]  f<:'a'&&. ", CompilationError.TypecheckException.class),
                ex2(" f='':<0> [a-b]  f<:'a'&&[a-b]", CompilationError.TypecheckException.class),
                ex2("f='':<0> [a-b]  f<:[a-z]&&# ", CompilationError.TypecheckException.class),
                ex2("f='':<0> [a-b]  f<:[a-z]-># ", CompilationError.TypecheckException.class),
                ex2(" f='':<0> [a-b]  f<:[a-z]->'a'", CompilationError.TypecheckException.class),
                ex2(" f='':<0> [a-b]  f<:[a-z]->'b'", CompilationError.TypecheckException.class),
                a(" f='':<0> [a-b]  f<:.* && .*", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a(" f='':<0> [a-b]  f<:.* && [a-b]", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a(" f='':<0> [a-b]  f<:. && [a-b]", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a(" f='':<0> [a-b]  f<:[a-z] && [a-b]", ps("a;a", "b;b"), "`", "c", "d", "e"),
                a(" f='':<0> [a-b]  f<:'a'->[a-b]", ps("a;a", "b;b"), "`", "c", "d", "e"),
                ex2(" f='':<0> 'a'  f<:[a-b]->[a-b]", CompilationError.TypecheckException.class),
                ex2("f='':'a'*", CompilationError.KleeneNondeterminismException.class),
                ex2("f='':'a'+", CompilationError.KleeneNondeterminismException.class),
                ex2("f='':'a'?", CompilationError.KleeneNondeterminismException.class),
                ex2(" f='':<0> ([a-b] | [d-e])  f<:([a-z]|[d-e])->([a-z]|[d-e])", CompilationError.NondeterminismException.class),
                a(" f='':<0> ([a-b] | [d-e])  f<:([a-b]|[d-e])->([a-b]|[d-e])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:([a-b]|[d-e])&&([a-b]|[d-e])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:([a-e])&&([a-b]|[d-e])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a("f='':<0> ([a-b] | [d-e])  f<:([a-e])&&.* ", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a("f='':<0> ([a-b] | [d-e])  f<:.&&.* ", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                ex2(" f='':<0> ([a-b] | [d-e])  f<:([a-z]|[d-e])->([a-e])", CompilationError.NondeterminismException.class),
                a(" f='':<0> ([a-b] | [d-e])  f<:([a-b]|[d-e])->([a-e])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:([a-b]|[d-e])->([a-b]|[d-e])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                ex2(" f='':<0> ([a-b] | [d-e])  f<:([a-e])->([a-z]|[d-e])", CompilationError.NondeterminismException.class),
                ex2(" f='':<0> ([a-b] | [d-e])  f<:([a-e])->([a-b]|[d-e])", CompilationError.TypecheckException.class),
                a(" f='':<0> ([a-b] | [d-e])  f<:([a-e])&&([a-b]|[d-e])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:([a-b] | [d-e])->([a-b]|[d-e])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a("f='':<0> ([a-b] | [d-e])  f<:#->([a-b]|[d-e]) ", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                ex2("f='':<0> ([a-b] | [d-e])  f<:.->. ", CompilationError.TypecheckException.class),
                a(" f='u'[a-b]'x' | 'v'[d-e]'y'  f<:'u'[a-b]'x'->''", ps("uax;", "ubx;", "vdy;", "vey;"), "`", "c", "f", "g"),
                a("f='':<0> ([a-b] | [d-e])  f<:[a-b]->. ", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:[a-b]->.", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> .  f<:.->.", ps("a;a", "b;b", "d;d", "e;e", "`;`", "c;c", "f;f", "g;g"), "aa"),
                ex2(" f='':<0> ([a-b] | [d-e])   f<:.->('a'|'b'|'d'|'e'|'')", CompilationError.TypecheckException.class),
                a(" f='':<0> ([a-b] | [d-e])  f<:.&&('a'|'b'|'d'|'e'|'')", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:#->('a'|'b'|'d'|'e'|'')", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                ex2(" f='':<0> ([a-b] | [d-e])  f<:.->('a'|'b'|'d'|'e'|#)", CompilationError.TypecheckException.class),
                a(" f='':<0> ([a-b] | [d-e])  f<:.&&('a'|'b'|'d'|'e'|#)", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:#->('a'|'b'|'d'|'e'|#)", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='a':'xyz'  f<:'a'->'xyz'", ps("a;xyz"), "`", "c", "f", "g"),
                a(" f='':'xyz'  f<:''->'xyz'", ps(";xyz"), "`", "c", "f", "g"),
                t("rpni!('a','aa':#,'aaa','aaaa':#)", ps("a;", "aaa;", "aaaaa;", "aaaaaaa;"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                t("rpni_edsm!('a','aa':#,'aaa','aaaa':#)", ps("a;", "aaa;", "aaaaa;", "aaaaaaa;"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                a(" f='xyz' | 'xy'  f<:'xy'(''|'z')&&''", ps("xy;", "xyz;"), "`", "c", "f", "g"),
                a(" f='xyz' | 'xy'  f<:'xy' 'z'?&&''", ps("xy;", "xyz;"), "`", "c", "f", "g"),
                a(" f='xyz' | 'xy'  f<:'xy'->''", ps("xy;", "xyz;"), "`", "c", "f", "g"),
                t("dict!('a':'be')", ps("a;be"),"","aa","efr","etrry"),
                t("dict!('a':'be','aa':'ykfe','aaa','idw':'gerg','ferf':'fer','ded':'ret','ueh':'grge','efr':#,'etrry':#)",
                        ps("a;be", "aa;ykfe", "aaa;","idw;gerg", "ferf;fer","ded;ret","ueh;grge"),"","aaaa","aaaaa","efr","etrry"),
                a("g = 'a' " +
                        "f = g", ps("a;"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                ex2("g = 'a' " +
                        "f = g g", CompilationError.MissingFunction.class),
                a("g = 'a' " +
                        "f = !!g g", ps("aa;"), "", "a", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                a("g = 'a' " +
                        "f = !!g !!g g", ps("aaa;"), "", "a", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                ex2("g = 'a' " +
                        "f = !!g g g", CompilationError.MissingFunction.class),
                a("!!g = 'a' " +
                        "f = g g g", ps("aaa;"), "", "a", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                a("g = 'a' " +
                        "g = !!g g " +
                        "f = !!g !!g g", ps("aaaaaa;"), "", "a", "aaaa", "aaaaa", "aaaaaaaa", "`", "c", "f", "g"),

        };

        int i = 0;
        for (TestCase testCase : testCases) {
            String input = null;
            try {
                CLI.OptimisedHashLexTransducer tr = new CLI.OptimisedHashLexTransducer(testCase.regex, false);
                LexUnicodeSpecification.Var<HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> g = tr.getTransducer("f");
                Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> o = tr.getOptimisedTransducer("f");
                assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.exception, null);
                if (testCase.numStates > -1) {
                    assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.numStates, tr.getOptimisedTransducer("f").graph.size());
                }
                for (Positive pos : testCase.positive) {
                    input = pos.input;
                    final String out = tr.run("f", pos.input);
                    final String exp = pos.output;
                    assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + pos.input, exp, out);
                }
                for (String neg : testCase.negative) {
                    input = neg;
                    final String out = tr.run("f", neg);
                    assertNull("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + input, out);
                }
                final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> dfa = tr.specs.powerset(o, tr.specs::successor, tr.specs::predecessor);
                for (Positive pos : testCase.positive) {
                    assertTrue("DFA idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + pos.input,
                            tr.specs.accepts(dfa, pos.input.codePoints().iterator()));
                }
                for (String neg : testCase.negative) {
                    assertFalse("DFA idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + input,
                            tr.specs.accepts(dfa, neg.codePoints().iterator()));
                }

                boolean shouldInversionFail = false;
                final HashSet<String> outputs = new HashSet<>();
                for(Positive pos:testCase.positive){
                    if(!outputs.add(pos.output)){
                        shouldInversionFail = true;
                        break;
                    }
                }
                try {
                    tr.specs.inverse(g.graph);
                    o = tr.specs.optimiseGraph(g.graph);
                    tr.specs.checkStrongFunctionality(o);
                    assertEquals("INV idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o ,false,shouldInversionFail);

                    for(Positive pos:testCase.positive){
                        input = pos.output;
                        final String out = tr.specs.evaluate(o, input);
                        final String exp = pos.input;
                        assertEquals("INV idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + input, exp, out);
                    }
                }catch (CompilationError e){
                    if(!shouldInversionFail)e.printStackTrace();
                    assertEquals("INV idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o+"\n"+e ,true ,shouldInversionFail);

                }
            } catch (Throwable e) {
                if(e instanceof ComparisonFailure)throw e;
                if (testCase.exception != null) {
                    if (!testCase.exception.equals(e.getClass())) {
                        e.printStackTrace();
                    }
                    assertEquals("INV idx=" + i + "\nregex=" + testCase.regex + "\n", testCase.exception, e.getClass());
                } else {
                    throw new Exception(i + " INV{" + testCase.regex + "}'" + input + "';" + e.getClass() + " " + e.getMessage(), e);
                }
            }
            try {
                CLI.OptimisedHashLexTransducer tr = new CLI.OptimisedHashLexTransducer(testCase.regex, false);
                LexUnicodeSpecification.Var< HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> g = tr.getTransducer("f");
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                tr.specs.compressBinary(g.graph,new DataOutputStream(s));
                final HashMapIntermediateGraph<Pos, E, P> decompressed = tr.specs.decompressBinary(Pos.NONE,new DataInputStream(new ByteArrayInputStream(s.toByteArray())));
                Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> o = tr.specs.optimiseGraph(decompressed);
                assertEquals("BIN\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.exception, null);
                if (testCase.numStates > -1) {
                    assertEquals("BIN\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.numStates, tr.getOptimisedTransducer("f").graph.size());
                }
                for (Positive pos : testCase.positive) {
                    input = pos.input;
                    final String out = tr.specs.evaluate(o, pos.input);
                    final String exp = pos.output;
                    assertEquals("BIN\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + pos.input, exp, out);
                }
                for (String neg : testCase.negative) {
                    input = neg;
                    final String out = tr.specs.evaluate(o, neg);
                    assertNull("BIN\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + input, out);
                }
                final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> dfa = tr.specs.powerset(o, tr.specs::successor, tr.specs::predecessor);
                for (Positive pos : testCase.positive) {
                    assertTrue("BIN DFA\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + pos.input,
                            tr.specs.accepts(dfa, pos.input.codePoints().iterator()));
                }
                for (String neg : testCase.negative) {
                    assertFalse("BIN DFA\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + input,
                            tr.specs.accepts(dfa, neg.codePoints().iterator()));
                }
            } catch (Throwable e) {
                if(e instanceof ComparisonFailure)throw e;
                if (testCase.exception != null) {
                    if (!testCase.exception.equals(e.getClass())) {
                        e.printStackTrace();
                    }
                    assertEquals("BIN\nidx=" + i + "\nregex=" + testCase.regex + "\n", testCase.exception, e.getClass());
                } else {
                    throw new Exception(i + "BIN{" + testCase.regex + "}'" + input + "';" + e.getClass() + " " + e.getMessage(), e);
                }
            }
            try {
                CLI.OptimisedHashLexTransducer tr = new CLI.OptimisedHashLexTransducer(testCase.regex, true);
                LexUnicodeSpecification.Var< HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> g = tr.getTransducer("f");
                Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> o = tr.getOptimisedTransducer("f");
                assertEquals("MIN\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.exceptionAfterMin, null);
                if (testCase.numStatesAfterMin > -1) {
                    assertEquals("minimised\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.numStatesAfterMin,
                            tr.getOptimisedTransducer("f").graph.size());
                }
                for (Positive pos : testCase.positive) {
                    input = pos.input;
                    final String out = tr.run("f", pos.input);
                    final String exp = pos.output;
                    assertEquals("MIN\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + pos.input, exp, out);
                }
                for (String neg : testCase.negative) {
                    input = neg;
                    final String out = tr.run("f", neg);
                    assertNull("MIN\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + input, out);
                }
                final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> dfa = tr.specs.powerset(o, tr.specs::successor, tr.specs::predecessor);
                for (Positive pos : testCase.positive) {
                    assertTrue("MIN DFA idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + pos.input,
                            tr.specs.accepts(dfa, pos.input.codePoints().iterator()));
                }
                for (String neg : testCase.negative) {
                    assertFalse("MIN DFA idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + input,
                            tr.specs.accepts(dfa, neg.codePoints().iterator()));
                }
            } catch (Throwable e) {
                if(e instanceof ComparisonFailure)throw e;
                if (testCase.exceptionAfterMin != null) {
                    assertEquals("MIN\nidx=" + i + "\nregex=" + testCase.regex + "\n", testCase.exceptionAfterMin, e.getClass());
                } else {
                    throw new Exception(i + "MIN{" + testCase.regex + "}'" + input + "';" + e.getClass() + " " + e.getMessage(), e);
                }
            }
            i++;
        }

    }

    static class PipelineTestCase {
        final String code;
        final Positive[] ps;
        final String[] negative;

        PipelineTestCase(String code, Positive[] ps, String[] negative) {
            this.code = code;
            this.ps = ps;
            this.negative = negative;
        }
    }

    static PipelineTestCase p(String code, Positive[] ps, String... negative) {
        return new PipelineTestCase(code, ps, negative);
    }

    @Test
    void testPipelines() throws Exception {
        PipelineTestCase[] cases = {
                p("@f = 'a':'b';", ps("a;b"), ""),
                p("@f = 'a':'b'; 'b' : 'c' ;", ps("a;c"), "","b","c","d","aa"),
                p("@f = 'a':'b'; 'b' : 'c' ; 'c' : 'd' ;", ps("a;d"), "","b","c","d","aa"),
                p("@f = 'a':'b' {'b'} 'b' : 'c' ;", ps("a;c"), "","b","c","d","aa"),
                p("@f = 'a':'b' {'b'} 'b' : 'c' {'c'} 'c' : 'd' ;", ps("a;d"), "","b","c","d","aa"),
                p("@f = 'a':'b'|'h':'i' {'b'|'i'} 'b' : 'c'|'i':'j' {'c'|'j'} 'c' : 'd' |'j':'k';", ps("a;d","h;k"), "","b","c","d","aa"),
                p("@g = 'a':'b' {'b'} 'b' : 'c' {'c'}" +
                        "@f = @g ; 'c' : 'd' {'d'} ", ps("a;d"), "","b","c","d","aa"),
        };

        for (PipelineTestCase caze : cases) {
            CLI.OptimisedHashLexTransducer tr = new CLI.OptimisedHashLexTransducer(caze.code, true);
            LexUnicodeSpecification.LexPipeline<HashMapIntermediateGraph.N<Pos, LexUnicodeSpecification.E>, HashMapIntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P>> g = tr.getPipeline("f");
            for (Positive pos : caze.ps) {
                String out = g.evaluate(pos.input);
                assertEquals(pos.output, out);
            }
            for (String neg : caze.negative) {
                assertNull(g.evaluate(neg));
            }

        }
    }
}
