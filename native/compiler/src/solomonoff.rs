use ghost::Ghost;
use int_seq::IntSeq;
lalrpop_mod!(pub solomonoff_parser); // synthesized by LALRPOP
use regular_operations;
use g::G;
use parser_state::ParserState;
use compilation_error::CompErr;
use lalrpop_util::ParseError;
use lalrpop_util::lexer::Token;
use logger::Logger;
use pipeline::Pipeline;
use std::fs::File;
use std::io::BufReader;

pub struct Solomonoff<L:Logger> {
    parser: solomonoff_parser::FuncsParser,
    state: ParserState<L>,
}

impl <L:Logger> Solomonoff<L> {
    pub fn state(&self)->&ParserState<L>{
        &self.state
    }
    pub fn state_mut(&mut self)->&mut ParserState<L>{
        &mut self.state
    }
    pub fn new(ghost:&Ghost) -> Self {
        Self {
            parser: solomonoff_parser::FuncsParser::new(),
            state: ParserState::new_with_standard_library(ghost),
        }
    }

    pub fn parse<'input>(&mut self, logger:&mut L, s: &'input str, ghost: &Ghost) -> Result<(), ParseError<usize, Token<'input>, CompErr>> {
        self.parser.parse(ghost, logger, &mut self.state, s)
    }

    pub fn get_transducer(&self, name: &String) -> Option<&G> {
        self.state.borrow_variable(name).map(|(_, _, g)| g)
    }

    pub fn get_pipeline(&self, name: &String) -> Option<&Pipeline> {
        self.state.borrow_pipeline(name)
    }

    pub fn delete_all(&mut self, ghost: &Ghost) {
        self.state.delete_all(ghost)
    }
}


#[cfg(test)]
mod tests {
    // Note this useful idiom: importing names from outer (for mod tests) scope.
    use super::*;
    use int_seq::A;
    use compilation_error::CompErr;
    use parser_state::ParserState;
    use ranged_graph::RangedGraph;
    use logger::StdoutLogger;

    #[test]
    fn test_1() {
        struct Test<'a> {
            code: String,
            accepted_inputs: Vec<&'a str>,
            rejected_inputs: Vec<&'a str>,
        }
        fn t<'a>(c: &'a str, i: Vec<&'a str>, r: Vec<&'a str>) -> Test<'a> {
            Test { code: String::from("f = ") + c, accepted_inputs: i, rejected_inputs: r }
        }
        fn a<'a>(c: &'a str, i: Vec<&'a str>, r: Vec<&'a str>) -> Test<'a> {
            Test { code: String::from(c), accepted_inputs: i, rejected_inputs: r }
        }
        let cases: Vec<Test<'static>> = vec![
            t("'aa'", vec!["aa;"], vec!["a"]),
            t("'aa'\n\n", vec!["aa;"], vec!["a"]),
            t("'aa' // 'bb'", vec!["aa;"], vec!["a"]),
            t("'aa' 'bb'", vec!["aabb;"], vec!["aa", ""]),
            t("'aa' \n 'bb'", vec!["aabb;"], vec!["aa", ""]),
            t("'aa' /* 'cc' */ 'bb'", vec!["aabb;"], vec!["aa", ""]),
            t("'aa' | 'bb'", vec!["aa;", "bb;"], vec!["a", "", "b"]),
            t("('aa' | 'bb')*", vec!["aa;", "bb;", ";", "aabbaa;"], vec!["a", "aba", "b"]),
            t("('aa':'yy' | 'bb':'xx')*", vec!["aa;yy", "bb;xx", ";", "aabbaa;yyxxyy"], vec!["a", "aba", "b"]),
            t("('aa':'yy' | 'bb':'xx')+", vec!["aa;yy", "bb;xx", "aabbaa;yyxxyy"], vec!["a", "", "aba", "b"]),
            t("('aa':'yy' | 'bb':'xx')?", vec!["aa;yy", "bb;xx", ";"], vec!["a", "aba", "b", "aabbaa"]),
            t(":'xx'", vec![";xx"], vec!["a", "aba", "b", "aabbaa"]),
            t(":'xx' :'yy'", vec![";xxyy"], vec!["a", "aba", "b", "aabbaa"]),
            a("g = :'xx' :'yy' f = 'aa' g", vec!["aa;xxyy"], vec!["a", "aba", "b", "aabbaa"]),
            a("nonfunc f = 'a':'b' | 'a':'c'",vec![],vec![]),
            a("\n g = :'xx' :'yy' \n\n f = 'aa' !!g g", vec!["aa;xxyyxxyy"], vec!["a", "aba", "b", "aabbaa"]),
            a("!!g = :'xx' :'yy' \n \r \n f = 'aa' g g", vec!["aa;xxyyxxyy"], vec!["a", "aba", "b", "aabbaa"]),
            t("'a':'b' 1 |'a':'c' 2", vec!["a;c"], vec!["aa", ""]),
            t("'a':'c' 2 | 'a':'b' 1 ", vec!["a;c"], vec!["aa", ""]),
            t("'a':'y' 0 | 'a':'c' 2 | 'a':'b' 1 ", vec!["a;c"], vec!["aa", ""]),
            t("3 'a':'c' 2 | 10 'a':'b' 1 ", vec!["a;c"], vec!["aa", ""]),
            t("'a':'b' 1 |'a':'c' -2", vec!["a;b"], vec!["aa", ""]),
            t("('a':'b') 1* ", vec!["a;b", "aa;bb", ";"], vec!["aba", "b"]),
            t("('a':'x' ('a':'b') 1*)* ", vec!["aa;xb", "a;x", ";", "aaaa;xbbb"], vec!["aba", "b"]),
            t("('a':'x' ('a':'b') -1 *)* ", vec!["aa;xx", "a;x", ";", "aaaa;xxxx"], vec!["aba", "b"]),
            t("[abc]", vec!["a;", "b;", "c;"], vec!["aba", "", "ab", "d", "`"]),
            t("[a-c]", vec!["a;", "b;", "c;"], vec!["aba", "", "ab", "d", "`"]),
            t(":'\\0' 'a'", vec!["a;a"], vec!["aba", "", "ab"]),
            t("(:'\\0' [a-z ]|'xx':'010' 2)*", vec!["a;a", "b;b", "c;c", ";", "xx;010", "hello;hello", "helxxlo;hel010lo", "xxxx;010010"], vec!["0", "1", "[", "'", "\"", "-"]),
            t("<97>", vec!["a;"], vec!["aba", "", "ab"]),
            t("<97 98>", vec!["ab;"], vec!["aba", "", "a"]),
            t("<97 98 99>", vec!["abc;"], vec!["ab", "", "a"]),
            t("<[97 98 99]>", vec!["a;", "b;", "c;"], vec!["aba", "", "ab", "d", "`"]),
            t("<[97-99]>", vec!["a;", "b;", "c;"], vec!["aba", "", "ab", "d", "`"]),
            t("ostiaCompress!('a':'b','aa':'a','ab':'b','ba':'a','bb':'b')", vec!["a;b", "aa;a", "ab;b", "bb;b", "ba;a"], vec!["aba", "", "abb", "d", "b", "`"]),
            t("activeLearningFromDataset!('resources/test/sample.txt')", vec!["a;b", "aa;a", "ab;b", "bb;b", "ba;a"], vec!["aba", "", "abb", "d", "b", "`"]),
            t("activeLearningFromDataset!('resources/test/sample.py')", vec!["a;b", "aa;a", "ab;b", "bb;b", "ba;a"], vec!["aba", "", "abb", "d", "b", "`"]),
            t("activeLearningFromDataset!('resources/test/sample.sh')", vec!["a;b", "aa;a", "ab;b", "bb;b", "ba;a"], vec!["aba", "", "abb", "d", "b", "`"]),
        ];
        for test in cases {
            Ghost::with_mock(|ghost| {
                let mut sol = Solomonoff::new(ghost);
                println!("Testing {}", test.code);
                sol.parse(&mut StdoutLogger::new(), test.code.as_str(), ghost).unwrap();
                let g = sol.get_transducer(&String::from("f")).unwrap();
                let r = g.optimise_graph(ghost);
                for input in test.accepted_inputs {
                    let (input, output) = input.split_once(';').unwrap();
                    let y = r.evaluate_to_string(input.chars());
                    assert!(y.is_some());
                    let y: String = y.unwrap();
                    assert_eq!(y, String::from(output));
                }
                for input in test.rejected_inputs {
                    let y = r.evaluate_to_string(input.chars());
                    assert!(y.is_none());
                }
                sol.delete_all(ghost)
            });
        }
    }
}