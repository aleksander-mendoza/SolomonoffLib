package net.alagris.cli;

import net.alagris.cli.conv.Compiler;
import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.cli.conv.ThraxParser;
import net.alagris.lib.ArrayBacked;
import net.alagris.lib.Config;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KolmTest {


    @Test
    public <N, G extends IntermediateGraph<Pos, E, P, N>> void testThrax() throws IOException, CompilationError {
        final String[][] cases = {

//                {"es/chars.grm", null
//                        , "Kroot.ToLowercase;0", "Kroot.ToLowercase;9", "Kroot.ToLowercase;5", "Kroot.ToLowercase;8", "Kroot.ToLowercase;a", "Kroot.ToLowercase;ñ", "Kroot.ToLowercase;A;a", "Kroot.ToLowercase;B;b", "Kroot.ToLowercase;C;c", "Kroot.ToLowercase;Z;z", "Kroot.ToLowercase;Y;y", "Kroot.ToLowercase;Á;á", "Kroot.ToLowercase;É;é", "Kroot.ToLowercase;Í;í", "Kroot.ToLowercase;Ó;ó", "Kroot.ToLowercase;Ñ;ñ", "Kroot.ToLowercase;Ú;ú"
//                        , "Kroot.ToUppercase;0", "Kroot.ToUppercase;9", "Kroot.ToUppercase;5", "Kroot.ToUppercase;8", "Kroot.ToUppercase;A", "Kroot.ToUppercase;Ñ", "Kroot.ToUppercase;a;A", "Kroot.ToUppercase;b;B", "Kroot.ToUppercase;c;C", "Kroot.ToUppercase;z;Z", "Kroot.ToUppercase;y;Y", "Kroot.ToUppercase;á;Á", "Kroot.ToUppercase;é;É", "Kroot.ToUppercase;í;Í", "Kroot.ToUppercase;ó;Ó", "Kroot.ToUppercase;ñ;Ñ", "Kroot.ToUppercase;ú;Ú"
//                        , "Kroot.RWToUpper;0123456789abcdefghijklmnopqrstuvwxyzáéíóñú!@#$%^&*()ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÑÚ;0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÑÚ!@#$%^&*()ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÑÚ"
//                        , "Kroot.RWToLower;0123456789abcdefghijklmnopqrstuvwxyzáéíóñú!@#$%^&*()ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÑÚ;0123456789abcdefghijklmnopqrstuvwxyzáéíóñú!@#$%^&*()abcdefghijklmnopqrstuvwxyzáéíóñú"}
//                ,
//                {"es/numerals.grm",null,
//                        "Kroot.Cardinal;2;dos"},

//                {"es/numerals.grm",null,
//                        "Kroot.Cardinal;1;uno"},
//                {"es/numerals.grm",null,
//                        "Kroot._numbers_to_99;cuatro[GMA][NDUAL];4[E0]",
//                        "Kroot.Fact;2;2[E0]",
//                        "Kroot.Fact;4;4[E0]",
//                        "Kroot.numbers_to_1t;2[E0];dos[GMA][NDUAL]",
//                        "Kroot.numbers_to_1t;4[E0];cuatro[GMA][NDUAL]",
//                        "Kroot.CardinalFrame;2;dos[GMA][NDUAL]",
//                        "Kroot.CardinalFrame;4;cuatro[GMA][NDUAL]",
//                        "Kroot.IsFForm.__0.root.IsForm.return;una[GF][NSG];una",
//                        "Kroot.IsMForm.__0.root.IsForm.return;uno[GMA][NSG];uno",
//                        "Kroot.IsMForm.__0.root.IsForm.return;una[GF][NSG];una[GF][NSG]",
////                        "Kroot.Cardinal;1;una",
//                        "Kroot.Cardinal;2;dos",
//                        "Kroot.Cardinal;3;tres",
//                        "Kroot.Cardinal;4;cuatro",
//                        "Kroot.NumbersTo1B;4[E0];cuatro[GMA][NDUAL]",
//                        "Kroot.numbers_to_1m;4[E0];cuatro[GMA][NDUAL]",
//                        "Kroot.NumbersTo999;4[E0];cuatro[GMA][NDUAL]",
//                        "Kroot.numbers_to_99;4[E0];cuatro[GMA][NDUAL]",
//                        "Kroot.units;4[E0];cuatro[GMA][NDUAL]",
//                        "Kroot._units;cuatro[GMA][NDUAL];4[E0]",
//                        "Kroot._numbers_to_99;cuatro[GMA][NDUAL];4[E0]",
//                        "Kroot._NumbersTo999;cuatro[GMA][NDUAL];4[E0]",
//                        "Kroot._numbers_to_1m;cuatro[GMA][NDUAL];4[E0]",
//                        "Kroot._NumbersTo1B;cuatro[GMA][NDUAL];4[E0]",
//                        "Kroot._Fact;4[E0];4",
//                        "Kroot._numbers_to_1t;cuatro[GMA][NDUAL];4[E0]",
//                        "Kroot._Cardinal;cuatro;4"
//                },


//                {"thrax-1.grm",null,"Kroot.e;a;x"},
                {"thrax0.grm", null, "Kroot.z;qq;uu"},
                {"thrax1.grm", null, "Kroot.z;qq;uu", "Kroot.a;hello world;hello world", "Kroot.b;a;a", "Kroot.b;b;b", "Kroot.b;c;c", "Kroot.c;babacccbacbb;babacccbacbb", "Kroot.c;;", "Kroot.c;aaaaa;aaaaa", "Kroot.c;aaaaa;aaaaa",
                        "Kroot.d;ababababhello world;ababababhello world", "Kroot.d;hello world;hello world", "Kroot.e;abababab;hello world", "Kroot.e;;hello world"},
                {"thrax2.grm", null, "Kroot.a;a;c", "Kroot.b;ad;cd", "Kroot.c;d;d", "Kroot.c;a;c",
                        "Kroot.d;aaa;ccc", "Kroot.d;;", "Kroot.e;a;c", "Kroot.e;e;f", "Kroot.f;ae;cf", "Kroot.g;ae;cg",},
                {"thrax3.grm", null, "Kroot.a;a;c", "Kroot.b;;", "Kroot.b;a;b", "Kroot.b;aa;bb", "Kroot.b;aaa;2", "Kroot.c;;", "Kroot.c;a;b", "Kroot.c;aa;bb", "Kroot.c;aaa;bbb",},
                {"thrax4.grm", null, "Kroot.g;abcbcatre;hello worldtre", "Kroot.g;tre;hello worldtre",
                        "Kroot.f;aaatrtraa;ccctrtrbb", "Kroot.f;aaatrtraaa;ccctrtr2",
                        "Kroot.f;aaatraa;ccctrbb", "Kroot.f;aaatraaa;ccctr2",
                },
                {"thrax5.grm", null, "Kroot.a;abc;xyz", "Kroot.b;abbbc;xyyyz", "Kroot.b;ac;xz",
                        "Kroot.c;ababccba;xyxyzzyx", "Kroot.c;;",
                        "Kroot.d;a;x", "Kroot.d;b;y", "Kroot.d;c;z",
                        "Kroot.e;a;0", "Kroot.e;b;1", "Kroot.e;c;z", "Kroot.e;0;x", "Kroot.e;1;y"
                },
                {"thrax6.grm", null,
                        "Kroot.a;a;c", "Kroot.a;x;y",
                        "Kroot.b;a;c", "Kroot.b;x;y", "Kroot.b;0;3",
                        "Kroot.c;a;c", "Kroot.c;x;y", "Kroot.c;0;3",
                        "Kroot.d;a;c", "Kroot.d;x;y", "Kroot.d;0;3",
                        "Kroot.a1;ax;cy",
                        "Kroot.b1;ax0;cy3",
                        "Kroot.c1;a0x;c3y",
                        "Kroot.d1;0ax;3cy",
                },
                {"thrax7.grm", null,
                        "Kroot.a;a;c", "Kroot.a;x;y",
                        "Kroot.b;a;c", "Kroot.b;x;y", "Kroot.b;0;3",
                        "Kroot.c;a;c", "Kroot.c;x;y", "Kroot.c;0;3",
                        "Kroot.d;a;c", "Kroot.d;x;y", "Kroot.d;0;3",
                        "Kroot.a1;ax;cy",
                        "Kroot.b1;ax0;cy3",
                        "Kroot.c1;a0x;c3y",
                        "Kroot.d1;0ax;3cy",
                },
                {"thrax8.grm", null,
                        "Kroot.NumbersTo1B;gdf;gajai"},
                {"thrax9.grm", null,
                        "Kroot.a;;", "Kroot.a;a;c", "Kroot.a;aa;cc", "Kroot.a;aaaaa;ccccc",
                        "Kroot.b;", "Kroot.b;a;c", "Kroot.b;aa;cc", "Kroot.b;aaaaa;ccccc",
                        "Kroot.c;;", "Kroot.c;a;c",},
                {"thrax10.grm", null,
                        "Kroot.a;;", "Kroot.a;a", "Kroot.a;aa", "Kroot.a;aaaaa",
                        "Kroot.b;aaa;ccc",
                        "Kroot.c;", "Kroot.c;a", "Kroot.c;aa", "Kroot.c;aaa;ccc", "Kroot.c;aaaa;cccc", "Kroot.c;aaaaa;ccccc", "Kroot.c;aaaaaa;cccccc", "Kroot.c;aaaaaaa",},
                {"thrax11.grm", null, "Kroot.y;b;a", "Kroot.z;a;b"},

                {"thrax12.grm", null, "Kroot.a;c;a", "Kroot.b;cd;ad", "Kroot.c;d;d", "Kroot.c;c;a",
                        "Kroot.d;ccc;aaa", "Kroot.d;;", "Kroot.e;c;a", "Kroot.e;f;e", "Kroot.f;cf;ae", "Kroot.g;cg;ae",},
                {"thrax13.grm", null, "Kroot.a;xyz;abc", "Kroot.b;xyyyz;abbbc", "Kroot.b;xz;ac",
                        "Kroot.c;xyxyzzyx;ababccba", "Kroot.c;;",
                        "Kroot.d;x;a", "Kroot.d;y;b", "Kroot.d;z;c",
                        "Kroot.e;0;a", "Kroot.e;1;b", "Kroot.e;z;c", "Kroot.e;x;0", "Kroot.e;y;1"
                },
                {"thrax14.grm", null,
                        "Kroot.a;aa;bb", "Kroot.a;bb;bb",
                        "Kroot.b;aa;aa", "Kroot.b;bb;aa",
                        "Kroot.c;aa;aa", "Kroot.c;bb;aa",
                        "Kroot.d;aa;bb", "Kroot.d;bb;bb",},
                {"thrax15.grm", null,
                        "Kroot.a;a;0",
                        "Kroot.a;b;1",
                        "Kroot.a;c;2",
                        "Kroot.a;aa;4",
                        "Kroot.a;ab;5",
                        "Kroot.a;hello;world",
                        "Kroot.a;;empty",
                        "Kroot.a;empty;",
                        "Kroot.b;0;a",
                        "Kroot.b;1;b",
                        "Kroot.b;2;c",
                        "Kroot.b;4;aa",
                        "Kroot.b;5;ab",
                        "Kroot.b;world;hello",
                        "Kroot.b;empty;",
                        "Kroot.b;;empty"},
                {"thrax16.grm", null,
                        "Kroot.a;a;a",
                        "Kroot.b;b;b",
                        "Kroot.c;aaa;aaa", "Kroot.c;;", "Kroot.c;a;a",
                        "Kroot.d;bbb;bbb", "Kroot.d;;", "Kroot.d;b;b",
                        "Kroot.e;aaa;aaa", "Kroot.e;aa;aa", "Kroot.e;a;a",
                        "Kroot.f;bbb;bbb", "Kroot.f;bb;bb", "Kroot.f;b;b",
                        "Kroot.g;aa;aa",
                        "Kroot.h;bb;bb",
                        "Kroot.i;a;a", "Kroot.i;c;c",
                        "Kroot.j;b;b", "Kroot.j;d;d",
                        "Kroot.k;a;a", "Kroot.k;c;c",
                        "Kroot.l;b;b", "Kroot.l;d;d",
                        "Kroot.m;a;a", "Kroot.m;b;b",
                        "Kroot.n;c;c", "Kroot.n;d;d"},
                {"thrax17.grm", null,
                        "Kroot.a;a",
                        "Kroot.b;a","Kroot.b;;b","Kroot.b;aaaaa;b",
                        "Kroot.c;a","Kroot.c;;","Kroot.c;aaaaa;bbbbb",
                        "Kroot.d;a;b",
                        "Kroot.e;aaa;bbb", "Kroot.e;aa;bb", "Kroot.e;a;b","Kroot.e;",
                        "Kroot.f;abcd;bbbb", "Kroot.f;aa", "Kroot.f;",
                        "Kroot.g;abbcdcd;bbbbbbb", "Kroot.g;bbb", "Kroot.g;",
                        "Kroot.h;abbcdcd;bbbbbbb", "Kroot.h;cccc", "Kroot.h;",
                        "Kroot.i;abbcdcd;bbbbbbb", "Kroot.i;d", "Kroot.i;",
                        "Kroot.a0;a",
                        "Kroot.b0;a","Kroot.b0;;b","Kroot.b0;aaaaa;b",
                        "Kroot.c0;a","Kroot.c0;;","Kroot.c0;aaaaa;bbbbb",
                        "Kroot.d0;a;b",
                        "Kroot.e0;aaa;bbb", "Kroot.e0;aa;bb", "Kroot.e0;a;b","Kroot.e0;",},
                {"thrax18.grm", null,
                        "Kroot.a;a;a",
                        "Kroot.a;b;b",
                        "Kroot.a;c;c",
                        "Kroot.a;aa;aa",
                        "Kroot.a;ab;ab",
                        "Kroot.a;hello;hello",
                        "Kroot.a;;",
                        "Kroot.b;a;a",
                        "Kroot.b;b;b",
                        "Kroot.b;c;c",
                        "Kroot.b;aa;aa",
                        "Kroot.b;ab;ab",
                        "Kroot.b;hello;hello",
                        "Kroot.b;;"},
                {"thrax19.grm", null,
                        "Kroot.a;aac;abc",
                        "Kroot.b;aac;abc",
                        "Kroot.c;aac;aac",
                        "Kroot._a;abc;aac",
                        "Kroot._b;abc;aac",
                        "Kroot._c;aac;aac"},
                {"thrax20.grm", null,
                        "Kroot.a;a;a","Kroot.a;aa","Kroot.a;aab","Kroot.a;ab","Kroot.a;aba;aba","Kroot.a;abbb;abbb",
                        "Kroot.b;a;a","Kroot.b;aa","Kroot.b;aab","Kroot.b;ab","Kroot.b;aba;aba","Kroot.b;abbb;abbb",
                        "Kroot.b;aaaa","Kroot.b;ababaa;ababaa","Kroot.b;ababaab;ababaab","Kroot.b;abbbab;abbbab",
                        "Kroot.c;a;a","Kroot.c;aa","Kroot.c;aab","Kroot.c;ab","Kroot.c;aba;aba","Kroot.c;abbb;abbb",
                        "Kroot.c;aaaa","Kroot.c;ababaa;ababaa","Kroot.c;ababaab;ababaab","Kroot.c;abbbab;abbbab",
                        "Kroot.c;aaaab","Kroot.c;aaaaaab","Kroot.c;aaaaaaa",
                        "Kroot.d;a","Kroot.d;b;b","Kroot.d;c;c","Kroot.d;d;d",
                        "Kroot.e;a;a","Kroot.e;b","Kroot.e;c;c","Kroot.e;d;d",
                        "Kroot.f;a;a","Kroot.f;b","Kroot.f;c","Kroot.f;d;d",
                        "Kroot.g;a;a","Kroot.g;b","Kroot.g;c;c","Kroot.g;d",},
                {"thrax22.grm", "ignoreEpsilonsUnderKleeneClosure",
                        "Kroot.a;aaa;aaa","Kroot.a;;",
                        "Kroot.b;aaa;aaa","Kroot.b;;a"},
                {"byte.grm", null, "Kroot.kBytes;a;a", "Kroot.kBytes;b;b", "Kroot.kBytes;z;z", "Kroot.kBytes; ; ", "Kroot.kBytes;0;0", "Kroot.kBytes;9;9", "Kroot.kBytes;#;#", "Kroot.kBytes;!;!", "Kroot.kBytes;&;&", "Kroot.kBytes;(;("
                        , "Kroot.kDigit;0;0", "Kroot.kDigit;9;9", "Kroot.kDigit;5;5", "Kroot.kDigit;8;8"
                        , "Kroot.kLower;a;a", "Kroot.kLower;b;b", "Kroot.kLower;y;y", "Kroot.kLower;z;z"
                        , "Kroot.kUpper;A;A", "Kroot.kUpper;B;B", "Kroot.kUpper;C;C", "Kroot.kUpper;Z;Z"
                        , "Kroot.kAlpha;A;A", "Kroot.kAlpha;B;B", "Kroot.kAlpha;C;C", "Kroot.kAlpha;Z;Z", "Kroot.kAlpha;a;a", "Kroot.kAlpha;b;b", "Kroot.kAlpha;y;y", "Kroot.kAlpha;z;z"
                        , "Kroot.kAlnum;A;A", "Kroot.kAlnum;B;B", "Kroot.kAlnum;C;C", "Kroot.kAlnum;Z;Z", "Kroot.kAlnum;a;a", "Kroot.kAlnum;b;b", "Kroot.kAlnum;y;y", "Kroot.kAlnum;z;z", "Kroot.kAlnum;0;0", "Kroot.kAlnum;9;9", "Kroot.kAlnum;5;5", "Kroot.kAlnum;8;8"
                        , "Kroot.kSpace; ; ", "Kroot.kSpace;\n;\n", "Kroot.kSpace;\t;\t", "Kroot.kSpace;\r;\r"
                        , "Kroot.kNotSpace;a;a", "Kroot.kNotSpace;b;b", "Kroot.kNotSpace;z;z", "Kroot.kNotSpace;_;_", "Kroot.kNotSpace;0;0", "Kroot.kNotSpace;9;9", "Kroot.kNotSpace;#;#", "Kroot.kNotSpace;!;!", "Kroot.kNotSpace;&;&", "Kroot.kNotSpace;(;("
                        , "Kroot.kPunct;.;.", "Kroot.kPunct;,;,", "Kroot.kPunct;?;?", "Kroot.kPunct;!;!"},



        };
        int num = -1;
        for (String[] caze : cases) {
            num++;
            final String thrax = caze[0];
            final String flags = caze[1];
            System.out.println(num + " Testing: " + thrax);
            final File file = new File("src/test/resources", thrax);
            final CharStream stream = CharStreams.fromFileName(file.getPath());
            final ThraxParser<N, G> tp = ThraxParser.parse(file, stream);
            for (int i = 2; i < caze.length; i++) {
                final String[] parts = caze[i].split(";", -1);
                final String name = parts[0].startsWith("~") ? parts[0].substring(1) : parts[0];
                tp.fileImportHierarchy.peek().export.add(name.substring(1));
            }
            final String convertedSol = Compiler.compileSolomonoff(false, true, false, tp);
            System.out.println(convertedSol);
            final Config c = Config.config();
            if("ignoreEpsilonsUnderKleeneClosure".equals(flags)){
                c.errorOnEpsilonUnderKleeneClosure(false);
            }
            final ArrayBacked tr = new ArrayBacked(c);
            try {
                tr.parse(CharStreams.fromString(convertedSol));
            } catch (Throwable t) {

                throw t;
            }

            for (int i = 2; i < caze.length; i++) {
                final String nameInOut = caze[i];
                final String[] parts = nameInOut.split(";", -1);
                final String name;
                final IntSeq in;
                final IntSeq exp;
                final String expOrig = parts.length == 3 ? parts[2] : null;
                final String inOrig = parts[1];
                if (parts[0].startsWith("~")) {
                    name = parts[0].substring(1);
                    in = ParserListener.parseCodepointOrStringLiteral(parts[1]);
                    exp = parts.length == 3 ? ParserListener.parseCodepointOrStringLiteral(parts[2]) : null;
                } else {
                    name = parts[0];
                    in = parseTmpSymbols(tp, parts[1]);
                    exp = parts.length == 3 ? parseTmpSymbols(tp, parts[2]) : null;
                }

                final Specification.RangedGraph<Pos, Integer, E, P> optimal = tr.getOptimisedTransducer(name);
                final IntSeq out;
                if (optimal == null) {
                    final Pipeline<Pos, Integer, E, P, ArrayIntermediateGraph.N<Pos, E>, ArrayIntermediateGraph<Pos, E, P>> p = tr.getPipeline(name);
                    assertNotNull(name + " " + tr.specs.variableAssignments.keySet() + " " + tr.specs.pipelines.keySet(), p);
                    final Seq<Integer> o = tr.specs.evaluate(p, in);
                    out = o == null ? null : new IntSeq(o);
                } else {
                    out = tr.specs.evaluate(optimal, in);
                }
                String outS = parseCodepointsAsTmpSymbols(tp, out);
                if (!Objects.equals(exp, out)) {
                    System.err.println(convertedSol);
                }
                Assert.assertEquals(name + "(" + inOrig + ") = " + outS + "!=" + expOrig + "\n", exp, out);
            }
        }

    }

    private <N, G extends IntermediateGraph<Pos, E, P, N>> String parseCodepointsAsTmpSymbols(ThraxParser<N, G> tp, IntSeq in) {
        if (in == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int cp : in) {
            Map.Entry<String, Integer> e = Util.find(tp.TEMPORARY_THRAX_SYMBOLS.entrySet(), v -> v.getValue() == cp);
            if (e == null) {
                sb.appendCodePoint(cp);
            } else {
                sb.append("[").append(e.getKey()).append("]");
            }

        }
        String inS = sb.toString();
        return inS;
    }

    private <N, G extends IntermediateGraph<Pos, E, P, N>> IntSeq parseTmpSymbols(ThraxParser<N, G> tp, String inOrig) {
        final IntSeq in;
        final Matcher m = Pattern.compile("\\[[a-zA-Z0-9]+\\]").matcher(inOrig);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String tmpSymbol = m.group().substring(1, m.group().length() - 1);
            int s = tp.TEMPORARY_THRAX_SYMBOLS.get(tmpSymbol);
            m.appendReplacement(sb, new String(new int[]{s}, 0, 1));
        }
        m.appendTail(sb);
        in = new IntSeq(sb.toString());
        return in;
    }


    @Test
    public <N, G extends IntermediateGraph<Pos, E, P, N>> void testThraxConv() throws IOException, CompilationError {
        final String[] cases = {
                "thrax_syntactic_eq0.grm;sol_syntactic_eq0.mealy",
                "thrax_syntactic_eq1.grm;sol_syntactic_eq1.mealy",
                "thrax_syntactic_eq2.grm;sol_syntactic_eq2.mealy",
                "thrax_syntactic_eq3.grm;sol_syntactic_eq3.mealy",
                "thrax_syntactic_eq4.grm;sol_syntactic_eq4.mealy",
        };
        int num = 0;
        for (String caze : cases) {
            num++;
            final String[] parts = caze.split(";");
            final String thraxFile = parts[0];
            final String solFile = parts[1];
            System.out.println(num + " Testing: " + thraxFile + " with " + solFile);

            final File file = new File("src/test/resources", thraxFile);
            final CharStream stream = CharStreams.fromFileName(file.getPath());
            final ThraxParser<N, G> tp = ThraxParser.parse(file, stream);
            final String convertedSol = Compiler.compileSolomonoff(false, false, false, tp);
            byte[] encoded = Files.readAllBytes(Paths.get("src/test/resources", solFile));
            assertEquals(new String(encoded).trim(), convertedSol.trim());
        }

    }
}
