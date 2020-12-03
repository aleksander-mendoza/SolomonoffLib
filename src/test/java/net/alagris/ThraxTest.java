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

public class ThraxTest {

//	static class Case {
//		String thrax;
//		String solomonoff;
//	}
//
//	static Case c(String thrax, String solomonoff) {
//		Case c = new Case();
//		c.solomonoff = solomonoff.trim();
//		c.thrax = thrax;
//		return c;
//	}
//
//	@Test
//	public void test() {
//		final Case[] sources = {
//				
//				c("func F[x]{return x x;} func G[x]{return F[x : \"0\"] F[x \",\"];} export x = G[\"a\":\"\"] ;","x = 'aaa,a,':'00'\n"),
//				c("export x = \"a\";", "x = 'a'"),
//				c("#export x = \"a\";", ""),
//				c("export x = \"[XXX]abc\";", "!!.XXX = <1048576>\nx = .XXX 'abc'"),
//				c("export x = \"[XXX]abc[YYY]def\";", "!!.YYY = <1048577>\n!!.XXX = <1048576>\nx = .XXX 'abc' .YYY 'def'"),
//				c("export x = \"[EOS][XXX]abc[YYY]def[BOS]\";", "!!.BOS = <1114108>\n" + 
//						"!!.YYY = <1048577>\n" + 
//						"!!.EOS = <1114109>\n" + 
//						"!!.XXX = <1048576>\n" + 
//						"x = .EOS .XXX 'abc' .YYY 'def' .BOS"),
//				c("export x = Optimize[\"a\"];", "x = 'a'"),
//				c("export x = \"a\":\"b\";", "x = 'a':'b'"),
//				c("export x = (\"a\":\"b\") | (\"e\":\"f\");", "x = 'a':'b'|'e':'f'"),
//				c("export x = (\"a\":\"b\")* | (\"e\":\"f\");", "x = 'a':'b'*|'e':'f'"),
//				c("export x = (\"a\":\"b\")? | (\"e\":\"f\");", "x = 'a':'b'?|'e':'f'"),
//				c("export x = \"a\"|\"b\";", "x = [a-b]"),
//				c("export x = \"a\"|\"b\"|\"c\"|\"d\"|\"e\";", "x = [a-e]"),
//				c("export x = \"a\"|\"b\"|\"c\"|\"c\"|\"c\";", "x = [a-c]"),
//				c("export x = \"a\"|\"c\"|\"b\";", "x = [a-c]"),
//				c("export x = \"[97]\"|\"[0142]\"|\"[0x63]\";", "x = [a-c]"),
//				c("export x = \"[97][0142][0x63]\";", "x = 'abc'"),
//				c("export x = \"a\" \"b\";", "x = 'ab'"),
//				c("export x = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\") - (\"a\"|\"a\");", "x = [b-e]"),
//				c("export x = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\") - (\"a\"|\"d\");", "x = [b-c]|'e'"),
//				c("export y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\");"
//						+ "export x = y - (\"a\"|\"d\");", "y = [a-e]\nx = [b-c]|'e'"),
//				c("y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\");"
//						+ "export x = y - (\"a\"|\"d\");", "x = [b-c]|'e'"),
//				c("y = (\"a\"|\"b\"|\"c\"|\"d\"|\"e\")\"xxx\";"
//						+ "export x = y - (\"a\"|\"d\");", "y = [a-e] 'xxx'\nx = subtract[y,'a'|'d']"),
//				c("x = \"a\"; y = x x x ; export z = y y y ;","z = 'aaaaaaaaa'\n"),
//				c("export x = \"a\"; export y = x x x ; export z = y y y ;","x = 'a'\ny = 'aaa'\nz = 'aaaaaaaaa'\n"),
//				c("x = \"a\"; export y = x x x ; z = y y y ;","y = 'aaa'\n"),
//				c("x = \"a\"*; y = x x x ; export z = y y y ;","x = 'a'*\ny = !!x !!x x\nz = !!y !!y y\n"),
//				c("export x = \"a\":\"\" ;","x = 'a'\n"),
//				c("export x = (\"a\":\"1\") (\"bc\":\"\") (\"\":\"234\") (\"de\":\"5\") ;","x = 'abcde':'12345'\n"),
//				c("export x = \"a\"{5} ;","x = 'aaaaa'\n"),
//				c("export x = (\"a\":\"b\"):\"c\";","x = 'a':'bc'\n"),
//				c("export x = \"a\"{4,5} ;","x = 'aaaa' 'a'?\n"),
//				c("export x = \"a\"{3,5} ;","x = 'aaa' 'a'? 'a'?\n"),
//				c("export x = \"a\"{3,5} ;","x = 'aaa' 'a'? 'a'?\n"),
//				c("export x = CDRewrite[\"a\",\"pre\",\"post\",\"x\"|\"y\"|\"z\"];","x = ('preapost':'prepost' 2|'':'\\0' [x-z])*\n"),
//				c("export x = CDRewrite[\"a\":\"b\",\"pre\",\"post\",\"a\"|\"b\"|\"c\"|\"d\"|\"e\"];","x = ('preapost':'prebpost' 2|'':'\\0' [a-e])*\n"),
//				c("func F[x]{return x;} export x = F[\"a\":\"\"] ;","x = 'a'\n"),
//				c("func F[x]{y = x ; return y;} export x = F[\"a\":\"\"] ;","x = 'a'\n"),
//				c("func F[x]{y = x ; return y y;} export x = F[\"a\":\"\"] ;","x = 'aa'\n"),
//				c("func F[x]{y = x x ; return y y;} export x = F[\"a\":\"\"] ;","x = 'aaaa'\n"),
//				c("func F[x]{y = x x ; return y y;} export x = F[\"a\":\"\"] F[\"b\":\"\"];","x = 'aaaabbbb'\n"),
//				c("func F[x]{y = x? x ; return y y;} export x = F[\"a\":\"\"] ;","x.__0.F.y = 'a'? 'a'\n" + 
//						"x.__0.F = !!x.__0.F.y x.__0.F.y\n" + 
//						"x = x.__0.F"),
//				c("func F[x]{y = x? x ; return y y;} export x = F[\"a\":\"\"] F[\"b\":\"\"];","x.__0.F.y = 'a'? 'a'\n" + 
//						"x.__0.F = !!x.__0.F.y x.__0.F.y\n" + 
//						"x.__1.F.y = 'b'? 'b'\n" + 
//						"x.__1.F = !!x.__1.F.y x.__1.F.y\n" + 
//						"x = x.__0.F x.__1.F"),
//				c("func F[x]{return x;} func G[x]{return F[x];} export x = G[\"a\":\"\"] ;","x = 'a'\n"),
//				c("func F[x]{return x;} func G[x]{return F[x] F[x];} export x = G[\"a\":\"\"] ;","x = 'aa'\n"),
//				c("func F[x]{return x x;} func G[x]{return F[x] F[x];} export x = G[\"a\":\"\"] ;","x = 'aaaa'\n"),
//				c("func F[x]{return x x;} func G[x]{return F[x] F[x \",\"];} export x = G[\"a\":\"\"] ;","x = 'aaa,a,'\n"),
//				c("func F[x]{return x x;} func G[x]{return F[x : \"0\"] F[x \",\"];} export x = G[\"a\":\"\"] ;","x = 'aaa,a,':'00'\n"),
//				c("export kBytes = Optimize[\n" + 
//						"  \"[1]\" |   \"[2]\" |   \"[3]\" |   \"[4]\" |   \"[5]\" |   \"[6]\" |   \"[7]\" |   \"[8]\" |   \"[9]\" |  \"[10]\" |\n" + 
//						" \"[11]\" |  \"[12]\" |  \"[13]\" |  \"[14]\" |  \"[15]\" |  \"[16]\" |  \"[17]\" |  \"[18]\" |  \"[19]\" |  \"[20]\" |\n" + 
//						" \"[21]\" |  \"[22]\" |  \"[23]\" |  \"[24]\" |  \"[25]\" |  \"[26]\" |  \"[27]\" |  \"[28]\" |  \"[29]\" |  \"[30]\" |\n" + 
//						" \"[31]\" |  \"[32]\" |  \"[33]\" |  \"[34]\" |  \"[35]\" |  \"[36]\" |  \"[37]\" |  \"[38]\" |  \"[39]\" |  \"[40]\" |\n" + 
//						" \"[41]\" |  \"[42]\" |  \"[43]\" |  \"[44]\" |  \"[45]\" |  \"[46]\" |  \"[47]\" |  \"[48]\" |  \"[49]\" |  \"[50]\" |\n" + 
//						" \"[51]\" |  \"[52]\" |  \"[53]\" |  \"[54]\" |  \"[55]\" |  \"[56]\" |  \"[57]\" |  \"[58]\" |  \"[59]\" |  \"[60]\" |\n" + 
//						" \"[61]\" |  \"[62]\" |  \"[63]\" |  \"[64]\" |  \"[65]\" |  \"[66]\" |  \"[67]\" |  \"[68]\" |  \"[69]\" |  \"[70]\" |\n" + 
//						" \"[71]\" |  \"[72]\" |  \"[73]\" |  \"[74]\" |  \"[75]\" |  \"[76]\" |  \"[77]\" |  \"[78]\" |  \"[79]\" |  \"[80]\" |\n" + 
//						" \"[81]\" |  \"[82]\" |  \"[83]\" |  \"[84]\" |  \"[85]\" |  \"[86]\" |  \"[87]\" |  \"[88]\" |  \"[89]\" |  \"[90]\" |\n" + 
//						" \"[91]\" |  \"[92]\" |  \"[93]\" |  \"[94]\" |  \"[95]\" |  \"[96]\" |  \"[97]\" |  \"[98]\" |  \"[99]\" | \"[100]\" |\n" + 
//						"\"[101]\" | \"[102]\" | \"[103]\" | \"[104]\" | \"[105]\" | \"[106]\" | \"[107]\" | \"[108]\" | \"[109]\" | \"[110]\" |\n" + 
//						"\"[111]\" | \"[112]\" | \"[113]\" | \"[114]\" | \"[115]\" | \"[116]\" | \"[117]\" | \"[118]\" | \"[119]\" | \"[120]\" |\n" + 
//						"\"[121]\" | \"[122]\" | \"[123]\" | \"[124]\" | \"[125]\" | \"[126]\" | \"[127]\" | \"[128]\" | \"[129]\" | \"[130]\" |\n" + 
//						"\"[131]\" | \"[132]\" | \"[133]\" | \"[134]\" | \"[135]\" | \"[136]\" | \"[137]\" | \"[138]\" | \"[139]\" | \"[140]\" |\n" + 
//						"\"[141]\" | \"[142]\" | \"[143]\" | \"[144]\" | \"[145]\" | \"[146]\" | \"[147]\" | \"[148]\" | \"[149]\" | \"[150]\" |\n" + 
//						"\"[151]\" | \"[152]\" | \"[153]\" | \"[154]\" | \"[155]\" | \"[156]\" | \"[157]\" | \"[158]\" | \"[159]\" | \"[160]\" |\n" + 
//						"\"[161]\" | \"[162]\" | \"[163]\" | \"[164]\" | \"[165]\" | \"[166]\" | \"[167]\" | \"[168]\" | \"[169]\" | \"[170]\" |\n" + 
//						"\"[171]\" | \"[172]\" | \"[173]\" | \"[174]\" | \"[175]\" | \"[176]\" | \"[177]\" | \"[178]\" | \"[179]\" | \"[180]\" |\n" + 
//						"\"[181]\" | \"[182]\" | \"[183]\" | \"[184]\" | \"[185]\" | \"[186]\" | \"[187]\" | \"[188]\" | \"[189]\" | \"[190]\" |\n" + 
//						"\"[191]\" | \"[192]\" | \"[193]\" | \"[194]\" | \"[195]\" | \"[196]\" | \"[197]\" | \"[198]\" | \"[199]\" | \"[200]\" |\n" + 
//						"\"[201]\" | \"[202]\" | \"[203]\" | \"[204]\" | \"[205]\" | \"[206]\" | \"[207]\" | \"[208]\" | \"[209]\" | \"[210]\" |\n" + 
//						"\"[211]\" | \"[212]\" | \"[213]\" | \"[214]\" | \"[215]\" | \"[216]\" | \"[217]\" | \"[218]\" | \"[219]\" | \"[220]\" |\n" + 
//						"\"[221]\" | \"[222]\" | \"[223]\" | \"[224]\" | \"[225]\" | \"[226]\" | \"[227]\" | \"[228]\" | \"[229]\" | \"[230]\" |\n" + 
//						"\"[231]\" | \"[232]\" | \"[233]\" | \"[234]\" | \"[235]\" | \"[236]\" | \"[237]\" | \"[238]\" | \"[239]\" | \"[240]\" |\n" + 
//						"\"[241]\" | \"[242]\" | \"[243]\" | \"[244]\" | \"[245]\" | \"[246]\" | \"[247]\" | \"[248]\" | \"[249]\" | \"[250]\" |\n" + 
//						"\"[251]\" | \"[252]\" | \"[253]\" | \"[254]\" | \"[255]\"\n" + 
//						"];","kBytes = .")
//				
//		};
//		try {
//			final OptimisedHashLexTransducer specs = new OptimisedHashLexTransducer();
//			for (Case c : sources) {
//				try {
//					ThraxParser<?, ?> parser = ThraxParser.parse(new File("."),CharStreams.fromString(c.thrax), specs.specs);
//					assertEquals(c.solomonoff, parser.toSolomonoff().trim());
//				} catch (Throwable e) {
//					e.printStackTrace();
//					System.out.println(c.thrax+"\n"+e);
//					throw e;
//				}
//			}
//		} catch (CompilationError e1) {
//			e1.printStackTrace();
//		}
//	}
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
