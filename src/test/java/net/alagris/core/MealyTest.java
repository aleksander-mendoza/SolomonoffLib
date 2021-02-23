package net.alagris.core;

import net.alagris.core.*;
import net.alagris.core.learn.OSTIA;
import net.alagris.core.learn.OSTIAState;
import net.alagris.lib.Config;
import net.alagris.lib.ArrayBacked;
import net.alagris.lib.HashMapBacked;
import net.alagris.lib.Solomonoff;
import org.antlr.v4.runtime.CharStreams;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.ComparisonFailure;
import org.junit.jupiter.api.Test;

import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.core.LexUnicodeSpecification.Var;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;

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
        private final String[] equivalentRegexes;
        private final Positive[] positive;
        private final String[] negative;
        private final Class<? extends Throwable> exception;
        private final Class<? extends Throwable> exceptionAfterMin;
        private final int numStates;
        private final boolean skipGenerator;

        public TestCase(String regex, String[] eqRegex, Positive[] positive, String[] negative, Class<? extends Throwable> exception,
                        Class<? extends Throwable> exceptionAfterMin, int numStates, boolean skipGenerator) {
            this.equivalentRegexes = eqRegex;
            this.regex = regex;
            this.positive = positive;
            this.negative = negative;
            this.exception = exception;
            this.exceptionAfterMin = exceptionAfterMin;
            this.numStates = numStates;
            this.skipGenerator = skipGenerator;
        }
    }

    static String[] eq(String... e) {
        return e;
    }

    static TestCase ex(String regex, Class<? extends Throwable> exception, Class<? extends Throwable> exceptionAfterMin) {
        assert exceptionAfterMin != null;
        assert exception != null;
        return new TestCase(regex, eq(), null, null, exception, exceptionAfterMin, -1,  false);
    }

    static TestCase exAfterMinNG(String regex, Positive[] positive, Class<? extends Throwable> exceptionAfterMin,String... negative) {
        assert exceptionAfterMin != null;
        return new TestCase(regex, eq(), positive, negative, null, exceptionAfterMin, -1,  true);
    }

    static TestCase exNoMin(String regex, Class<? extends Throwable> exception, Positive[] positive, String... negative) {
        assert exception != null;
        return new TestCase(regex, eq(), positive, negative, exception, null, -1,  false);
    }

    static TestCase ex2(String regex, Class<? extends Throwable> exception) {
        return new TestCase(regex, eq(), null, null, exception, exception, -1,  false);
    }

    static TestCase t(String regex, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, eq(), positive, negative, null, null, -1, false);
    }

    /**
     * NG=No generator
     */
    static TestCase tNG(String regex, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, eq(), positive, negative, null, null, -1,  true);
    }

    static TestCase t(String regex, int states, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, eq(), positive, negative, null, null, states, false);
    }

    static TestCase tEq(String regex, String[] eqRegex, int states, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, eqRegex, positive, negative, null, null, states, false);
    }

    static TestCase tNG(String regex, int states, Positive[] positive, String... negative) {
        return new TestCase("f=" + regex, eq(), positive, negative, null, null, states, true);
    }

    static TestCase a(String regex, Positive[] positive, String... negative) {
        return new TestCase(regex, eq(), positive, negative, null, null, -1, false);
    }
    static TestCase aNG(String regex, Positive[] positive, String... negative) {
        return new TestCase(regex, eq(), positive, negative, null, null, -1,  true);
    }
    static TestCase a(String regex, int states, Positive[] positive, String... negative) {
        return new TestCase(regex, eq(), positive, negative, null, null, states, false);
    }


//    @Test
//    void tmp() throws Exception {
//        final Alphabet<Character> alphabet = Alphabets.characters('a', 'b');
//        final BlueFringeRPNIMealy<Character,Character> learner = new BlueFringeRPNIMealy<>(alphabet);
//
//        learner.addSample(Word.fromSymbols('a','a'),Word.fromSymbols('0','1'));
//        learner.addSample(Word.fromSymbols('a'),Word.fromSymbols('0'));
//
//        // if no training samples have been provided, only the empty automaton can be constructed
//        final MealyMachine<?, Character, ?, Character> emptyModel = learner.computeModel();
//        Visualization.visualize(emptyModel, alphabet);
//
//
//    }

    @Test
    void testEagerCopy() throws Exception {

        TestCase[] testCases = {
                a("g = 'a' \n " +
                        "f = g g g !!g",  ps("aaaa;"), "a","b", "c", "", " "),
                a("g = 'a' \n " +
                        "f = g g g g",  ps("aaaa;"), "a","b", "c", "", " "),

                ex2("g = 'a' \n " +
                        "f = g g !!g g",  CompilationError.MissingFunction.class),
                a("!!g = 'a' \n " +
                        "f = g g g g",  ps("aaaa;"), "a","b", "c", "", " "),
                a("!!g = 'a' \n " +
                        "f = g !!g g g",  ps("aaaa;"), "a","b", "c", "", " "),
                a("!!g = 'a' \n " +
                        "f = !!g !!g !!g !!g ",  ps("aaaa;"), "a","b", "c", "", " "),
                a("f = 'a' \n " +
                        "f = f f f !!f",  ps("aaaa;"), "a","b", "c", "", " "),
                a("f = 'a' \n " +
                        "f = f f f f",  ps("aaaa;"), "a","b", "c", "", " "),

                ex2("f = 'a' \n " +
                        "f = f f !!f f",  CompilationError.MissingFunction.class),

        };
        int i = 0;
        for (TestCase testCase : testCases) {
            String input = null;
            try {
                begin(testCase, i);
                HashMapBacked tr = new HashMapBacked(Config.config().eagerCopy());
                tr.specs.setVariableRedefinitionCallback((prevVar, newVar, position) -> {});
                tr.parse(CharStreams.fromString(testCase.regex));
                tr.checkStrongFunctionality();

                LexUnicodeSpecification.Var<HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> g = tr.getTransducer("f");
                Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> o = tr.getOptimisedTransducer("f");
                assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.exception, null);
                if (testCase.numStates > -1) {
                    assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.numStates, o.graph.size());
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

            } catch (Throwable e) {
                if (e instanceof ComparisonFailure) throw e;
                if (testCase.exception != null) {
                    if (!testCase.exception.equals(e.getClass())) {
                        e.printStackTrace();
                    }
                    assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n", testCase.exception, e.getClass());
                } else {
                    throw new Exception(i + " {" + testCase.regex + "}'" + input + "';" + e.getClass() + " " + e.getMessage(), e);
                }
            }
        }
    }
    
    
    interface Constructor<N, G extends IntermediateGraph<Pos, E, P, N>>{
    	Solomonoff<N, G> cons(Config config) throws CompilationError;
    }

    @Test
    void testHash() throws Exception {
    	test(HashMapBacked::new);
    }

    @Test
    void testArray() throws Exception {
    	test(ArrayBacked::new);
    }
    
    
    <N, G extends IntermediateGraph<Pos, E, P, N>> void test(Constructor<N,G> cons) throws Exception {

        TestCase[] testCases = {
                aNG("f = ostia!('one':'1','o':[],'on':[])",
                        ps("one;1","one;1"),"o","on"),
                aNG("f = ostia!('a':'b','aaa':'bbb','aaaaa':'bbbbb','aa':[],'aaaa':[])",
                        ps("aaaaa;bbbbb","a;b","aaa;bbb","aaaaaaa;bbbbbbb","aaaaaaaaa;bbbbbbbbb"),"aaaaaaaa","aaaaaa","aaaa","","aa"),
                aNG("f = ostia!('one':'1')",
                        ps("one;1","o;1")),
                aNG("f = ostia!('one':'1','two':'2','three':'3')",
                        ps("one;1","one;1","two;2","three;3")),
                aNG("f = ostia!('aa':'','aaaa':'a','aaaaaa':'aa','aaaaaaaa':'aaa')",
                        ps("aa;","aaaa;a","aaaaaa;aa","aaaaaaaa;aaa","aaaaaaaaaa;aaaa","aaaaaaaaaaaa;aaaaa")),
                aNG("f = ostia!('one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22','twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100")),
                aNG("f = ostia!('a':[],'b':[],'c':[],'d':[],'e':[],'f':[],'g':[],'h':[],'i':[],'j':[],'k':[],'l':[],'m':[],'n':[],'o':[],'p':[],'q':[],'on':[],'t':[],'tw':[],'th':[],'thr':[],'thre':[],'fi':[],'fo':[],'fiv':[],'fou':[],'one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22','twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100','one hundred one':'101','two hundred':'200','two hundred eighty three':'283','three hundred':'300','three hundred fifty one':'351','four hundred':'400','four hundred seventeen':'417','five hundred':'500','five hundred seventy seven':'577','six hundred':'600','six hundred forty five':'645','seven hundred':'700','seven hundred sixteen':'716','eight hundred':'800','eight hundred one':'801','eight hundred forty six':'846','eight hundred fifteen':'815','nine hundred':'900','nine hundred seventy four':'974','nine hundred seventy three':'973','nine hundred seventy two':'972','nine hundred seventy nine':'979','nine hundred ninety nine':'999')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100","one hundred one;101","two hundred;200","three hundred fifty one;351","four hundred seventeen;417","five hundred seventy seven;577","six hundred forty five;645","seven hundred sixteen;716","eight hundred one;801","eight hundred forty;840","nine hundred ninety nine;999","nine hundred seventy four;974","nine hundred seventy three;973","nine hundred seventy two;972"),
                        "a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","on"),
                aNG("f = ostia!('one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22','twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100','one hundred one':'101','two hundred':'200','two hundred eighty three':'283','three hundred':'300','three hundred fifty one':'351','four hundred':'400','four hundred seventeen':'417','five hundred':'500','five hundred seventy seven':'577','six hundred':'600','six hundred forty five':'645','seven hundred':'700','seven hundred sixteen':'716','eight hundred':'800','eight hundred one':'801','eight hundred forty six':'846','eight hundred fifteen':'815','nine hundred':'900','nine hundred seventy four':'974','nine hundred seventy three':'973','nine hundred seventy two':'972','nine hundred seventy nine':'979','nine hundred ninety nine':'999','nine hundred ninety four':'994')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100","one hundred one;101","two hundred;200","three hundred fifty one;351","four hundred seventeen;417","five hundred seventy seven;577","six hundred forty five;645","seven hundred sixteen;716","eight hundred one;801","eight hundred forty;840","eight hundred forty one;841","eight hundred forty two;842","nine hundred seventy two;972","nine hundred seventy three;973","nine hundred seventy four;974","nine hundred seventy seven;977","nine hundred ninety four;994","nine hundred ninety five;995","nine hundred ninety nine;999")),
                t("'a'", 2,  ps("a;"), "b", "c", "", " "),
                t("compress!['a']", 2,  ps("a;"), "b", "c", "", " "),
                t("[]", 1, ps(), "a", "b", "c", "", " "),
                t("compress![[]]", 1,  ps(), "a", "b", "c", "", " "),
                tEq("∅", eq("[]", "'a' []", "[] 'a'"), 1,  ps(), "a", "b", "c", "", " "),
                tEq("compress![∅]", eq("[]", "'a' []", "[] 'a'"), 1, ps(), "a", "b", "c", "", " "),
                t(".", 2,  ps("a;", "b;", "c;", "Σ;", "∉;", "𝕄;", "Ω;", "∧;", ".;", "`;", " ;", "\n;", "\t;", "\\;"), "aa", "ba", "cc", "", "    "),
                t("compress![.]", 2,  ps("a;", "b;", "c;", "Σ;", "∉;", "𝕄;", "Ω;", "∧;", ".;", "`;", " ;", "\n;", "\t;", "\\;"), "aa", "ba", "cc", "", "    "),
                tEq("Σ", eq("."), 2, ps("a;", "b;", "c;", "Σ;", "∉;", "𝕄;", "Ω;", "∧;", ".;", "`;", " ;", "\n;", "\t;", "\\;"), "aa", "ba", "cc", "", "    "),
                tEq("compress![Σ]", eq("."), 2,  ps("a;", "b;", "c;", "Σ;", "∉;", "𝕄;", "Ω;", "∧;", ".;", "`;", " ;", "\n;", "\t;", "\\;"), "aa", "ba", "cc", "", "    "),
                t("'a'", 2, ps("a;"), "b", "c", "", " "),
                t("compress!['a']", 2, ps("a;"), "b", "c", "", " "),
                t("'a'|[]", 2,  ps("a;"), "b", "c", "", " "),
                t("compress!['a'|[]]", 2,  ps("a;"), "b", "c", "", " "),
                t("[]|'a'", 2,  ps("a;"), "b", "c", "", " "),
                t("compress![[]|'a']", 2,  ps("a;"), "b", "c", "", " "),
                t("[]|'aa'", 3, ps("aa;"), "b", "c", "", " "),
                t("compress![[]|'aa']", 3, ps("aa;"), "b", "c", "", " "),
                t("'a' 'a'", 3, ps("aa;"), "b", "c", "", " "),
                t("compress!['a' 'a']", 3, ps("aa;"), "b", "c", "", " "),
                t("'a' 'b'", 3, ps("ab;"), "b", "c", "", " "),
                t("compress!['a' 'b']", 3, ps("ab;"), "b", "c", "", " "),
                t("'a' ∙ 'b'", 3, ps("ab;"), "b", "c", "", " "),
                t("compress!['a' ∙ 'b']", 3, ps("ab;"), "b", "c", "", " "),
                t("'a' ∙ 'b' ∙ 'c' ∙ 'd'", 5, ps("abcd;"), "b", "c", "", " "),
                t("compress!['a' ∙ 'b' ∙ 'c' ∙ 'd']", 5, ps("abcd;"), "b", "c", "", " "),
                t("'a' ∙ 'bb' ∙ 'ccc' ∙ '' ∙ '' ∙ '' ∙ 'd'", 8, ps("abbcccd;"), "b", "c", "", " "),
                t("compress!['a' ∙ 'bb' ∙ 'ccc' ∙ '' ∙ '' ∙ '' ∙ 'd']",  8, ps("abbcccd;"), "b", "c", "", " "),
                t("'aa'|[]", 3, ps("aa;"), "b", "c", "", " "),
                t("compress!['aa'|[]]", 3, ps("aa;"), "b", "c", "", " "),
                t("'aa' []", 1, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("compress!['aa' []]", 1, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("[] []", 1, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("compress![[] []]", 1, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("[]*", 1, ps(";"), "aa", "a", "aaa", "b", "c", " "),
                t("compress![[]*]",  1, ps(";"), "aa", "a", "aaa", "b", "c", " "),
                t("[]* []", 1, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("compress![[]* []]", 1, ps(), "aa", "a", "aaa", "b", "c", "", " "),
                t("[]* | []", 1, ps(";"), "aa", "a", "aaa", "b", "c", " "),
                t("compress![[]* | []]", 1, ps(";"), "aa", "a", "aaa", "b", "c", " "),
                t("([]*) :'a'", 1, ps(";a"), "aa", "a", "aaa", "b", "c", " "),
                t("compress![([]*) :'a']", 1, ps(";a"), "aa", "a", "aaa", "b", "c", " "),
                t("<97>", 2,  ps("a;"), "b", "c", "", " "),
                t("compress![<97>]", 2, ps("a;"), "b", "c", "", " "),
                t("''", 1, ps(";"), "a", "b", "c", "aa", " "),
                t("compress!['']",  1, ps(";"), "a", "b", "c", "aa", " "),
                t("ε", 1, ps(";"), "a", "b", "c", "aa", " "),
                t("compress![ε]", 1, ps(";"), "a", "b", "c", "aa", " "),
                t("'':''", 1, ps(";"), "a", "b", "c", "aa", " "),
                t("compress!['':'']", 1, ps(";"), "a", "b", "c", "aa", " "),
                t("'a':''", 2, ps("a;"), "aa", "b", "c", "", " "),
                t("compress!['a':'']", 2, ps("a;"), "aa", "b", "c", "", " "),
                t("'a':'a'", 2, ps("a;a"), "b", "c", "", " "),
                t("compress!['a':'a']", 2,ps("a;a"), "b", "c", "", " "),
                t("'':'a' 'a'", 2, ps("a;a"), "b", "c", "", " "),
                t("compress!['':'a' 'a']", 2, ps("a;a"), "b", "c", "", " "),
                t(":'a' 'a'", 2, ps("a;a"), "b", "c", "", " "),
                t("compress![:'a' 'a']", 2, ps("a;a"), "b", "c", "", " "),
                t("'a':'aa'", 2,ps("a;aa"), "b", "c", "", " "),
                t("compress!['a':'aa']", 2, ps("a;aa"), "b", "c", "", " "),
                t("'a':' '", 2, ps("a; "), "b", "c", "", " "),
                t("compress!['a':' ']", 2, ps("a; "), "b", "c", "", " "),
                t("'':'   '",1, ps(";   "), "a", "b", "c", " "),
                t("compress!['':'   ']", 1, ps(";   "), "a", "b", "c", " "),
                t(":'   '", 1, ps(";   "), "a", "b", "c", " "),
                t("compress![:'   ']", 1, ps(";   "), "a", "b", "c", " "),
                t("'a':'TeSt Yo MaN'", 2, ps("a;TeSt Yo MaN"), "b", "c", "", " "),
                t("compress!['a':'TeSt Yo MaN']", 2, ps("a;TeSt Yo MaN"), "b", "c", "", " "),
                t("('a')*",  2, ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("compress![('a')*]", 2, ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("('a')+", 2,  ps("a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", "", " "),
                t("compress![('a')+]", 2, ps("a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", "", " "),
                t("('a')?", 2, ps("a;", ";"), "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("compress![('a')?]", 2, ps("a;", ";"), "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("'a'*",  2, ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("compress!['a'*]", 2, ps(";", "a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", " "),
                t("'a'+",  2, ps("a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", "", " "),
                t("compress!['a'+]", 2, ps("a;", "aa;", "aaa;", "aaaaaaaaaaaaaa;"), "b", "c", "", " "),
                t("'a'?", 2, ps("a;", ";"), "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("compress!['a'?]", 2, ps("a;", ";"), "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                tEq("('abcd')*", eq("('abcdabcd')+ |'abcd' 'abcdabcd'+ | ''", "'abcd'* 2|'abcdabcd'*"), 5, ps(";", "abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;"), "a", "b", "c", " "),
                tEq("compress![('abcd')*]", eq("('abcdabcd')+ |'abcd' 'abcdabcd'+ | ''", "'abcd'* 2|'abcdabcd'*"), 5, ps(";", "abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;"), "a", "b", "c", " "),
                t("('abcd')+", 5, ps("abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;"), "b", "c", "", " "),
                t("compress![('abcd')+]", 5, ps("abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;"), "b", "c", "", " "),
                t("('abcd')?", 5, ps("abcd;", ";"), "abcdabcd", "abcdabcdabcd", "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("compress![('abcd')?]", 5, ps("abcd;", ";"), "abcdabcd", "abcdabcdabcd", "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("('abcd'|'012')*", 8, ps(";", "abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;",
                        "012;", "abcd012;", "abcd012abcd;", "abcd012abcd012abcd;", "012abcdabcdabcdabcd012012abcdabcdabcd;",
                        "012012;", "012abcd;", "abcd012012abcd;", "abcdabcdabcd012012;", "abcdabcd012abcdabcdabcd012012abcdabcd;"), "a", "b", "c", " "),
                t("compress![('abcd'|'012')*]", 7, ps(";", "abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;",
                        "012;", "abcd012;", "abcd012abcd;", "abcd012abcd012abcd;", "012abcdabcdabcdabcd012012abcdabcdabcd;",
                        "012012;", "012abcd;", "abcd012012abcd;", "abcdabcdabcd012012;", "abcdabcd012abcdabcdabcd012012abcdabcd;"), "a", "b", "c", " "),
                t("('abcd'|'012')+", 8, ps("abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;",
                        "012;", "abcd012;", "abcd012abcd;", "abcd012abcd012abcd;", "012abcdabcdabcdabcd012012abcdabcdabcd;",
                        "012012;", "012abcd;", "abcd012012abcd;", "abcdabcdabcd012012;", "abcdabcd012abcdabcdabcd012012abcdabcd;"), "b", "c", "", " "),
                t("compress![('abcd'|'012')+]", 7, ps("abcd;", "abcdabcd;", "abcdabcdabcd;", "abcdabcdabcdabcdabcdabcdabcd;",
                        "012;", "abcd012;", "abcd012abcd;", "abcd012abcd012abcd;", "012abcdabcdabcdabcd012012abcdabcdabcd;",
                        "012012;", "012abcd;", "abcd012012abcd;", "abcdabcdabcd012012;", "abcdabcd012abcdabcdabcd012012abcdabcd;"), "b", "c", "", " "),
                t("('abcd'|'012')?", 8,  ps("abcd;", ";", "012;"), "012012", "abcd012", "abcd012abcd", "abcd012abcd012abcd", "012abcdabcdabcdabcd012012abcdabcdabcd",
                        "012012", "012abcd", "abcd012012abcd", "abcdabcdabcd012012", "abcdabcd012abcdabcdabcd012012abcdabcd", "abcdabcd", "abcdabcdabcd", "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("compress![('abcd'|'012')?]",  7, ps("abcd;", ";", "012;"), "012012", "abcd012", "abcd012abcd", "abcd012abcd012abcd", "012abcdabcdabcdabcd012012abcdabcdabcd",
                        "012012", "012abcd", "abcd012012abcd", "abcdabcdabcd012012", "abcdabcd012abcdabcdabcd012012abcdabcd", "abcdabcd", "abcdabcdabcd", "b", "c", "aa", "aaa", "aaaaaaaaaaaaaa", " "),
                t("''*", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![''*]", 1,  ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''?)*",  1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''?)*]", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''+)*", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''+)*]", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''*)*", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''*)*]", 1,ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''?)+",  1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''?)+]", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''+)+", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''+)+]", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''*)+", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''*)+]", 1,ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''?)?", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''?)?]", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''+)?", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''+)?]", 1,ps(";"), "r", "`", "aa", "b", "c", " "),
                t("(''*)?", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("compress![(''*)?]", 1, ps(";"), "r", "`", "aa", "b", "c", " "),
                t("[1-3]", 2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("compress![[1-3]]", 2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("[3-1]",  2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("compress![[3-1]]", 2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("<[49-51]>", 2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("<[49-51 53-59]>", 2, ps("1;", "2;", "3;","5;","6;","7;","8;","9;"), "11", "12", "13", "31", "5 ", "4","k4", "k", "b", "c", "", " "),
                t("<[49-51 53-59 32 10]>", 2, ps("\n;"," ;","1;", "2;", "3;","5;","6;","7;","8;","9;"), "11", "12", "13", "31", "5 ","4", "k4", "k", "b", "c", ""),
                t("<[49-51 52 32 53-59 10]>", 2, ps("\n;"," ;","1;", "2;", "3;","4;","5;","6;","7;","8;","9;"), "11", "12", "13", "31", "5 ", "k4", "k", "b", "c", ""),
                t("compress![<[49-51]>]", 2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("<[51-49]>", 2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("compress![<[51-49]>]", 2, ps("1;", "2;", "3;"), "11", "12", "13", "31", "5", "4", "0", "b", "c", "", " "),
                t("'ab':'xx'", 3, ps("ab;xx"), "a", "b", "c", "", " "),
                t("compress!['ab':'xx']", 3, ps("ab;xx"), "a", "b", "c", "", " "),
                t("('a'|'b'):'a'", 3, ps("a;a", "b;a"), "e", "c", "", " "),
                t("compress![('a'|'b'):'a']", 2, ps("a;a", "b;a"), "e", "c", "", " "),
                t("[a-z0-9]", 2, ps("a;", "b;","z;","0;","9;"), "`", "{", "", " "),
                t("[a-kn-z0-9]", 2, ps("a;", "b;","k;","n;","z;","0;","9;"), "l","m","`", "{", "", " "),
                t("[a-kn-z0-9 ]", 2, ps("a;", "b;","k;","n;","z;","0;","9;"," ;"), "l","m","`", "{", ""),
                t("[a-kmn-z0-9 ]", 2, ps("a;", "b;","k;","n;","m;","z;","0;","9;"," ;"), "l","`", "{", ""),
                t("[a-klmn-z0-9 ]", 2, ps("a;", "b;","k;","n;","m;","l;","z;","0;","9;"," ;"), "`", "{", ""),
                t("'ab' | 'ab' 1", 5, ps("ab;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("compress![ 'ab' 1]",3, ps("ab;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("compress!['ab' | 'ab' 1]",3, ps("ab;","ab;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("'b' | 'b' 1", 3, ps("b;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "c", "", " "),
                t("compress!['b' | 'b' 1]", 2, ps("b;","b;"), "a", "aab", "aaa", "bbb", "bb", "abb", "aa", "c", "", " "),
                t("'a' | 'ab'", 4, ps("a;", "ab;"), "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("compress!['a' | 'ab']", 3, ps("a;", "ab;"), "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "", " "),
                t("'abc':'rte ()[]te'", 4, ps("abc;rte ()[]te"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb",
                        " abc", "abc "),
                t("compress!['abc':'rte ()[]te']", 4,  ps("abc;rte ()[]te"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb",
                        " abc", "abc "),
                t("'a(b|e)c':'abc'",  8, ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("compress!['a(b|e)c':'abc']", 8,  ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("<97 40 98 124 101 41 99>:<97 98 99>", 8, ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("compress![<97 40 98 124 101 41 99>:<97 98 99>]", 8, ps("a(b|e)c;abc"), "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc",
                        "abc ", "abc", "aec"),
                t("('a'('b'|'e')'c'):'tre'", 5, ps("abc;tre", "aec;tre"), "a", "b", "c", "", " ", "ab", "bb",
                        "cb", "abb", " abc", "abc "),
                t("compress![('a'('b'|'e')'c'):'tre']",  4, ps("abc;tre", "aec;tre"), "a", "b", "c", "", " ", "ab", "bb",
                        "cb", "abb", " abc", "abc "),
                t("(('a'('b'|'e'|'f')'c')):'tre'", 6,  ps("abc;tre", "aec;tre", "afc;tre"), "a", "b", "c", "",
                        " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("compress![(('a'('b'|'e'|'f')'c')):'tre']", 4, ps("abc;tre", "aec;tre", "afc;tre"), "a", "b", "c", "",
                        " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                tNG("((('a'('b'|'e'|'f')*'c'))):'tre'", 6,
                        ps("abc;tre", "aec;tre", "afc;tre", "abbc;tre", "aeec;tre", "affc;tre", "abec;tre", "aefc;tre",
                                "afbc;tre", "abbbeffebfc;tre"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                tNG("compress![((('a'('b'|'e'|'f')*'c'))):'tre']", 3,
                        ps("abc;tre", "aec;tre", "afc;tre", "abbc;tre", "aeec;tre", "affc;tre", "abec;tre", "aefc;tre",
                                "afbc;tre", "abbbeffebfc;tre"),
                        "a", "b", "c", "", " ", "ab", "bb", "cb", "abb", " abc", "abc "),
                t("'a':'x' 'b':'y'", 3, ps("ab;xy"), "a", "b", "c", "", " "),
                t("compress!['a':'x' 'b':'y']", 3,ps("ab;xy"), "a", "b", "c", "", " "),
                t("'a':'x' | 'b':'y' | 'c':'x'| 'd':'y'| 'e':'x'| 'f':'y'", 7, ps("a;x", "b;y", "c;x", "d;y", "e;x", "f;y"), "g", "h", "i", "", "aa"),
                t("compress!['a':'x' | 'b':'y' | 'c':'x'| 'd':'y'| 'e':'x'| 'f':'y']", 3, ps("a;x", "b;y", "c;x", "d;y", "e;x", "f;y"), "g", "h", "i", "", "aa"),
                t("'k'('a':'x' | 'b':'y' | 'c':'x'| 'd':'y'| 'e':'x'| 'f':'y')'l'", 9,  ps("kal;x", "kbl;y", "kcl;x", "kdl;y", "kel;x", "kfl;y"), "g", "h", "i", "", "a", "b", "kl", "kgl"),
                t("compress!['k'('a':'x' | 'b':'y' | 'c':'x'| 'd':'y'| 'e':'x'| 'f':'y')'l']", 5, ps("kal;x", "kbl;y", "kcl;x", "kdl;y", "kel;x", "kfl;y"), "g", "h", "i", "", "a", "b", "kl", "kgl"),
                t("'ax':'x' | 'bx':'y' |'cx':'z'", 7, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("compress!['ax':'x' | 'bx':'y' |'cx':'z']", 7, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("'a':'x' 'x'| 'b':'y' 'x'|'c':'z' 'x'", 7, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("compress!['a':'x' 'x'| 'b':'y' 'x'|'c':'z' 'x']", 5, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("'':'x' 'a' 'x'| '':'y' 'b' 'x'|'':'z' 'c' 'x'", 7, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("compress!['':'x' 'a' 'x'| '':'y' 'b' 'x'|'':'z' 'c' 'x']", 3, ps("ax;x", "bx;y", "cx;z"), "a", "b", "c", "xx", "axax", "xa", ""),
                t("'yxa' | 'yxab'", 8, ps("yxa;", "yxab;"),
                        "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "",
                        "xaab", "xaaa", "xbbb", "xbb", "xabb", "xaa", "xb", "xc", "x",
                        "xxaab", "xxaaa", "xxbbb", "xxbb", "xxbb", "xxaa", "xxb", "xxc", "xx",
                        "yaab", "yaaa", "ybbb", "ybb", "yabb", "yaa", "yb", "yc", "",
                        "yxaab", "yxaaa", "yxbbb", "yxbb", "yxabb", "yxaa", "yxb", "yxc", "yx",
                        "yxxaab", "yxxaaa", "yxxbbb", "yxxbb", "yxxbb", "yxxaa", "yxxb", "yxxc", "yxx"),
                t("compress!['yxa' | 'yxab']", 5, ps("yxa;", "yxab;"),
                        "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "",
                        "xaab", "xaaa", "xbbb", "xbb", "xabb", "xaa", "xb", "xc", "x",
                        "xxaab", "xxaaa", "xxbbb", "xxbb", "xxbb", "xxaa", "xxb", "xxc", "xx",
                        "yaab", "yaaa", "ybbb", "ybb", "yabb", "yaa", "yb", "yc", "",
                        "yxaab", "yxaaa", "yxbbb", "yxbb", "yxabb", "yxaa", "yxb", "yxc", "yx",
                        "yxxaab", "yxxaaa", "yxxbbb", "yxxbb", "yxxbb", "yxxaa", "yxxb", "yxxc", "yxx"),
                t("'xa' | 'xab'", 6, ps("xa;", "xab;"), "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "",
                        "xaab", "xaaa", "xbbb", "xbb", "xabb", "xaa", "xb", "xc", "x", "xxaab", "xxaaa", "xxbbb", "xxbb", "xxbb", "xxaa", "xxb", "xxc", "xx"),
                t("compress!['xa' | 'xab']", 4, ps("xa;", "xab;"), "aab", "aaa", "bbb", "bb", "abb", "aa", "b", "c", "",
                        "xaab", "xaaa", "xbbb", "xbb", "xabb", "xaa", "xb", "xc", "x", "xxaab", "xxaaa", "xxbbb", "xxbb", "xxbb", "xxaa", "xxb", "xxc", "xx"),
                t("'abcdefgh' | ''", 9, ps("abcdefgh;", ";"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("compress!['abcdefgh' | '']", 9, ps("abcdefgh;", ";"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefgh' 1", 17, ps("abcdefgh;"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("compress!['abcdefgh' | 'abcdefgh' 1]", 9, ps("abcdefgh;","abcdefgh;"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefgh' 1 | 'abcdefgh' 2 | 'abcdefgh' 3", 33, ps("abcdefgh;"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("compress!['abcdefgh' | 'abcdefgh' 1 | 'abcdefgh' 2 | 'abcdefgh' 3]", 9, ps("abcdefgh;","abcdefgh;"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefg' | 'abcdef'", 22,
                        ps("abcdefgh;", "abcdefg;", "abcdef;"),
                        "abcdefghh", "aa", "abcdefgha", "abcde", "abcd", "abc", "ab", "a", "", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("compress!['abcdefgh' | 'abcdefg' | 'abcdef']", 9,
                        ps("abcdefgh;", "abcdefg;", "abcdef;"),
                        "abcdefghh", "aa", "abcdefgha", "abcde", "abcd", "abc", "ab", "a", "", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("'abcdefgh' | 'abcdefg' | 'abcdef' | 'abcde' | 'abcd' | 'abc' | 'ab' | 'a' | ''", 37,
                        ps("abcdefgh;", "abcdefg;", "abcdef;", "abcde;", "abcd;", "abc;", "ab;", "a;", ";"),
                        "abcdefghh", "aa", "abcdefgha", "abcdegh", "abcefgh", "abcdefh", "bcdefgh", "abcdefghabcdefgh"),
                t("compress!['abcdefgh' | 'abcdefg' | 'abcdef' | 'abcde' | 'abcd' | 'abc' | 'ab' | 'a' | '']",  9,
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
                tNG("'a':'x' ('b':'y' | 'c':'z')* 'de':'vw'",
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
                t("'a':'a' 2 | 'a':'b' 3", 3,  ps("a;b"), "b", "c", "", " "),
                t("compress!['a':'a' 2 | 'a':'b' 3]", 2, ps("a;b"), "b", "c", "", " "),
                t("'a':'x'2|'a':'y'3", 3, ps("a;y"), "b", "c", "", "aa", "`"),
                t("compress!['a':'x'2|'a':'y'3]", 2, ps("a;y"), "b", "c", "", "aa", "`"),
                t("(1 'a':'x' 2 | 2 'a':'y' 3) ", ps("a;y"), "", "aa", "b", " "),
                t("(1 'a':'x' 2 | 2 '':'y' 3) ", ps("a;x", ";y"), "aa", "b", " "),
                t("('a':'a' 2 | 'a':'b' 3) 'a':'a'", ps("aa;ba"), "a", "ab", "b", "ba", "bb", "c", "", " "),
                t("(1 'a':'x' 2 | 2 'a':'y' 3)( 'a':'x' 2 |'a':'y'3) ", ps("aa;yy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)( 'a':'x' 2 |'a':'y'3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)( 1000 'a':'x' 2 |'a':'y'3) ", 5, ps("aa;xy"), "", "a", "b", " "),
                t("compress![(1 'a':'x' 3 | 2 'a':'y' 2)( 1000 'a':'x' 2 |'a':'y'3)]", 3, ps("aa;xy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)(  'a':'x' 2 | 1000 'a':'y'3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 2 'a':'y' 2)( 1000 'a':'x' 2 | 1000 'a':'y'3) ", ps("aa;xy"), "", "a", "b", " "),
                t("(1 'a':'x' 3 | 1 1 'a':'y' 2)( 1000 100 -100 'a':'x' 2 | 1000 'a':'y'3) ", ps("aa;xy"), "", "a", "b", " "),
                t("('a':'a'|'b':'b' | 'aaa':'3'1)*", ps("aa;aa", ";", "a;a", "aaa;3", "ab;ab", "abbba;abbba")),
                t("[a-z]:<0>", ps("a;", "b;", "c;", "d;", "z;"), "", "aa", "ab", "bb", "1", "\t"),
                t("'':<0> [a-z]", ps("a;a", "b;b", "c;c", "d;d", "z;z"), "", "aa", "ab", "bb", "1", "\t"),
                t("('':<0> [a-z])*", ps("abcdefghijklmnopqrstuvwxyz;abcdefghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";")),
                t("('':<0> [a-z] 10 -30) 10 1 -5 * 1 0 -23", ps("abcdefghijklmnopqrstuvwxyz;abcdefghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";")),
                t("([a-z]:<0>)*", ps("abcdefghijklmnopqrstuvwxyz;bcdefghijklmnopqrstuvwxyz", "a;", "b;", "c;", "d;", "z;", "aa;a", "zz;z", "rr;r", "ab;b", ";")),
                t("[a-z]*", ps("abcdefghijklmnopqrstuvwxyz;", "a;", "b;", "c;", "d;", "z;", "aa;", "zz;", "rr;", ";", "abc;", "jbebrgebcbrjbabcabcabc;")),
                tNG("('':'\\0' [a-z] | '':'3' 'abc' 1)*", ps("abcdefghijklmnopqrstuvwxyz;3defghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3", "jbebrgebcbrjbabcabcabcadfe;jbebrgebcbrjb333adfe")),
                t("('':'\\0' [a-z] | '':'3' 'abc' 1)*", ps("a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3")),
                tNG("('':<0> [a-z] | '':'3' 'abc' 1)*", ps("abcdefghijklmnopqrstuvwxyz;3defghijklmnopqrstuvwxyz", "a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3", "jbebrgebcbrjbabcabcabcadfe;jbebrgebcbrjb333adfe")),
                t("('':<0> [a-z] | '':'3' 'abc' 1)*", ps("a;a", "b;b", "c;c", "d;d", "z;z", "aa;aa", "zz;zz", "rr;rr", ";", "abc;3")),
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
                        ps("abc;", "aec;", "afc;", "abbc;", "aeec;", "affc;", "abec;", "aefc;", "afbc;"),
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
                a(" f='a' f<:'a'", ps("a;"), "b", "c", "", " "),
                a(" f='' f<:''->''", ps(";"), "a", "b", "c", "aa", " "),
                t("'a':'q\0v\0w' [k-o]", ps("ak;qkvkw",/*duplicate tells inversion to fail*/  "ak;qkvkw", "al;qlvlw", "am;qmvmw", "an;qnvnw", "ao;qovow"), "a", "b", "c", "aa", "aj", "ap", "aka", "", " "),
                a("f='a':'q\0v\0w' [k-o] f<:'a'[k-o]&&'q'[k-o]'v'[k-o]'w'", ps("ak;qkvkw",/*duplicate tells inversion to fail*/  "ak;qkvkw", "al;qlvlw", "am;qmvmw", "an;qnvnw", "ao;qovow"), "a", "b", "c", "aa", "aj", "ap", "aka", "", " "),
                a("f='a':'q\0v\0w' [k-o] f<:'a'[k-o]&&'q'('kvkw'|'lvlw'|'mvmw'|'nvnw'|'ovow')", ps("ak;qkvkw",/*duplicate tells inversion to fail*/  "ak;qkvkw", "al;qlvlw", "am;qmvmw", "an;qnvnw", "ao;qovow"), "a", "b", "c", "aa", "aj", "ap", "aka", "", " "),
                a("f='a':'\0\0' [a-b] f<:.*&&'aa'|'bb'", ps("aa;aa",/*duplicate tells inversion to fail*/  "aa;aa", "ab;bb"), "a", "b", "c", "aj", "ap", "aka", "", " "),
                a("f='':'\0q\0' [a-b] f<:.*&&[a-b]'q'[a-b]", ps("a;aqa",/*duplicate tells inversion to fail*/  "a;aqa", "b;bqb"), "aa", "ab", "c", "aaa", "aj", "ap", "aka", "`", "cc", "", " "),
                a("f='a':'\0q\0' [a-b] f<:.*&&[a-b]'q'[a-b]", ps("aa;aqa",/*duplicate tells inversion to fail*/  "aa;aqa", "ab;bqb"), "a", "b", "c", "aaa", "aj", "ap", "aka", "`", "cc", "", " "),
                a("f='':'\0\0' [a-b] f<:.*&&'aa'|'bb'", ps("a;aa",/*duplicate tells inversion to fail*/  "a;aa", "b;bb"), "aa", "bb", "c", "aaa", "aj", "ap", "aka", "`", "cc", "", " "),
                ex2("f='':'\0q' [a-b] f<:.*&&'q'[a-b]", CompilationError.TypecheckException.class),
                ex2("f='a':'\0q' [a-b] f<:.*&&'q'[a-b]", CompilationError.TypecheckException.class),
                a("f='a':'\0\0' [a-b] f<:.*&&'aa'|'bb'", ps("aa;aa",/*duplicate tells inversion to fail*/  "aa;aa", "ab;bb"), "a", "b", "c", "aaa", "aj", "ap", "aka", "`", "cc", "", " "),
                a("f='a':'\0\0' [a-b] f<:.*&&[a-b][a-b]", ps("aa;aa",/*duplicate tells inversion to fail*/  "aa;aa", "ab;bb"), "a", "b", "c", "aaa", "aj", "ap", "aka", "`", "cc", "", " "),
                ex2("f='a':'\0\0q' [a-b] f<:.*&&'q'[a-b][a-b]", CompilationError.TypecheckException.class),
                a("f='a':'\0\0q' [a-b] f<:.*&&[a-b][a-b]'q'", ps("aa;aaq",/*duplicate tells inversion to fail*/  "aa;aaq", "ab;bbq"), "a", "b", "c", "aaa", "aj", "ap", "aka", "`", "cc", "", " "),
                a("f='a':'q\0\0' [a-b] f<:.*&&'q'[a-b][a-b]", ps("aa;qaa",/*duplicate tells inversion to fail*/  "aa;qaa", "ab;qbb"), "a", "b", "c", "aaa", "aj", "ap", "aka", "`", "cc", "", " "),
                a("f='a':'q\0v\0w' [k-o] f<:'a'[k-o]&&'q'[k-o]'v'[k-o]'w'", ps("ak;qkvkw",/*duplicate tells inversion to fail*/  "ak;qkvkw", "al;qlvlw", "am;qmvmw", "an;qnvnw", "ao;qovow"), "a", "b", "c", "aa", "aj", "ap", "aka", "", " "),
                ex2("f='a' f<:''->'' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:'a'->'a' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:'b'->'' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:'aa'->'' ", CompilationError.TypecheckException.class),
                ex2("f='a' f<:''*->'' ", CompilationError.TypecheckException.class),
                ex2("f=missingFunc !('test', 'ter', 'otr')", CompilationError.UndefinedExternalFunc.class),
                ex2("f=missingFunc!('test':'', 'ter':'re', 'otr':'te')", CompilationError.UndefinedExternalFunc.class),
                ex2(" f='a'  f<:'a'*->''", CompilationError.TypecheckException.class),
                a("f='a'*   f<:'a'*->'' ", ps(";", "a;", "aa;", "aaaaa;"), "b", "c", " "),
                a("f='a'*  f<:'a'->'' ", ps(";", "a;", "aa;", "aaaaa;"), "b", "c", " "),
                a("f='a'*  f<:'a'* ", ps(";", "a;", "aa;", "aaaaa;"), "b", "c", " "),
                ex2("f='':''  f<:''|'e'|'r'->''|'' ", CompilationError.TypecheckException.class),
                ex2("f='':'a'  f<:. ", CompilationError.TypecheckException.class),
                a("f=(''|'e'|'r'):''  f<:''->''|'' ", ps(";", "e;", "r;"), "a", "b", "c", "aa", " "),
                a("f=(''|'e'|'r'):''  f<:''|[e-r] ", ps(";", "e;", "r;"), "a", "b", "c", "aa", " "),
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
                        "f<:[]->.* ", ps("aa;xy"), "", "a", "b", " "),
                ex2("!!A=[0-1] " +
                        "f='01':'10' " +
                        "f<:A*->A* ", CompilationError.TypecheckException.class),
                a("A=[0-1] " +
                        "f='01':'10' " +
                        "f<:'01'->A* ", ps("01;10"), "", "1", "0", "010"),
                a("A=[0-1] " +
                        "f='01':'10' " +
                        "f<:[]->A* ", ps("01;10"), "", "1", "0", "010"),
                ex2("A=[0-1] " +
                        "B=[0-9] " +
                        "f=[9-0]:'10' " +
                        "f<:B*->A*", CompilationError.TypecheckException.class),
                a("A=[0-1] " +
                        "B=[0-9] " +
                        "f=[9-0]:'10' " +
                        "f<:[0-9]->A*", ps("9;10", "4;10", "7;10", "0;10"), "", "10", "20", "65"),
                a("A=[0-1] " +
                        "B=[0-9] " +
                        "f=[9-0]:'10' " +
                        "f<:[0-4]->A*", ps("9;10", "4;10", "7;10", "0;10"), "", "10", "20", "65"),
                a("A=[0-1] " +
                        "B=[0-9] " +
                        "f=[9-0]:'10' " +
                        "f<:[3-4]->A*", ps("9;10", "4;10", "7;10", "0;10"), "", "10", "20", "65"),
                a("!!g='abc':'0'|'def':'1' f= g g g",
                        ps("abcdefabc;010", "abcabcabc;000", "defdefdef;111", "defabcdef;101"),
                        "", "abc", "def", "abcabc", "abcabcabcabc", "defdefdefdef"),
                a("!!g='aab' h= g g g g f='aabaabaabaab' f<:h->'' ",
                        ps("aabaabaabaab;"), "", "abc", "def", "aa", "aabaabaabaaba", "aabaabaabaa"),
                a("f=('':'\\0' .)*", ps(";", "a;a", "afeee;afeee",
                        "\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?;\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?")),
                a("f=('':<0> .)*", ps(";", "a;a", "afeee;afeee",
                        "\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?;\n1234567890-=qwertyuiop[]asdfghjkl'\\zxcvbnm,./ !@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:'|ZXCVBNM<>?")),
                a("f=[]", ps(), "", "a", "b", "aa", " ", "1", "0", "!", "'", ",", ";", ")", ".", "[]", "$", "\n"),
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
                ex2("f='':<0> [a-b]  f<:'a'&& Σ ", CompilationError.TypecheckException.class),
                ex2(" f='':<0> [a-b]  f<:'a'&&[a-b]", CompilationError.TypecheckException.class),
                ex2("f='':<0> [a-b]  f<:[a-z]&&[] ", CompilationError.TypecheckException.class),
                ex2("f='':<0> [a-b]  f<:[a-z]->[] ", CompilationError.TypecheckException.class),
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
                a("f='':<0> ([a-b] | [d-e])  f<:[]->([a-b]|[d-e]) ", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                ex2("f='':<0> ([a-b] | [d-e])  f<:.->. ", CompilationError.TypecheckException.class),
                a(" f='u'[a-b]'x' | 'v'[d-e]'y'  f<:'u'[a-b]'x'->''", ps("uax;", "ubx;", "vdy;", "vey;"), "`", "c", "f", "g"),
                a("f='':<0> ([a-b] | [d-e])  f<:[a-b]->. ", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:[a-b]->.", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> .  f<:.->.", ps("a;a", "b;b", "d;d", "e;e", "`;`", "c;c", "f;f", "g;g"), "aa"),
                ex2(" f='':<0> ([a-b] | [d-e])   f<:.->('a'|'b'|'d'|'e'|'')", CompilationError.TypecheckException.class),
                a(" f='':<0> ([a-b] | [d-e])  f<:.&&('a'|'b'|'d'|'e'|'')", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:[]->('a'|'b'|'d'|'e'|'')", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                ex2(" f='':<0> ([a-b] | [d-e])  f<:.->('a'|'b'|'d'|'e'|[])", CompilationError.TypecheckException.class),
                a(" f='':<0> ([a-b] | [d-e])  f<:.&&('a'|'b'|'d'|'e'|[])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> ([a-b] | [d-e])  f<:[]->('a'|'b'|'d'|'e'|[])", ps("a;a", "b;b", "d;d", "e;e"), "`", "c", "f", "g"),
                a(" f='':<0> [g-k] f<:[g-l] && [g-l]", ps("g;g", "h;h", "i;i", "j;j"), "`", "c", "f"),
                a(" f='':<0> [g-k] f<:[f-k] && [f-k]", ps("g;g", "h;h", "i;i", "j;j"), "`", "c", "f"),
                a(" f='':<0> [g-k] f<:[f-l] && [f-l]", ps("g;g", "h;h", "i;i", "j;j"), "`", "c", "f"),
                ex2(" f='':<0> [g-k] f<:[h-k] && [g-k]", CompilationError.TypecheckException.class),
                ex2(" f='':<0> [g-k] f<:[g-j] && [g-k]", CompilationError.TypecheckException.class),
                ex2(" f='':<0> [g-k] f<:[g-k] && [h-k]", CompilationError.TypecheckException.class),
                ex2(" f='':<0> [g-k] f<:[g-k] && [g-j]", CompilationError.TypecheckException.class),
                a(" f='a':'xyz'  f<:'a'->'xyz'", ps("a;xyz"), "`", "c", "f", "g"),
                a(" f='':'xyz'  f<:''->'xyz'", ps(";xyz"), "`", "c", "f", "g"),
                t("rpni!('a','aa':∅,'aaa','aaaa':∅)", ps("a;", "aaa;", "aaaaa;", "aaaaaaa;"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                t("rpni!('a','aa':[],'aaa','aaaa':[])", ps("a;", "aaa;", "aaaaa;", "aaaaaaa;"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                t("ostia!('a':'0','aa':'01','aaa':'010')", ps("a;0", "aa;01","aaa;010", "aaaaa;01010", "aaaaaaa;0101010"),  "`", "c", "f", "g"),
                t("rpniMealy!('a':'0','aaaaa':'01010')", ps("a;0", "aa;01","aaa;010", "aaaaa;01010", "aaaaaaa;0101010"),  "`", "c", "f", "g"),
                t("rpniMealy!('aaaaa':'01010')", ps("a;0", "aa;01","aaa;010", "aaaaa;01010", "aaaaaaa;0101010"),  "`", "c", "f", "g"),
                t("rpniMealy!('a':'0','aa':'01','aaa':'010','b':'1')", ps("b;1","a;0", "aa;01","aaa;010", "aaaaa;01010", "aaaaaaa;0101010"),  "`", "c", "f", "g"),
                t("rpniEdsm!('a','aa':[],'aaa','aaaa':[])", ps("a;", "aaa;", "aaaaa;", "aaaaaaa;"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                a(" f='xyz' | 'xy'  f<:'xy'(''|'z')&&''", ps("xy;", "xyz;"), "`", "c", "f", "g"),
                a(" f='xyz' | 'xy'  f<:'xy' 'z'?&&''", ps("xy;", "xyz;"), "`", "c", "f", "g"),
                a(" f='xyz' | 'xy'  f<:'xy'->''", ps("xy;", "xyz;"), "`", "c", "f", "g"),
                t("dict!('a':'be')", ps("a;be"), "", "aa", "efr", "etrry"),
                t("dict!('a':'be','aa':'ykfe','aaa','idw':'gerg','ferf':'fer','ded':'ret','ueh':'grge','efr':[],'etrry':[])",
                        ps("a;be", "aa;ykfe", "aaa;", "idw;gerg", "ferf;fer", "ded;ret", "ueh;grge"), "", "aaaa", "aaaaa", "efr", "etrry"),
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
                t("compose!['a':'b']!['b':'c']", ps("a;c"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "g"),
                t("compose!['':<0> [g-k]]!['':<0> [g-k]]", ps("g;g", "h;h", "i;i", "j;j", "k;k"), "l", "f", "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "l"),
                t("compose!['':<0> [g-k]]!['':<0> [h-k]]", ps("h;h", "i;i", "j;j", "k;k"), "g", "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "l"),
                t("compose!['':<0> [g-k]]!['':<0> [g-j]]", ps("g;g", "h;h", "i;i", "j;j"), "k", "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "l"),
                t("compose!['':<0> [g-j]]!['':<0> [g-k]]", ps("g;g", "h;h", "i;i", "j;j"), "k", "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "l"),
                t("compose!['':<0> [h-k]]!['':<0> [g-k]]", ps("h;h", "i;i", "j;j", "k;k"), "g", "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "l"),
                t("compose!['a':'b'|'c':'d']!['b':'c'|'d':'e']", ps("a;c", "c;e"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "f", "g"),
                t("compose!['aaa':'b'|'c':'d']!['b':'c'|'d':'e']", ps("aaa;c", "c;e"), "", "a", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "f", "g"),
                t("compose!['']![ '']", ps(";"), "a", "aaaa", "aaaaa", "aaaaaaaa", "b", "ab", "aac", "aaaac", "aacaaaa", "aaaacaaaa", "`", "c", "f", "g"),
                t("compose![('a':'b')*]![ ('b':'c')*]", ps("aaa;ccc", "a;c", ";", "aaaaa;ccccc"), "b", "ab", "aac", "aaaac", "aacaaaa", "aaaacaaaa", "`", "c", "f", "g"),
                t("compose!['a':'1'|'aa':'2'|'aaa':'3'|'ab':'4'|'aab':'5'|'b':'6'|'':'7']!['1':'x'|'2':'xx'|'3':'xxx'|'4':'xxxx'|'5':'xxxxx'|'6':'xxxxxx'|'7':'xxxxxxx']",
                        ps("a;x", "aa;xx", "aaa;xxx", "ab;xxxx", "aab;xxxxx", "b;xxxxxx", ";xxxxxxx"), "ba", "aba", "aaaa", "aaaaaa", "aaaaaaaa", "`", "f", "g"),
                t("compose!['':<0> [h-k]]![[]]", ps(), "k","j","i","h","g", "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "l"),
                t("compose!['':<0> [h-k]]![[]]", ps(), "k","j","i","h","g", "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "c", "f", "l"),
                a("t='a':'1'|'aa':'2'|'aaa':'3'|'ab':'4'|'aab':'5'|'b':'6'|'':'7' " +
                                "h='1':'x'|'2':'xx'|'3':'xxx'|'4':'xxxx'|'5':'xxxxx'|'6':'xxxxxx'|'7':'xxxxxxx' " +
                                "f=compose![t]![h]",
                        ps("a;x", "aa;xx", "aaa;xxx", "ab;xxxx", "aab;xxxxx", "b;xxxxxx", ";xxxxxxx"), "ba", "aba", "aaaa", "aaaaaa", "aaaaaaaa", "`", "f", "g"),
                a("!!g = ('aa':'a')* " +
                        "f = g", ps(";", "aa;a", "aaaa;aa", "aaaaaa;aaa", "aaaaaaaa;aaaa"), "a", "aaa", "aaaaa", "aaaaaaa", "`", "c", "f", "g"),
                a("!!g = ('aa':'a')* " +
                        "f = compose![g]![g]", ps(";", "aaaa;a", "aaaaaaaa;aa", "aaaaaaaaaaaa;aaa"), "a", "aa", "aaa", "aaaaa", "aaaaaa", "aaaaaaa", "aaaaaaaaa", "`", "c", "f", "g"),
                a("!!g = ('aa':'a')* " +
                        "f = compose![g]![g]![g]", ps(";", "aaaaaaaa;a", "aaaaaaaaaaaaaaaa;aa"), "a", "aa", "aaa", "aaaa", "aaaaa", "aaaaaa", "aaaaaaa", "aaaaaaaaa", "`", "c", "f", "g"),
                t("inverse!['a':'b'|'b':'c'|'c':'d'|'d':'a']", ps("a;d", "b;a", "c;b", "d;c"), "", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a']!['a']", ps(), "", "a", "b", "c", "d", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['aa']!['a']", ps("aa;"), "", "a", "b", "c", "d", "aaa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']!['a']", ps("b;1", "c;2", "d;3"), "a", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']![[]]", ps("a;0", "b;1", "c;2", "d;3"), "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']![.*]", ps(), "a", "b", "c", "d", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']!['b']", ps("a;0", "c;2", "d;3"), "b", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']!['c']", ps("a;0", "b;1", "d;3"), "c", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']!['d']", ps("a;0", "b;1", "c;2"), "d", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']!['a'|'b']", ps("c;2", "d;3"), "a", "b", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtractNondet!['a':'0'|'b':'1'|'c':'2'|'d':'3']!['a'|'a']", ps("b;1", "c;2", "d;3"), "a", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract!['a':'0'|'b':'1'|'c':'2'|'d':'3']!['a'*]", ps("b;1", "c;2", "d;3"), "a", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                t("subtract![[a-z]:'0'|[a-k]'b':'1'|[a-f]'c':'2'|[a-e]'d':'3']!['a'*'b'?]", ps("c;0", "d;0", "e;0", "z;0", "bb;1", "kb;1", "ac;2", "fc;2", "ad;3", "ed;3"), "b", "ab", "a", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`"),
                t("subtract![('a':'0'|'b':'1'|'c':'2'|'d':'3')*]!['a'*]", ps("b;1", "c;2", "d;3", "aab;001", "bcdaa;12300"), "a", "aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                tNG("identity![('a':'0'|'b':'1'|'c':'2'|'d':'3')*]", ps(";", "b;b", "c;c", "d;d", "aab;aab", "bcdaa;bcdaa", "a;a", "aa;aa", "aaaa;aaaa", "aaaaaa;aaaaaa", "aaaaaaaa;aaaaaaaa"), "`", "e", "f", "g"),
                tNG("clearOutput![('a':'0'|'b':'1'|'c':'2'|'d':'3')*]", ps(";", "b;", "c;", "d;", "aab;", "bcdaa;", "a;", "aa;", "aaaa;", "aaaaaa;", "aaaaaaaa;"), "`", "e", "f", "g"),
                tNG("identity!['a':'0'|'b':'1'|'c':'2'|'d':'3']", ps( "b;b", "c;c", "d;d", "a;a"),"aab", "bcdaa","aa", "aaaa", "aaaaaa", "aaaaaaaa", "`", "e", "f", "g"),
                tNG("clearOutput!['a':'0'|'b':'1'|'c':'2'|'d':'3']", ps("b;", "c;", "d;", "a;"), "aab", "bcdaa", "aa", "aaaa", "aaaaaa", "aaaaaaaa","`", "e", "f", "g"),
                exAfterMinNG("ws = ' '|'\\t'|'\\n' " +
                        "fourToNine = 'four':'4' 3|'five':'5' 5|'six':'6' 6|'seven':'7' 7|'eight':'8' 8|'nine':'9' 9\n" +
                        "firstTen = 'one':'1' 1 |'two':'2' 2 |'three':'3' 3| !!fourToNine \n" +
                        "tens= 'ten':'10' 100|'twenty':'20' 11|'thirty':'30' 12|'fourty':'40' 13|'fifty':'50' 14|'sixty':'60' 15|'seventy':'70' 16|'eighty':'80' 17|'ninety':'90' 18\n" +
                        "twoDigits = !!firstTen 1 | tens ws+:' ' firstTen 2 \n" +
                        "f = (:<0> .  | twoDigits 2)* \n", ps("one;1","two;2","one1;11","oneone;11","11;11","twot;2t","nine;9","four;4","twenty nine;20 9","thirty four;30 4","twenty one;20 1","ten1;ten1","1010;1010","ten two;10 2","ninety eight;90 8","fourty six;40 6","ten ten;ten ten", "k;k","j;j","i;i","h;h","g;g", ";", "aa;aa", "aaaa;aaaa", "aaaaaa;aaaaaa", "aaaaaaaa;aaaaaaaa", "`;`", "c;c", "f;f", "l;l"),CompilationError.WeightConflictingToThirdState.class),
                aNG("ws = ' '|'\\t'|'\\n' " +
                        "fourToNine = 'four':'4'|'five':'5'|'six':'6'|'seven':'7'|'eight':'8'|'nine':'9' \n" +
                        "firstTen = 'one':'1'|'two':'2'|'three':'3'| !!fourToNine \n" +
                        "f = (:<0> .  | firstTen 2)* \n", ps("one;1","two;2","one1;11","oneone;11","twot;2t","nine;9","four;4","k;k","j;j","i;i","h;h","g;g", ";", "aa;aa", "aaaa;aaaa", "aaaaaa;aaaaaa", "aaaaaaaa;aaaaaaaa", "`;`", "c;c", "f;f", "l;l")),
                aNG("ws = ' '|'\\t'|'\\n' " +
                        "tens= 'ten':'10'|'twenty':'20'|'thirty':'30'|'fourty':'40'|'fifty':'50'|'sixty':'60'|'seventy':'70'|'eighty':'80'|'ninety':'90' \n" +
                        "f = (:<0> .  | tens 2)* \n", ps("ten;10","twenty;20","ten1;101","1010;1010","tenten;1010","ninety;90","fourty;40","k;k","j;j","i;i","h;h","g;g", ";", "aa;aa", "aaaa;aaaa", "aaaaaa;aaaaaa", "aaaaaaaa;aaaaaaaa", "`;`", "c;c", "f;f", "l;l")),
                aNG("ws = ' '|'\\t'|'\\n' " +
                        "fourToNine = 'four':'4'|'five':'5'|'six':'6'|'seven':'7'|'eight':'8'|'nine':'9' \n" +
                        "firstTen = 'one':'1'|'two':'2'|'three':'3'| !!fourToNine \n" +
                        "tens= 'ten':'10'|'twenty':'20'|'thirty':'30'|'fourty':'40'|'fifty':'50'|'sixty':'60'|'seventy':'70'|'eighty':'80'|'ninety':'90' \n" +
                        "f = (:<0> .  | tens 2 | firstTen 3)* \n", ps("one;1","two;2","one1;11","oneone;11","twot;2t","nine;9","four;4","twenty nine;20 9","thirty four;30 4","twenty;20","ten1;101","ten;10","1010;1010","tenten;1010","ninety;90","fourty;40","k;k","j;j","i;i","h;h","g;g", ";", "aa;aa", "aaaa;aaaa", "aaaaaa;aaaaaa", "aaaaaaaa;aaaaaaaa", "`;`", "c;c", "f;f", "l;l")),
                aNG("ws = ' '|'\\t'|'\\n' " +
                        "fourToNine = 'four':'4' 3|'five':'5' 5|'six':'6' 6|'seven':'7' 7|'eight':'8' 8|'nine':'9' 9\n" +
                        "firstTen = 'one':'1' 1 |'two':'2' 2 |'three':'3' 3| !!fourToNine \n" +
                        "tens= 'ten':'10' 100|'twenty':'20' 11|'thirty':'30' 12|'fourty':'40' 13|'fifty':'50' 14|'sixty':'60' 15|'seventy':'70' 16|'eighty':'80' 17|'ninety':'90' 18\n" +
                        "twoDigits = !!firstTen 1 | tens ws+:' ' 2 firstTen 2 \n" +
                        "f = (:<0> .  | twoDigits 2)* \n", ps("one;1","two;2","one1;11","oneone;11","11;11","twot;2t","nine;9","four;4","twenty nine;20 9","thirty four;30 4","twenty one;20 1","ten1;ten1","1010;1010","ten two;10 2","ninety eight;90 8","fourty six;40 6","ten ten;ten ten", "k;k","j;j","i;i","h;h","g;g", ";", "aa;aa", "aaaa;aaaa", "aaaaaa;aaaaaa", "aaaaaaaa;aaaaaaaa", "`;`", "c;c", "f;f", "l;l")),
                aNG("ws = ' '|'\\t'|'\\n' " +
                        "fourToNine = 'four':'4'|'five':'5'|'six':'6'|'seven':'7'|'eight':'8'|'nine':'9' \n" +
                        "firstTen = 'one':'1'|'two':'2'|'three':'3'| !!fourToNine \n" +
                        "tens= 'ten':'10'|'twenty':'20'|'thirty':'30'|'fourty':'40'|'fifty':'50'|'sixty':'60'|'seventy':'70'|'eighty':'80'|'ninety':'90' \n" +
                        "twoDigits = !!firstTen | tens ws+:' ' 2 firstTen 1\n" +
                        "f = (:<0> .  | twoDigits 2)* \n", ps("one;1","two;2","one1;11","oneone;11","11;11","twot;2t","nine;9","four;4","twenty nine;20 9","thirty four;30 4","ten one;10 1","twenty one;20 1","ten1;ten1","1010;1010","tenten;tenten","ninety;9ty","fourty;4ty", "k;k","j;j","i;i","h;h","g;g", ";", "aa;aa", "aaaa;aaaa", "aaaaaa;aaaaaa", "aaaaaaaa;aaaaaaaa", "`;`", "c;c", "f;f", "l;l")),
                aNG("f = ostia!('1':'one','2':'two','3':'three','4':'four','5':'five','6':'six','7':'seven','8':'eight','9':'nine','10':'ten','11':'eleven','12':'twelve','13':'thirteen','14':'fourteen','15':'fifteen','16':'sixteen','17':'seventeen','18':'eighteen','19':'nineteen','20':'twenty','21':'twenty one','22':'twenty two','23':'twenty three','24':'twenty four','25':'twenty five','26':'twenty six','27':'twenty seven','28':'twenty eight','29':'twenty nine','30':'thirty','31':'thirty one','32':'thirty two','33':'thirty three','34':'thirty four','35':'thirty five','36':'thirty six','37':'thirty seven','38':'thirty eight','39':'thirty nine','40':'forty','41':'forty one','42':'forty two','43':'forty three','44':'forty four','45':'forty five','46':'forty six','47':'forty seven','48':'forty eight','49':'forty nine','50':'fifty','51':'fifty one','52':'fifty two','53':'fifty three','54':'fifty four','55':'fifty five','56':'fifty six','57':'fifty seven','58':'fifty eight','59':'fifty nine','60':'sixty','61':'sixty one','62':'sixty two','63':'sixty three','64':'sixty four','65':'sixty five','66':'sixty six','67':'sixty seven','68':'sixty eight','69':'sixty nine','70':'seventy','71':'seventy one','72':'seventy two','73':'seventy three','74':'seventy four','75':'seventy five','76':'seventy six','77':'seventy seven','78':'seventy eight','79':'seventy nine','80':'eighty','81':'eighty one','82':'eighty two','83':'eighty three','84':'eighty four','85':'eighty five','86':'eighty six','87':'eighty seven','88':'eighty eight','89':'eighty nine','90':'ninety','91':'ninety one','92':'ninety two','93':'ninety three','94':'ninety four','95':'ninety five','96':'ninety six','97':'ninety seven','98':'ninety eight','99':'ninety nine','100':'one hundred')",
                        ps("1;one","2;two","3;three","4;four","5;five","6;six","7;seven","8;eight","9;nine","10;ten","11;eleven","12;twelve","13;thirteen","14;fourteen","15;fifteen","16;sixteen","17;seventeen","18;eighteen","19;nineteen","20;twenty","21;twenty one","22;twenty two","23;twenty three","24;twenty four","25;twenty five","26;twenty six","27;twenty seven","28;twenty eight","29;twenty nine","30;thirty","31;thirty one","32;thirty two","33;thirty three","34;thirty four","35;thirty five","36;thirty six","37;thirty seven","38;thirty eight","39;thirty nine","40;forty","41;forty one","42;forty two","43;forty three","44;forty four","45;forty five","46;forty six","47;forty seven","48;forty eight","49;forty nine","50;fifty","51;fifty one","52;fifty two","53;fifty three","54;fifty four","55;fifty five","56;fifty six","57;fifty seven","58;fifty eight","59;fifty nine","60;sixty","61;sixty one","62;sixty two","63;sixty three","64;sixty four","65;sixty five","66;sixty six","67;sixty seven","68;sixty eight","69;sixty nine","70;seventy","71;seventy one","72;seventy two","73;seventy three","74;seventy four","75;seventy five","76;seventy six","77;seventy seven","78;seventy eight","79;seventy nine","80;eighty","81;eighty one","82;eighty two","83;eighty three","84;eighty four","85;eighty five","86;eighty six","87;eighty seven","88;eighty eight","89;eighty nine","90;ninety","91;ninety one","92;ninety two","93;ninety three","94;ninety four","95;ninety five","96;ninety six","97;ninety seven","98;ninety eight","99;ninety nine","100;one hundred")),
                a("f = ostiaMaxDeepOverlap!('one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22','twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100','one hundred one':'101','two hundred':'200','two hundred eighty three':'283','three hundred':'300','three hundred fifty one':'351','four hundred':'400','four hundred seventeen':'417','five hundred':'500','five hundred seventy seven':'577','six hundred':'600','six hundred forty five':'645','seven hundred':'700','seven hundred sixteen':'716','eight hundred':'800','eight hundred one':'801','eight hundred forty six':'846','eight hundred fifteen':'815','nine hundred':'900','nine hundred seventy four':'974','nine hundred seventy three':'973','nine hundred seventy two':'972','nine hundred seventy nine':'979','nine hundred ninety nine':'999','nine hundred ninety four':'994')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100","one hundred one;101","two hundred;200","three hundred fifty one;351","four hundred seventeen;417","five hundred seventy seven;577","six hundred forty five;645","seven hundred sixteen;716","eight hundred one;801","eight hundred forty;840","eight hundred forty one;841","eight hundred forty two;842","nine hundred seventy two;972","nine hundred seventy three;973","nine hundred seventy four;974","nine hundred seventy seven;977","nine hundred ninety four;994","nine hundred ninety five;995","nine hundred ninety nine;999")),
                a("f = ostiaMaxDeepOverlap!('a':'a','b':'b','c':'c','xa':'a','xb':'b','yb':'b','yc':'c')",
                        ps("a;a","b;b","c;c","xa;a","xb;b","xc;c","ya;a","yb;b","yc;c")),
                a("f = ostiaMaxCompatibleInputs!('a':'a','b':'b','c':'c','xa':'a','xb':'b','yb':'b','yc':'c')",
                        ps("a;a","b;b","c;c","xa;a","xb;b","xc;c","ya;a","yb;b","yc;c")),
                a("f = ostiaMaxCompatibleInputs!('aaa':'bbb')",
                        ps("a;","aaa;bbb"),"aa"),
                a("f = ostiaMaxCompatible!('a':'a','b':'b','c':'c','xa':'a','xb':'b','yb':'b','yc':'c')",
                        ps("a;a","b;b","c;c","xa;a","xb;b","xc;c","ya;a","yb;b","yc;c")),
                a("f = ostiaMaxCompatible!('aaa':'bbb')",
                        ps("a;","aaa;bbb"),"aa"),
                a("f = ostiaMaxOverlap!('aaa':'bbb')",
                        ps("a;","aaa;bbb"),"aa"),
                a("f = ostiaMaxOverlap!('aa':'bb','a':'b','aaa':[],'aaaa':'bbbb')",
                        ps("a;b","aa;bb","aaaa;bbbb"),"aaa","aaaaaa"),
                a("f = ostiaMaxOverlap!('aa':'bb','a':'b','aaa':[],'aaaa':'bbbb','aaaaa':[],'aaaaaa':[])",
                        ps("a;b","aa;bb","aaaa;bbbb"),"aaa","aaaaaa","aaaaa"),
                a("f = ostiaMaxOverlap!('one':'1','two':'2','three':'3','four':'4','twenty':'20','twenty one':'21','twenty two':'22')",
                        ps("one;1","one;1","two;2","three;3","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24")),
                a("f = ostiaMaxOverlap!('aa':'1','ab':'2','ac':'3','ad':'4','ae':'5','ba':'21','bb':'22')",
                        ps("aa;1","aa;1","ab;2","ac;3","ad;4","ba;21","bb;22","bc;23","bd;24","be;25")),
                a("f = ostiaMaxOverlap!('one':'1','two':'2','three':'3')",
                        ps("one;1","one;1","two;2","three;3")),
                a("f = ostiaMaxOverlap!('aa':'bb')",
                        ps("aa;bb","aaaa;bbbb","aaaaaa;bbbbbb")),
                a("f = ostiaMaxOverlap!('a':'b','aa':'bb')",
                        ps("a;b","aa;bb","aaa;bbb")),

                a("f = ostiaMaxOverlap!('one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22','twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100")),
                a("f = ostiaMaxOverlap!('one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22','twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100','one hundred one':'101','two hundred':'200','two hundred eighty three':'283','three hundred':'300','three hundred fifty one':'351','four hundred':'400','four hundred seventeen':'417','five hundred':'500','five hundred seventy seven':'577','six hundred':'600','six hundred forty five':'645','seven hundred':'700','seven hundred sixteen':'716','eight hundred':'800','eight hundred one':'801','eight hundred forty six':'846','eight hundred fifteen':'815','nine hundred':'900','nine hundred seventy four':'974','nine hundred seventy three':'973','nine hundred seventy two':'972','nine hundred seventy nine':'979','nine hundred ninety nine':'999','nine hundred ninety four':'994')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100","one hundred one;101","two hundred;200","three hundred fifty one;351","four hundred seventeen;417","five hundred seventy seven;577","six hundred forty five;645","seven hundred sixteen;716","eight hundred one;801","eight hundred forty;840","eight hundred forty one;841","eight hundred forty two;842","nine hundred seventy two;972","nine hundred seventy three;973","nine hundred seventy four;974","nine hundred seventy seven;977","nine hundred ninety four;994","nine hundred ninety five;995","nine hundred ninety nine;999")),
                a("f = ostiaMaxCompatible!('one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22','twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100','one hundred one':'101','two hundred':'200','two hundred eighty three':'283','three hundred':'300','three hundred fifty one':'351','four hundred':'400','four hundred seventeen':'417','five hundred':'500','five hundred seventy seven':'577','six hundred':'600','six hundred forty five':'645','seven hundred':'700','seven hundred sixteen':'716','eight hundred':'800','eight hundred one':'801','eight hundred forty six':'846','eight hundred fifteen':'815','nine hundred':'900','nine hundred seventy four':'974','nine hundred seventy three':'973','nine hundred seventy two':'972','nine hundred seventy nine':'979','nine hundred ninety nine':'999','nine hundred ninety four':'994')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100","one hundred one;101","two hundred;200","three hundred fifty one;351","four hundred seventeen;417","five hundred seventy seven;577","six hundred forty five;645","seven hundred sixteen;716","eight hundred one;801","eight hundred forty;840","eight hundred forty one;841","eight hundred forty two;842","nine hundred seventy two;972","nine hundred seventy three;973","nine hundred seventy four;974","nine hundred seventy seven;977","nine hundred ninety four;994","nine hundred ninety five;995","nine hundred ninety nine;999")),
                a("f = activeLearningFromDataset!('algorithm':'ostia_max_overlap','firstBatchCount':'20','batchSize':'5')!('one':'1','two':'2','three':'3','four':'4','five':'5','six':'6','seven':'7','eight':'8','nine':'9','ten':'10','eleven':'11','twelve':'12','thirteen':'13','fourteen':'14','fifteen':'15','sixteen':'16','seventeen':'17','eighteen':'18','nineteen':'19','twenty':'20','twenty one':'21','twenty two':'22', 'twenty three':'23','twenty four':'24','twenty five':'25','twenty six':'26','twenty seven':'27','twenty eight':'28','twenty nine':'29','thirty':'30','thirty one':'31','thirty two':'32','thirty three':'33','thirty four':'34','thirty five':'35','thirty six':'36','thirty seven':'37','thirty eight':'38','thirty nine':'39','forty':'40','forty one':'41','forty two':'42','forty three':'43','forty four':'44','forty five':'45','forty six':'46','forty seven':'47','forty eight':'48','forty nine':'49','fifty':'50','fifty one':'51','fifty two':'52','fifty three':'53','fifty four':'54','fifty five':'55','fifty six':'56','fifty seven':'57','fifty eight':'58','fifty nine':'59','sixty':'60','sixty one':'61','sixty two':'62','sixty three':'63','sixty four':'64','sixty five':'65','sixty six':'66','sixty seven':'67','sixty eight':'68','sixty nine':'69','seventy':'70','seventy one':'71','seventy two':'72','seventy three':'73','seventy four':'74','seventy five':'75','seventy six':'76','seventy seven':'77','seventy eight':'78','seventy nine':'79','eighty':'80','eighty one':'81','eighty two':'82','eighty three':'83','eighty four':'84','eighty five':'85','eighty six':'86','eighty seven':'87','eighty eight':'88','eighty nine':'89','ninety':'90','ninety one':'91','ninety two':'92','ninety three':'93','ninety four':'94','ninety five':'95','ninety six':'96','ninety seven':'97','ninety eight':'98','ninety nine':'99','one hundred':'100','one hundred one':'101','two hundred':'200','two hundred eighty three':'283','three hundred':'300','three hundred fifty one':'351','four hundred':'400','four hundred seventeen':'417','five hundred':'500','five hundred seventy seven':'577','six hundred':'600','six hundred forty five':'645','seven hundred':'700','seven hundred sixteen':'716','eight hundred':'800','eight hundred one':'801','eight hundred forty six':'846','eight hundred fifteen':'815','nine hundred':'900','nine hundred seventy four':'974','nine hundred seventy three':'973','nine hundred seventy two':'972','nine hundred seventy nine':'979','nine hundred ninety nine':'999','nine hundred ninety four':'994')",
                        ps("one;1","one;1","two;2","three;3","four;4","five;5","six;6","seven;7","eight;8","nine;9","ten;10","eleven;11","twelve;12","thirteen;13","fourteen;14","fifteen;15","sixteen;16","seventeen;17","eighteen;18","nineteen;19","twenty;20","twenty one;21","twenty two;22","twenty three;23","twenty four;24","twenty five;25","twenty six;26","twenty seven;27","twenty eight;28","twenty nine;29","thirty;30","thirty one;31","thirty two;32","thirty three;33","thirty four;34","thirty five;35","thirty six;36","thirty seven;37","thirty eight;38","thirty nine;39","forty;40","forty one;41","forty two;42","forty three;43","forty four;44","forty five;45","forty six;46","forty seven;47","forty eight;48","forty nine;49","fifty;50","fifty one;51","fifty two;52","fifty three;53","fifty four;54","fifty five;55","fifty six;56","fifty seven;57","fifty eight;58","fifty nine;59","sixty;60","sixty one;61","sixty two;62","sixty three;63","sixty four;64","sixty five;65","sixty six;66","sixty seven;67","sixty eight;68","sixty nine;69","seventy;70","seventy one;71","seventy two;72","seventy three;73","seventy four;74","seventy five;75","seventy six;76","seventy seven;77","seventy eight;78","seventy nine;79","eighty;80","eighty one;81","eighty two;82","eighty three;83","eighty four;84","eighty five;85","eighty six;86","eighty seven;87","eighty eight;88","eighty nine;89","ninety;90","ninety one;91","ninety two;92","ninety three;93","ninety four;94","ninety five;95","ninety six;96","ninety seven;97","ninety eight;98","ninety nine;99","one hundred;100","one hundred one;101","two hundred;200","three hundred fifty one;351","four hundred seventeen;417","five hundred seventy seven;577","six hundred forty five;645","seven hundred sixteen;716","eight hundred one;801","eight hundred forty;840","eight hundred forty one;841","eight hundred forty two;842","nine hundred seventy two;972","nine hundred seventy three;973","nine hundred seventy four;974","nine hundred seventy seven;977","nine hundred ninety four;994","nine hundred ninety five;995","nine hundred ninety nine;999")),


        };

        int i = 0;
        for (TestCase testCase : testCases) {
            String input = null;
            try {
                begin(testCase, i);
                Solomonoff<N,G> tr = cons.cons(Config.config());
                tr.parse(CharStreams.fromString(testCase.regex));
                Var<N, G> g = tr.getTransducer("f");
                Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> o = tr.getOptimisedTransducer("f");
                assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.exception, null);
                if (testCase.numStates > -1) {
                    assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, testCase.numStates, o.graph.size());
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
                phase("powerset ");
                final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> dfa = tr.specs.powerset(o);
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
                for (Positive pos : testCase.positive) {
                    if (!outputs.add(pos.output)) {
                        shouldInversionFail = true;
                        break;
                    }
                }
                try {
                    phase("inverse ");
                    tr.specs.inverse(g.graph);
                    o = tr.specs.optimiseGraph(g.graph);
                    if(!shouldInversionFail){
                        for (Positive pos : testCase.positive) {
                            input = pos.output;
                            final String out = tr.specs.evaluate(o, input);
                            final String exp = pos.input;
                            assertEquals("INV idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\ninput=" + input, exp, out);
                        }
                    }
                    tr.specs.checkFunctionality(o,g.pos);


                    assertEquals("INV idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o, shouldInversionFail, false);

                } catch (CompilationError e) {
                    if (!shouldInversionFail) e.printStackTrace();
                    assertEquals("INV idx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n" + e, shouldInversionFail,true );

                }

            } catch (Throwable e) {
                if (e instanceof ComparisonFailure) throw e;
                if (testCase.exception != null) {
                    if (!testCase.exception.equals(e.getClass())) {
                        e.printStackTrace();
                    }
                    assertEquals("idx=" + i + "\nregex=" + testCase.regex + "\n", testCase.exception, e.getClass());
                } else {
                    throw new Exception(i + " {" + testCase.regex + "}'" + input + "';" + e.getClass() + " " + e.getMessage(), e);
                }
            }
            try {
                phase("binary ");
                Solomonoff<N,G> tr = cons.cons(Config.config());
                tr.parse(CharStreams.fromString(testCase.regex));
                Var<N, G> g = tr.getTransducer("f");
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                tr.specs.compressBinary(g.graph, new DataOutputStream(s));
                final G decompressed = tr.specs.decompressBinary(Pos.NONE, new DataInputStream(new ByteArrayInputStream(s.toByteArray())));
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
                phase("binary_powerset ");
                final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> dfa = tr.specs.powerset(o);
                for (Positive pos : testCase.positive) {
                    assertTrue("BIN DFA\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + pos.input,
                            tr.specs.accepts(dfa, pos.input.codePoints().iterator()));
                }
                for (String neg : testCase.negative) {
                    assertFalse("BIN DFA\nidx=" + i + "\nregex=" + testCase.regex + "\n" + g + "\n\n" + o + "\n\n" + dfa + "\ninput=" + input,
                            tr.specs.accepts(dfa, neg.codePoints().iterator()));
                }

            } catch (Throwable e) {
                if (e instanceof ComparisonFailure) throw e;
                if (testCase.exception != null) {
                    if (!testCase.exception.equals(e.getClass())) {
                        e.printStackTrace();
                    }
                    assertEquals("BIN\nidx=" + i + "\nregex=" + testCase.regex + "\n", testCase.exception, e.getClass());
                } else {
                    throw new Exception(i + "BIN{" + testCase.regex + "}'" + input + "';" + e.getClass() + " " + e.getMessage(), e);
                }
            }
            i++;
            end();
        }

    }

    private void begin(TestCase c, int idx) {
        System.out.println(idx + " Testing: " + c.regex);
    }

    private void end() {
//        System.out.println("OK!");
    }

    private void phase(String str) {
//        System.out.print(str);
    }

    static class PipelineTestCase {
        final String code;
        final Positive[] ps;
        final String[] negative;
        final Class<?> shouldFail;

        PipelineTestCase(String code, Positive[] ps, String[] negative,Class<?> shouldFail) {
            this.code = code;
            this.ps = ps;
            this.negative = negative;
			this.shouldFail = shouldFail;
        }
    }

    static PipelineTestCase p(String code, Positive[] ps, String... negative) {
        return new PipelineTestCase(code, ps, negative,null);
    }
    static PipelineTestCase pex(String code,Class<?> exception) {
        return new PipelineTestCase(code, null, null,exception);
    }

    @Test
    void testOSTIA() throws Exception {
        final Random rnd = new Random(8);//8
        final int testCount = 50;
        final int alphSize = 2;
        final int minSymbol = 'a' - 1;
        final int maxSymbol = minSymbol + alphSize;
        final IntEmbedding alph = new IntEmbedding('a','b');
        final ArrayList<Integer> FULL_SIGMA = new ArrayList<>();
        for(int i=minSymbol+1;i<=maxSymbol;i++){
            FULL_SIGMA.add(i);
        }
        HashMapBacked tr = new HashMapBacked(Config.config(minSymbol,maxSymbol, maxSymbol));
        for (int i = 1; i < testCount; i++) {
            System.out.println("Random test on " + i + " states");
            final int maxStates = i;
            final double partialityFact = rnd.nextDouble();
            final HashMapIntermediateGraph<Pos, E, P> rand =
                    tr.specs.randomDeterministic(maxStates, rnd.nextInt(20), 0,
                            () -> FULL_SIGMA,
                            (fromExclusive, toInclusive) -> new E(fromExclusive, toInclusive, IntSeq.rand(0, 4, 'a', 'a' + alphSize,rnd), 0),
                            () -> Pair.of(new P(IntSeq.rand(0, 4, 'a', 'a' + alphSize,rnd), 0), Pos.NONE),rnd);
            final Specification.RangedGraph<Pos, Integer, E, P> optimal = tr.specs.optimiseGraph(rand);
            assert optimal.isDeterministic() == null;
            assert optimal.size() <= maxStates + 1 : optimal.size() + " <= " + (maxStates + 1) + "\n" + optimal + "\n===\n" + rand;
            final ArrayList<LexUnicodeSpecification.BacktrackingHead> sample = tr.specs.generateSmallOstiaCharacteristicSample(optimal);
            final ArrayList<Pair<IntSeq,IntSeq>> informant = new ArrayList<>(sample.size());

            for(LexUnicodeSpecification.BacktrackingHead trace:sample){
                final IntSeq in = trace.randMatchingInput(rnd);
                final IntSeq out = tr.specs.collect(trace,in);
                informant.add(Pair.of(in, out));
            }
            Collections.shuffle(informant);
            final OSTIA.State init = OSTIA.buildPtt(alph, informant.iterator());
            for(Pair<IntSeq,IntSeq> pair:informant){
                final @Nullable IntSeq out = OSTIAState.run(init,alph,pair.l());
                assertNotNull(pair.toString(),out);
                assertArrayEquals(optimal+"\n\n\n"+informant+"\n\n"+init+"\n\n\nPAIR="+pair+"\nGOT="+out,pair.r().unsafe(),out.stream().mapToInt(k->k).toArray());
            }
            final long beginTiming = System.currentTimeMillis();
            OSTIA.ostia(init);
            final long ostiaTime = System.currentTimeMillis()-beginTiming;
            System.out.println("Took "+ostiaTime+" milis");
            for(Pair<IntSeq,IntSeq> pair:informant){
                final @Nullable IntSeq out = OSTIAState.run(init,alph,pair.l());
                assertNotNull(pair.toString(),out);
                assertArrayEquals("PAIR="+pair+"GOT="+out+"\n\n\nINFORMANT="+informant+"\n\nGENERATED="+optimal+"\n\n\nLEARNED="+init,pair.r().unsafe(),out.stream().mapToInt(k->k).toArray());
            }
            final Specification.RangedGraph<IntSeq, Integer, E, P> learned = tr.specs.convertCustomGraphToRanged(OSTIAState.asGraph(tr.specs,init, idx->idx+'a', OSTIA.State::shortest), E::getToExclsuive);
            for(Pair<IntSeq, IntSeq> pair:informant){
                final IntSeq in = pair.l();
                final IntSeq out = pair.r();
                for (int j = 0; j < in.unsafe().length; j++) {
                    assert 'a' <= in.at(j) && in.at(j) < 'a' + alphSize : IntSeq.toStringLiteral(in) + " " + pair;
                }
            }
            for(Pair<IntSeq,IntSeq> pair:informant){
                final IntSeq out = tr.specs.evaluate(learned,pair.l());
                assertNotNull(pair+"\n\n\nINFORMANT="+informant+"\n\nGENERATED="+optimal+"\n\n\nLEARNED="+learned+"\n\n\n"+init,out);
                assertEquals(pair.r(),out);
            }
            final Specification.AdvAndDelState<Integer, IntQueue> areEqiv = tr.specs.areEquivalent(optimal, learned, e -> IntQueue.asQueue(e.getOut()), p -> IntQueue.asQueue(p.getOut()), IntQueue::new);
//            if(areEqiv!=null || learned.size() > optimal.size()){
//                LearnLibCompatibility.visualize(optimal);
//                LearnLibCompatibility.visualize(learned);
//                OSTIA.ostia(OSTIA.buildPtt(alphSize, informantCopy.iterator()),true);
//            }
            assertNull(sample+"\n\n\n"+informant+"\n\n\nGENERATED="+optimal+"\n\n\nLEARNED="+learned,areEqiv);
            assert learned.size() <= optimal.size():learned.size()+" <= "+optimal.size();

        }
    }

    @Test
    void testRandom() throws Exception {
        final Random rnd = new Random(System.currentTimeMillis());
        final int testCount = 50;
        final int maxSymbol = 30;
        final int minSymbol = 20;
        HashMapBacked tr = new HashMapBacked(Config.config(minSymbol,maxSymbol, maxSymbol));
        for (int i = 1; i < testCount; i++) {
            System.out.println("Random test on " + i + " states");
            final int maxStates = i;
            final double partialityFact = rnd.nextDouble();
            final HashMapIntermediateGraph<Pos, E, P> rand =
                    tr.specs.randomDeterministic(maxStates, 1+rnd.nextInt(20), partialityFact > 0.1 ? partialityFact : 0,
                            () -> minSymbol + 1 + rnd.nextInt( maxSymbol-minSymbol),
                            (fromExclusive, toInclusive) -> new E(fromExclusive, toInclusive, IntSeq.rand(0, 4, minSymbol+1,  maxSymbol,rnd), 0),
                            () -> Pair.of(new P(IntSeq.rand(0, 4, minSymbol+1, maxSymbol,rnd), 0), Pos.NONE),rnd);
            final Specification.RangedGraph<Pos, Integer, E, P> optimal = tr.specs.optimiseGraph(rand);
            assert optimal.isDeterministic() == null;
            assert optimal.size() <= maxStates + 1 : optimal.size() + " <= " + (maxStates + 1) + "\n" + optimal + "\n===\n" + rand;
            final Object ALIVE = new Object();
            tr.specs.generate(optimal, ALIVE,(carry,backtrack, reachedState, activeBranches) -> {
                        if (rnd.nextDouble() > (20f / activeBranches)) return null;
                        final int len = LexUnicodeSpecification.BacktrackingNode.length(backtrack);
                        assert len <= maxStates;
                        return len < maxStates?ALIVE:null;
                    },
                    (backtrack, finalState) -> {
                        final LexUnicodeSpecification.BacktrackingHead head = new LexUnicodeSpecification.BacktrackingHead(backtrack, optimal.getFinalEdge(finalState));
                        final IntSeq in = head.randMatchingInput(rnd);
                        final IntSeq out = tr.specs.evaluate(optimal, in);
                        final IntSeq exp = tr.specs.collect(head,in);
                        assertEquals(in + "\n" + optimal, exp, out);
                    }, backtrack -> {

                    });
        }
    }

    @Test
    void testPipelines() throws Exception {
        PipelineTestCase[] cases = {
                p("@f = .* 1{ 'a':<0> . 'a' 2 } .* 1 ; {1->'a':'x'|'b':'y'|'c':'z'}", ps("aaa;x","aba;y","aca;z","kybfeggabager;y","kabaybfeggabager;y"), "abb",""),
                p("@f = (.* 1{ 'a':<0> . 'a' 2 } .* 1)* ; {1->'a':'x'|'b':'y'|'c':'z'}", ps("aaa;x","aba;y","aca;z","kybfeggabager;y","kabaybfeggabager;yy","kaaaybacafeggabager;xzy")),
                p("@f = (.* 2{ 1{'a':<0> . 'a' 2} } .* 1)* ; {1->'a':'x'|'b':'y'|'c':'z'}", ps("aaa;x","aba;y","aca;z","kybfeggabager;y","kabaybfeggabager;yy","kaaaybacafeggabager;xzy")),
                p("@f = (.* 2{ 1{'a':<0> . 'a' 2} } .* 1)* ; {2->'a':'x'|'b':'y'|'c':'z'}", ps("aaa;x","aba;y","aca;z","kybfeggabager;y","kabaybfeggabager;yy","kaaaybacafeggabager;xzy")),
                p("@f = 'a':'b'", ps("a;b"), ""),
                p("@f = 'a':'b'; 'b' : 'c' ", ps("a;c"), "", "b", "c", "d", "aa"),
                p("1@f = 'a':'b'; 'b' : 'c' ; 'c' : 'd' ", ps("a;d"), "", "b", "c", "d", "aa"),
                p("1@f = 'a':'b'; 'b' : 'c' ; 'c' : 'd' || 'x':'y' || 'y':'z' || 'z':'a'", ps("a;d","x;y","y;z","z;a"), "", "b", "c", "d", "aa"),
                p("@f = 'a':'b'; 'b' : 'c' ; 'c' : 'd' || 'x':'y' ; 'y':'z' ", ps("a;d","x;z"), "", "b", "c", "d", "aa"),
                p("1@f = 'a':'b'; 1@('b' : 'c' ; 'c' : 'd') || 'x':'y' ; 'y':'z' ", ps("a;d","x;z"), "", "b", "c", "d", "aa"),
                p("1@f = 1@('a':'b'; 'b' : 'c') ; 'c' : 'd' || 'x':'y' ; 'y':'z' ", ps("a;d","x;z"), "", "b", "c", "d", "aa"),
                p("@f = 'a':'b' ; assert 'b' ; 'b' : 'c' ", ps("a;c"), "", "b", "c", "d", "aa"),
                p("1@f = 'a':'b' ; assert 'b' ; 'b' : 'c' ; assert 'c' ; 'c' : 'd' ", ps("a;d"), "", "b", "c", "d", "aa"),
                p("@f = 'a':'b'|'h':'i' ; assert 'b'|'i' ; 'b' : 'c'|'i':'j' ; assert 'c'|'j' ; 'c' : 'd' |'j':'k'", ps("a;d", "h;k"), "", "b", "c", "d", "aa"),
                p("1@g = 'a':'b' ; assert 'b' ; 'b' : 'c'  ; assert 'c'" +
                        "@f = @g ; 'c' : 'd' ; assert 'd' ", ps("a;d"), "", "b", "c", "d", "aa"),
                p("@g = 'a':'b' ; assert 'b' ; 'b' : 'c' ; assert 'c' " +
                        "@f = @g ; 'c' : 'd' ; assert 'd'  ", ps("a;d"), "", "b", "c", "d", "aa"),
                pex("@f=('a':'a' 3|'a':'b' 3)'c'", CompilationError.WeightConflictingToThirdState.class),
                p("@f=nonfunc ('a':'a' 3|'a':'b' 3)'c'", ps(), "c","","b","caa","caca"),
        };

        int i =0;
        for (PipelineTestCase caze : cases) {
        	try {
        	    System.out.println((i++)+" Testing: "+caze.code);
	            HashMapBacked tr = new HashMapBacked(Config.config());
	            tr.parse(CharStreams.fromString(caze.code));
	            Pipeline<Pos, Integer, E, P, HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> g = tr.getPipeline("f");
	            assertNull(caze.shouldFail);
	            for (Positive pos : caze.ps) {
                    Seq<Integer> out = Pipeline.eval(tr.specs,g,new IntSeq(pos.input));
	                assertEquals(pos.output, out==null?null: IntSeq.toUnicodeString(out));
	            }
	            for (String neg : caze.negative) {
                    Seq<Integer> out = Pipeline.eval(tr.specs,g,new IntSeq(neg));
	                assertNull(out);
	            }
        	}catch (Throwable e) {
        		if(null==caze.shouldFail) {
        			throw e;
        		}else {
        			assertEquals(caze.shouldFail,e.getClass() );
        		}
			}

        }
    }
}
