use ghost::Ghost;
use int_seq::{IntSeq, A};
use g::G;
use parser_state::ParserState;
use compilation_error::CompErr;
use lalrpop_util::ParseError;
use lalrpop_util::lexer::Token;
use repl_command::{ReplCommand, REPL_EVAL, REPL_UNSET, REPL_UNSET_ALL, REPL_PRINT_HELP, REPL_LS};
use std::collections::HashMap;
use parser_utils::pe;
use solomonoff::Solomonoff;
use solomonoff::solomonoff_parser;
use std::collections::hash_map::Iter;

pub struct Repl {
    solomonoff: Solomonoff,
    repl: solomonoff_parser::StatementParser,
    repl_cmd: solomonoff_parser::ReplCmdParser,
    cmds: HashMap<String, ReplCommand>,
}

impl Repl {

    pub fn eval<I>(&mut self, ghost:&Ghost, name:&String, input:I)->Result<Option<IntSeq>,()>
        where I: DoubleEndedIterator<Item=A> + ExactSizeIterator + Clone{
        if self.solomonoff.state_mut().optimise(&name, ghost).is_none(){
            Err(())
        }else {
            let g = self.solomonoff.state().get_optimised(name).unwrap();
            Ok(g.evaluate_to_int_seq(input))
        }
    }
    pub fn get_cmd(&self, name:&String) -> Option<&ReplCommand> {
        self.cmds.get(name)
    }
    pub fn iter_cmds(&self) -> Iter<'_, String, ReplCommand> {
        self.cmds.iter()
    }
    pub fn new() -> Self {
        Self::from(Solomonoff::new())
    }

    pub fn new_with_standard_commands() -> Self {
        let mut me = Self::new();
        me.attach_standard_commands();
        me
    }

    pub fn attach_standard_commands(&mut self) {
        self.cmds.insert(String::from("?"), REPL_PRINT_HELP);
        self.cmds.insert(String::from("eval"), REPL_EVAL);
        self.cmds.insert(String::from("ls"), REPL_LS);
        self.cmds.insert(String::from("unset"), REPL_UNSET);
        self.cmds.insert(String::from("unset_all"), REPL_UNSET_ALL);
    }

    pub fn from(solomonoff:Solomonoff) -> Self {
        Self {
            solomonoff,
            repl: solomonoff_parser::StatementParser::new(),
            repl_cmd: solomonoff_parser::ReplCmdParser::new(),
            cmds: HashMap::new(),
        }
    }

    pub fn parse<'input>(&mut self, s: &'input str, ghost: &Ghost) -> Result<(), ParseError<usize, Token<'input>, CompErr>> {
        self.solomonoff.parse(s, ghost)
    }

    pub fn repl<'input>(&mut self, s: &'input str, ghost: &Ghost) -> Result<Option<String>, ParseError<usize, Token<'input>, CompErr>> {
        if s.starts_with("/") {
            let (cmd, args) = self.repl_cmd.parse(ghost, self.solomonoff.state_mut(), s)?;
            if let Some(cmd) = self.cmds.get(&cmd) {
                pe((cmd.f)(args, self, ghost))
            } else {
                Err(ParseError::User { error: CompErr::UnrecognisedCommand(cmd) })
            }
        } else {
            self.solomonoff.parse( s, ghost).map(|()|None)
        }
    }

    pub fn state(&self)->&ParserState{
        self.solomonoff.state()
    }
    pub fn state_mut(&mut self)->&mut ParserState{
        self.solomonoff.state_mut()
    }
    pub fn get_optimised(&mut self)->&mut ParserState{
        self.solomonoff.state_mut()
    }

    pub fn get(&self, name: &String) -> Option<&G> {
        self.solomonoff.get(name)
    }

    pub fn delete_all(&mut self, ghost: &Ghost) {
        self.solomonoff.delete_all(ghost)
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
        let mut output_buffer = Vec::<A>::with_capacity(256);
        unsafe { output_buffer.set_len(256) };
        for test in cases {
            Ghost::with_mock(|ghost| {
                let mut sol = Solomonoff::new();
                println!("Testing {}", test.code);
                sol.parse(test.code.as_str(), ghost).unwrap();
                let g = sol.get(&String::from("f")).unwrap();
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