use ghost::Ghost;
use int_seq::{IntSeq, A};
use g::G;
use parser_state::ParserState;
use compilation_error::CompErr;
use lalrpop_util::ParseError;
use lalrpop_util::lexer::Token;
use repl_command::{ReplCommand, repl_print_help, repl_eval, repl_ls, repl_unset, repl_unset_all, repl_funcs};
use std::collections::HashMap;
use parser_utils::pe;
use solomonoff::Solomonoff;
use solomonoff::solomonoff_parser;
use std::collections::hash_map::Iter;
use logger::Logger;
use std::time::SystemTime;


pub struct Repl<L: Logger> {
    solomonoff: Solomonoff<L>,
    repl: solomonoff_parser::StatementParser,
    repl_cmd: solomonoff_parser::ReplCmdParser,
    cmds: HashMap<String, ReplCommand<L>>,
}

impl<L: Logger> Repl<L> {
    pub fn eval<I>(&mut self, ghost: &Ghost, name: &String, input: I) -> Result<Option<IntSeq>, ()>
        where I: DoubleEndedIterator<Item=A> + ExactSizeIterator + Clone {
        if self.solomonoff.state_mut().optimise(&name, ghost).is_none() {
            Err(())
        } else {
            let g = self.solomonoff.state().get_optimised(name).unwrap();
            Ok(g.evaluate_to_int_seq(input))
        }
    }
    pub fn get_cmd(&self, name: &String) -> Option<&ReplCommand<L>> {
        self.cmds.get(name)
    }
    pub fn iter_cmds(&self) -> Iter<'_, String, ReplCommand<L>> {
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
        self.cmds.insert(String::from("?"), repl_print_help());
        self.cmds.insert(String::from("eval"), repl_eval());
        self.cmds.insert(String::from("ls"), repl_ls());
        self.cmds.insert(String::from("unset"), repl_unset());
        self.cmds.insert(String::from("unset_all"), repl_unset_all());
        self.cmds.insert(String::from("funcs"), repl_funcs());
    }

    pub fn attach_cmd(&mut self, name:String, cmd:ReplCommand<L>) -> Option<ReplCommand<L>> {
        self.cmds.insert(name, cmd)
    }

    pub fn from(solomonoff: Solomonoff<L>) -> Self {
        Self {
            solomonoff,
            repl: solomonoff_parser::StatementParser::new(),
            repl_cmd: solomonoff_parser::ReplCmdParser::new(),
            cmds: HashMap::new(),
        }
    }

    pub fn parse<'input>(&mut self, logger: &mut L, s: &'input str, ghost: &Ghost) -> Result<(), ParseError<usize, Token<'input>, CompErr>> {
        self.solomonoff.parse(logger, s, ghost)
    }

    pub fn repl<'input>(&mut self, log: &mut L, debug: &mut L, s: &'input str, ghost: &Ghost) -> Result<Option<String>, ParseError<usize, Token<'input>, CompErr>> {
        let now = SystemTime::now();
        let out = if s.starts_with("/") {
            if s.len() == 1 || (s.as_bytes()[1] as char).is_ascii_whitespace(){
                self.solomonoff.parse(debug, &s[1..], ghost).map(|()| None)
            }else {
                let (cmd, args) = self.repl_cmd.parse(ghost, debug, self.solomonoff.state_mut(), s)?;
                if let Some(cmd) = self.cmds.get(&cmd) {
                    pe((cmd.f)(log, debug, args, self, ghost))
                } else {
                    Err(ParseError::User { error: CompErr::UnrecognisedCommand(cmd) })
                }
            }
        } else {
            self.solomonoff.parse(debug, s, ghost).map(|()| None)
        };
        if let Ok(elapsed) = now.elapsed(){
            debug.println(format!("Took {} millis",elapsed.as_millis()))
        }
        out
    }

    pub fn state(&self) -> &ParserState<L> {
        self.solomonoff.state()
    }
    pub fn state_mut(&mut self) -> &mut ParserState<L> {
        self.solomonoff.state_mut()
    }
    pub fn get_optimised(&mut self) -> &mut ParserState<L> {
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
    use logger::{StdoutLogger, AccumulatingLogger};
    use regex::Regex;

    #[test]
    fn test_1() {
        struct Test {
            code: &'static str,
            out: Vec<Option<&'static str>>,
            log: &'static str,
            debug: &'static str,
        }
        fn a(code: &'static str, out: Vec<Option<&'static str>>, log: &'static str, debug: &'static str) -> Test{
            Test { code, out, log, debug }
        }
        let cases: Vec<Test> = vec![
            a("/ls", vec![Some("[]")], "", "Took [0-9]+ millis\n"),
            a("/ a = 'a' \n/ls", vec![None, Some("[a]")], "", "Took [0-9]+ millis\nTook [0-9]+ millis\n"),
            a("/ a = 'a' \n/eval a 'a'", vec![None, Some("''")], "", "Took [0-9]+ millis\nTook [0-9]+ millis\n"),
            a("/ a = 'a':'b' \n/eval a 'a'", vec![None, Some("'b'")], "", "Took [0-9]+ millis\nTook [0-9]+ millis\n"),

        ];

        for test in cases {
            Ghost::with_mock(|ghost| {
                let mut sol = Repl::new_with_standard_commands();
                println!("Testing {}", test.code);
                let mut log = AccumulatingLogger::new();
                let mut debug = AccumulatingLogger::new();
                for (i,line) in test.code.lines().enumerate(){
                    let out = sol.repl(&mut log, &mut debug, line, ghost);
                    assert!(i<test.out.len(),"{}",test.code);
                    assert!(out.is_ok(),"line=[{}]out=[{:?}]",line,out);
                    let out = out.unwrap();
                    let o = &out;
                    let o = o.as_ref().map(|e|e.as_str());
                    let exp = &test.out[i];
                    assert_eq!(&o,exp,"{}",test.code)
                }
                let log_re = Regex::new(test.log).unwrap();
                let debug_re = Regex::new(test.debug).unwrap();
                assert!(log_re.is_match(log.get()),"log={}",log.get());
                assert!(debug_re.is_match(debug.get()),"debug={}",debug.get());

                sol.delete_all(ghost)
            });
        }
    }
}