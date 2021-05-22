use ghost::Ghost;
use int_seq::IntSeq;
lalrpop_mod!(pub solomonoff); // synthesized by LALRPOP
use regular_operations;
use g::G;
use parser_state::ParserState;
use compilation_error::CompErr;
use lalrpop_util::ParseError;
use lalrpop_util::lexer::Token;

pub struct Solomonoff {
    parser: solomonoff::FuncsParser,
    state: ParserState,
}

impl Solomonoff {
    pub fn new() -> Self {
        Self { parser: solomonoff::FuncsParser::new(), state: ParserState::new() }
    }

    pub fn parse<'input>(&mut self, s: &'input str, ghost: &Ghost) -> Result<(), ParseError<usize, Token<'input>, CompErr>> {
        self.parser.parse(ghost, &mut self.state, s)
    }

    pub fn get(&self, name: &String) -> Option<&G> {
        self.state.borrow_variable(name).map(|(_, _, g)| g)
    }

    pub fn delete_all(&mut self,ghost:&Ghost){
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
            Test { code: String::from(c) , accepted_inputs: i, rejected_inputs: r }
        }
        let cases: Vec<Test<'static>> = vec![
            t("'aa'", vec!["aa;"], vec!["a"]),
            t("'aa'\n\n", vec!["aa;"], vec!["a"]),
            t("'aa' // 'bb'", vec!["aa;"], vec!["a"]),
            t("'aa' 'bb'", vec!["aabb;"], vec!["aa",""]),
            t("'aa' \\\n 'bb'", vec!["aabb;"], vec!["aa",""]),
            t("'aa' /* 'cc' */ 'bb'", vec!["aabb;"], vec!["aa",""]),
            t("'aa' | 'bb'", vec!["aa;","bb;"], vec!["a","","b"]),
            t("('aa' | 'bb')*", vec!["aa;","bb;",";","aabbaa;"], vec!["a","aba","b"]),
            t("('aa':'yy' | 'bb':'xx')*", vec!["aa;yy","bb;xx",";","aabbaa;yyxxyy"], vec!["a","aba","b"]),
            t("('aa':'yy' | 'bb':'xx')+", vec!["aa;yy","bb;xx","aabbaa;yyxxyy"], vec!["a","","aba","b"]),
            t("('aa':'yy' | 'bb':'xx')?", vec!["aa;yy","bb;xx",";"], vec!["a","aba","b","aabbaa"]),
            t(":'xx'", vec![";xx"], vec!["a","aba","b","aabbaa"]),
            t(":'xx' :'yy'", vec![";xxyy"], vec!["a","aba","b","aabbaa"]),
            a("g = :'xx' :'yy' \n f = 'aa' g", vec!["aa;xxyy"], vec!["a","aba","b","aabbaa"]),
            a("\n g = :'xx' :'yy' \n\n f = 'aa' !!g g", vec!["aa;xxyyxxyy"], vec!["a","aba","b","aabbaa"]),
            a("!!g = :'xx' :'yy' \n \r \n f = 'aa' g g", vec!["aa;xxyyxxyy"], vec!["a","aba","b","aabbaa"]),
            t("'a':'b' 1 |'a':'c' 2", vec!["a;c"], vec!["aa",""]),
            t("'a':'c' 2 | 'a':'b' 1 ", vec!["a;c"], vec!["aa",""]),
            t("'a':'y' 0 | 'a':'c' 2 | 'a':'b' 1 ", vec!["a;c"], vec!["aa",""]),
            t("3 'a':'c' 2 | 10 'a':'b' 1 ", vec!["a;c"], vec!["aa",""]),
            t("'a':'b' 1 |'a':'c' -2", vec!["a;b"], vec!["aa",""]),
        ];
        let mut output_buffer = Vec::<A>::with_capacity(256);
        unsafe { output_buffer.set_len(256) };
        for test in cases {
            Ghost::with_mock(|ghost| {
                let mut sol = Solomonoff::new();
                println!("Testing {}",test.code);
                sol.parse(test.code.as_str(),ghost).unwrap();
                let g = sol.get(&String::from("f")).unwrap();
                let r = g.optimise_graph(ghost);
                let mut state_to_index = r.make_state_to_index_table();
                for input in test.accepted_inputs{
                    let (input, output) = input.split_once(';').unwrap();
                    let y = r.evaluate_tabular(&mut state_to_index, output_buffer.as_mut_slice(), &IntSeq::from(input));
                    assert!(y.is_some());
                    let y: String = unsafe { y.unwrap().iter().map(|&x| char::from_u32_unchecked(x)).collect() };
                    assert_eq!(y,String::from(output));
                }
                for input in test.rejected_inputs{
                    let y = r.evaluate_tabular(&mut state_to_index, output_buffer.as_mut_slice(), &IntSeq::from(input));
                    assert!(y.is_none());
                }
                sol.delete_all(ghost)
            });
        }
    }


}