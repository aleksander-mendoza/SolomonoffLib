package net.alagris;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import net.alagris.EpsilonFree.E;
import net.alagris.Glushkov;
import net.alagris.Mealy;
import net.alagris.Simple;
import net.alagris.Regex.Eps;
import net.alagris.Regex.R;
import net.alagris.Simple.A;

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
        return new TestCase(regex, positive, negative);
    }

    @Test
    void test() throws Exception {
        TestCase[] testCases = {
                
                t("\"a\"", ps("a;"), "b","c",""," "),
                t("(\"a\")", ps("a;"), "b","c",""," "),
                t("((\"a\"))", ps("a;"), "b","c",""," "),
                t("\"\"", ps(";"), "a", "b", "c", " "),
                t("\"a\"*", ps(";","a;","aa;","aaa;","aaaaaaaaaaaaaa;"), "b","c"," "),
                t("\"ab\"", ps("ab;"),"a", "b","c",""," "), 
                t("\"abc\"", ps("abc;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                t("\"a(b|e)c\"", ps("a(b|e)c;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc ","abc","aec"),
                t("\"a\"(\"b\"|\"e\")\"c\"", ps("abc;","aec;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                t("\"a\"(\"b\"|\"e\"|\"f\")\"c\"", ps("abc;","aec;","afc;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                t("\"a\"(\"b\"|\"e\"|\"f\")*\"c\"", ps("abc;","aec;","afc;","abbc;","aeec;","affc;","abec;","aefc;","afbc;","abbbeffebfc;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                t("\"a\":\"a\"", ps("a;a"), "b","c",""," "),
                t("\"a\":\"aa\"", ps("a;aa"), "b","c",""," "),
                t("\"a\":\" \"", ps("a; "), "b","c",""," "),
                t("\"a\":\"TeSt Yo MaN\"", ps("a;TeSt Yo MaN"), "b","c",""," "),
                t("\"\":\"   \"", ps(";   "), "a", "b", "c", " "),
                t("(\"a\")*", ps(";","a;","aa;","aaa;","aaaaaaaaaaaaaa;"), "b","c"," "),
                t("\"ab\"", ps("ab;"),"a", "b","c",""," "), 
                t("\"abc\"", ps("abc;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                t("\"a(b|e)c\"", ps("a(b|e)c;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc ","abc","aec"),
                t("\"a\"(\"b\"|\"e\")\"c\"", ps("abc;","aec;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                t("\"a\"(\"b\"|\"e\"|\"f\")\"c\"", ps("abc;","aec;","afc;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                t("\"a\"(\"b\"|\"e\"|\"f\")*\"c\"", ps("abc;","aec;","afc;","abbc;","aeec;","affc;","abec;","aefc;","afbc;","abbbeffebfc;"),"a", "b","c",""," ","ab", "bb","cb","abb"," abc","abc "),
                };

        int i =0;
        for (TestCase testCase : testCases) {
            try {
            final A ast = MealyParser.parse(testCase.regex);
            final R regex = Simple.removeEpsilon(ast);
            final Eps epsilonFree = regex.pullEpsilonMappings();
            final Mealy automaton = Glushkov.glushkov(epsilonFree);
            for(Positive pos:testCase.positive) {
                String out = automaton.evaluate(pos.input);
                assertEquals(i+"["+testCase.regex+"];"+pos.input, pos.output, out);
            }
            }catch (Exception e) {
                throw new Exception("{"+testCase.regex+"};"+e.getClass()+" "+e.getMessage(),e);
            }
            i++;
        }

    }
}
