package net.alagris;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EmptyStackException;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import net.alagris.CLI.OptimisedHashLexTransducer;
import net.alagris.ast.ThraxParser;

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
				c("func F[t]{return t \"qq\";} export x = F[\"x\":\"y\"];","@oioroot.x = 'xqq':'y';"),
				c("func F[t]{return t;} export x = F[\"x\":\"y\"];","@oioroot.x = 'x':'y';"),
				c("func F[]{t=\"x\":\"y\";return t;} export x = F[];","@oioroot.x = 'x':'y';"),
				c("func F[]{return \"x\":\"y\";} export x = F[];","@oioroot.x = 'x':'y';"),
				//
				c("y = \"x\":\"y\"; export x = y;","@oioroot.x = 'x':'y';"),
				c("export x = \"a\";", "@oinroot.x = 'a';"),
				c("#export x = \"a\";", ""),
				c("export x = \"[XXX]abc\";", "@oinroot.x = <1048576 97 98 99>;"),
				c("export x = \"[XXX]abc[YYY]def\";", "@oinroot.x = <1048576 97 98 99 1048577 100 101 102>;"),
				c("export x = \"[EOS][XXX]abc[YYY]def[BOS]\";", "@oinroot.x = <1114108 1048576 97 98 99 1048577 100 101 102 1114108>;"),
				c("export x = Optimize[\"a\"];", "@oinroot.x = 'a';"),
				c("export x = \"a\":\"b\";", "@oioroot.x = 'a':'b';"),
				c("export x = (\"a\":\"b\") | (\"e\":\"f\");", "@oioroot.x = 'a':'b'|'e':'f' 1;"),
				c("export x = (\"a\":\"b\")* | (\"e\":\"f\");", "@oioroot.x = ('a':'b') 1*|'e':'f' 2;"),
				c("export x = (\"a\":\"b\")? | (\"e\":\"f\");", "@oioroot.x = ('a':'b')?|'e':'f' 1;"),
				c("export x = \"a\"|\"b\";", "@oinroot.x = [a-b];"),
				c("export x = \"a\"|\"b\"|\"c\"|\"d\"|\"e\";", "@oinroot.x = [a-e];"),
				c("export x = \"a\"|\"b\"|\"c\"|\"c\"|\"c\";", "@oinroot.x = [a-c];"),
				c("export x = \"a\"|\"c\"|\"b\";", "@oinroot.x = [a-c];"),
				c("export x = \"[97]\"|\"[0142]\"|\"[0x63]\";", "@oinroot.x = [a-c];"),
				c("export x = \"[97][0142][0x63]\";", "@oinroot.x = 'abc';"),
				c("export x = \"a\" \"b\";", "@oinroot.x = 'ab';"),
				c("export x = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\") - (\"a\"|\"a\");", "@oinroot.x = [b-e];"),
				c("export x = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\") - (\"a\"|\"d\");", "@oinroot.x = [b-c]|'e' 1;"),
				c("export y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\");"
						+ "export x = y - (\"a\"|\"d\");", "@oinroot.y = [a-e];\n@oinroot.x = [b-c]|'e' 1;"),
				c("y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\");"
						+ "export x = y - (\"a\"|\"d\");",
								"@oinroot.x = [b-c]|'e' 1;"),
				c("y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\")\"xxx\";"
						+ "export x = y - (\"a\"|\"d\");", "oinroot.y = [a-e] 'xxx'\n" + 
								"@oinroot.x = subtract[oinroot.y,'a'|'d' 1];"),
				c("x = \"a\"; y = x x x ; export z = y y y ;",
						"@oinroot.z = 'aaaaaaaaa';"),
				c("export x = \"a\"; export y = x x x ; export z = y y y ;",
						"@oinroot.x = 'a';\n" + 
						"@oinroot.y = 'aaa';\n" + 
						"@oinroot.z = 'aaaaaaaaa';"),
				c("x = \"a\"; export y = x x x ; z = y y y ;",
						"@oinroot.y = 'aaa';\n"),
				c("x = \"a\"*; y = x x x ; export z = y y y ;",
						"oinroot.x = 'a' 1*\n" + 
						"oinroot.y = !!oinroot.x -1 !!oinroot.x -1 oinroot.x\n" + 
						"@oinroot.z = !!oinroot.y -3 !!oinroot.y -3 oinroot.y;"),
				c("export x = \"a\":\"\" ;","@oinroot.x = 'a';\n"),
				c("export x = (\"a\":\"1\") (\"bc\":\"\") (\"\":\"234\") (\"de\":\"5\") ;","@oioroot.x = 'abcde':'12345';\n"),
				c("export x = \"a\"{5} ;","@oinroot.x = 'aaaaa';\n"),
				c("export x = (\"a\":\"b\"):\"c\";","@oioroot.x = 'a':'bc';\n"),
				c("export x = \"a\"{4,5} ;","@oinroot.x = 'aaaa' 'a'?;\n"),
				c("export x = \"a\"{3,5} ;","@oinroot.x = 'aaa' 'a'? 'a'?;\n"),
				c("export x = \"a\"{3,6} ;","@oinroot.x = 'aaa' 'a'? 'a'? 'a'?;\n"),
				c("export x = CDRewrite[\"a\",\"pre\",\"post\",\"x\"|\"y\"|\"z\"];","@oioroot.x = (:'\\0' [x-z]|'preapost':'prepost' 1) 1*;"),
				c("export x = CDRewrite[\"a\":\"b\",\"pre\",\"post\",\"a\"|\"b\"|\"c\"|\"d\"|\"e\"];","@oioroot.x = (:'\\0' [a-e]|'preapost':'prebpost' 1) 1*;"),
				c("func F[x]{return x;} export x = F[\"a\":\"\"] ;",
						"@oinroot.x = 'a';"),
				c("func F[x]{y = x ; return y;} export x = F[\"a\":\"\"] ;","@oinroot.x = 'a';\n"),
				c("func F[x]{y = x ; return y y;} export x = F[\"a\":\"\"] ;","@oinroot.x = 'aa';\n"),
				c("func F[x]{y = x x ; return y y;} export x = F[\"a\":\"\"] ;","@oinroot.x = 'aaaa';\n"),
				c("func F[x]{y = x x ; return y y;} export x = F[\"a\":\"\"] F[\"b\":\"\"];","@oinroot.x = 'aaaabbbb';\n"),
				c("func F[x]{y = x? x ; return y y;} export x = F[\"a\":\"\"] ;","oinroot.x.__0.root.F.y = 'a'? 'a'\n" + 
						"oinroot.x.__0.root.F.return = !!oinroot.x.__0.root.F.y oinroot.x.__0.root.F.y\n" + 
						"@oinroot.x = oinroot.x.__0.root.F.return;"),
				c("func F[x]{y = x? x ; return y y;} export x = F[\"a\":\"\"] F[\"b\":\"\"];","oinroot.x.__0.root.F.y = 'a'? 'a'\n" + 
						"oinroot.x.__1.root.F.y = 'b'? 'b'\n" + 
						"oinroot.x.__0.root.F.return = !!oinroot.x.__0.root.F.y oinroot.x.__0.root.F.y\n" + 
						"oinroot.x.__1.root.F.return = !!oinroot.x.__1.root.F.y oinroot.x.__1.root.F.y\n" + 
						"@oinroot.x = oinroot.x.__0.root.F.return oinroot.x.__1.root.F.return;"),
				c("func F[x]{return x;} func G[x]{return F[x];} export x = G[\"a\":\"\"] ;","@oinroot.x = 'a';\n"),
				c("func F[x]{return x;} func G[x]{return F[x] F[x];} export x = G[\"a\":\"\"] ;","@oinroot.x = 'aa';\n"),
				c("func F[x]{return x x;} func G[x]{return F[x] F[x];} export x = G[\"a\":\"\"] ;","@oinroot.x = 'aaaa';\n"),
				c("func F[x]{return x x;} func G[x]{return F[x] F[x \",\"];} export x = G[\"a\":\"\"] ;","@oinroot.x = 'aaa,a,';\n"),
				c("func F[x]{return x x;} func G[x]{return F[x : \"0\"] F[x \",\"];} export x = G[\"a\":\"\"] ;","@oioroot.x = 'aaa,a,':'00';\n"),
				c("export kBytes = Optimize[\n" + 
						"  \"[1]\" |   \"[2]\" |   \"[3]\" |   \"[4]\" |   \"[5]\" |   \"[6]\" |   \"[7]\" |   \"[8]\" |   \"[9]\" |  \"[10]\" |\n" + 
						" \"[11]\" |  \"[12]\" |  \"[13]\" |  \"[14]\" |  \"[15]\" |  \"[16]\" |  \"[17]\" |  \"[18]\" |  \"[19]\" |  \"[20]\" |\n" + 
						" \"[21]\" |  \"[22]\" |  \"[23]\" |  \"[24]\" |  \"[25]\" |  \"[26]\" |  \"[27]\" |  \"[28]\" |  \"[29]\" |  \"[30]\" |\n" + 
						" \"[31]\" |  \"[32]\" |  \"[33]\" |  \"[34]\" |  \"[35]\" |  \"[36]\" |  \"[37]\" |  \"[38]\" |  \"[39]\" |  \"[40]\" |\n" + 
						" \"[41]\" |  \"[42]\" |  \"[43]\" |  \"[44]\" |  \"[45]\" |  \"[46]\" |  \"[47]\" |  \"[48]\" |  \"[49]\" |  \"[50]\" |\n" + 
						" \"[51]\" |  \"[52]\" |  \"[53]\" |  \"[54]\" |  \"[55]\" |  \"[56]\" |  \"[57]\" |  \"[58]\" |  \"[59]\" |  \"[60]\" |\n" + 
						" \"[61]\" |  \"[62]\" |  \"[63]\" |  \"[64]\" |  \"[65]\" |  \"[66]\" |  \"[67]\" |  \"[68]\" |  \"[69]\" |  \"[70]\" |\n" + 
						" \"[71]\" |  \"[72]\" |  \"[73]\" |  \"[74]\" |  \"[75]\" |  \"[76]\" |  \"[77]\" |  \"[78]\" |  \"[79]\" |  \"[80]\" |\n" + 
						" \"[81]\" |  \"[82]\" |  \"[83]\" |  \"[84]\" |  \"[85]\" |  \"[86]\" |  \"[87]\" |  \"[88]\" |  \"[89]\" |  \"[90]\" |\n" + 
						" \"[91]\" |  \"[92]\" |  \"[93]\" |  \"[94]\" |  \"[95]\" |  \"[96]\" |  \"[97]\" |  \"[98]\" |  \"[99]\" | \"[100]\" |\n" + 
						"\"[101]\" | \"[102]\" | \"[103]\" | \"[104]\" | \"[105]\" | \"[106]\" | \"[107]\" | \"[108]\" | \"[109]\" | \"[110]\" |\n" + 
						"\"[111]\" | \"[112]\" | \"[113]\" | \"[114]\" | \"[115]\" | \"[116]\" | \"[117]\" | \"[118]\" | \"[119]\" | \"[120]\" |\n" + 
						"\"[121]\" | \"[122]\" | \"[123]\" | \"[124]\" | \"[125]\" | \"[126]\" | \"[127]\" | \"[128]\" | \"[129]\" | \"[130]\" |\n" + 
						"\"[131]\" | \"[132]\" | \"[133]\" | \"[134]\" | \"[135]\" | \"[136]\" | \"[137]\" | \"[138]\" | \"[139]\" | \"[140]\" |\n" + 
						"\"[141]\" | \"[142]\" | \"[143]\" | \"[144]\" | \"[145]\" | \"[146]\" | \"[147]\" | \"[148]\" | \"[149]\" | \"[150]\" |\n" + 
						"\"[151]\" | \"[152]\" | \"[153]\" | \"[154]\" | \"[155]\" | \"[156]\" | \"[157]\" | \"[158]\" | \"[159]\" | \"[160]\" |\n" + 
						"\"[161]\" | \"[162]\" | \"[163]\" | \"[164]\" | \"[165]\" | \"[166]\" | \"[167]\" | \"[168]\" | \"[169]\" | \"[170]\" |\n" + 
						"\"[171]\" | \"[172]\" | \"[173]\" | \"[174]\" | \"[175]\" | \"[176]\" | \"[177]\" | \"[178]\" | \"[179]\" | \"[180]\" |\n" + 
						"\"[181]\" | \"[182]\" | \"[183]\" | \"[184]\" | \"[185]\" | \"[186]\" | \"[187]\" | \"[188]\" | \"[189]\" | \"[190]\" |\n" + 
						"\"[191]\" | \"[192]\" | \"[193]\" | \"[194]\" | \"[195]\" | \"[196]\" | \"[197]\" | \"[198]\" | \"[199]\" | \"[200]\" |\n" + 
						"\"[201]\" | \"[202]\" | \"[203]\" | \"[204]\" | \"[205]\" | \"[206]\" | \"[207]\" | \"[208]\" | \"[209]\" | \"[210]\" |\n" + 
						"\"[211]\" | \"[212]\" | \"[213]\" | \"[214]\" | \"[215]\" | \"[216]\" | \"[217]\" | \"[218]\" | \"[219]\" | \"[220]\" |\n" + 
						"\"[221]\" | \"[222]\" | \"[223]\" | \"[224]\" | \"[225]\" | \"[226]\" | \"[227]\" | \"[228]\" | \"[229]\" | \"[230]\" |\n" + 
						"\"[231]\" | \"[232]\" | \"[233]\" | \"[234]\" | \"[235]\" | \"[236]\" | \"[237]\" | \"[238]\" | \"[239]\" | \"[240]\" |\n" + 
						"\"[241]\" | \"[242]\" | \"[243]\" | \"[244]\" | \"[245]\" | \"[246]\" | \"[247]\" | \"[248]\" | \"[249]\" | \"[250]\" |\n" + 
						"\"[251]\" | \"[252]\" | \"[253]\" | \"[254]\" | \"[255]\"\n" + 
						"];","@oinroot.kBytes = .;")
				
		};
		try {
			for (Case c : sources) {
				c.solomonoff = c.solomonoff.trim().replaceAll("\\s\\s+", " ");
				try {
					ThraxParser<?, ?> parser = ThraxParser.parse(new File("."),CharStreams.fromString(c.thrax));
					assertEquals(c.solomonoff, parser.compileSolomonoff().trim().replaceAll("\\s\\s+", " "));
				} catch (Throwable e) {
					e.printStackTrace();
					System.out.println(c.thrax+"\n"+e);
					throw e;
				}
			}
		} catch (CompilationError e1) {
			e1.printStackTrace();
		}
	}
	
	@Test
	public void testFiles() throws Throwable {
		System.out.println(new File(".").getAbsoluteFile());
		String[] files= {"fst-thrax-grammars/es/numerals.grm;thrax1.mealy"};
//		String[] files= {"thrax1.grm;thrax1.mealy","thrax3.grm;thrax3.mealy","thrax4.grm;thrax4.mealy"};
		try {
			for (String file : files) {
				try {
					final String[] inOut = file.split(";");
					final String expected = new String(Files.readAllBytes(Paths.get("src/test/resources",inOut[1]))).trim();
					final File in = new File("src/test/resources/",inOut[0]);
					ThraxParser<?, ?> parser = ThraxParser.parse(in,CharStreams.fromFileName(in.getPath()));
					assertEquals(expected, parser.compileSolomonoff().trim(),file);
				} catch (Throwable e) {
					e.printStackTrace();
					System.out.println(file+"\n"+e);
					throw e;
				}
			}
		} catch (CompilationError e1) {
			e1.printStackTrace();
		}
	}
}
