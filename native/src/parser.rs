use ghost::Ghost;
use int_seq::IntSeq;
lalrpop_mod!(pub solomonoff); // synthesized by LALRPOP
use regular_operations;
use g::G;
use parser_state::ParserState;
use compilation_error::CompErr;
use lalrpop_util::ParseError;
use lalrpop_util::lexer::Token;

pub struct Solomonoff{
    parser:solomonoff::FuncsParser,
    state:ParserState
}

impl Solomonoff{
    fn new()->Self{
        Self{parser:solomonoff::FuncsParser::new(),state:ParserState::new()}
    }

    fn parse<'input>(&mut self, s:&'input str, ghost:&Ghost)->Result<(),ParseError<usize, Token<'input>, CompErr>>{
        self.parser.parse(ghost,&mut self.state,s)
    }
}


#[cfg(test)]
mod tests {
    // Note this useful idiom: importing names from outer (for mod tests) scope.
    use super::*;
    use ranged_optimisation::optimise_graph;
    use int_seq::A;
    use compilation_error::CompErr;
    use parser_state::ParserState;

    #[test]
    fn test_1() {
        Ghost::with_mock(|ghost| {

            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,&mut ParserState::new(),"'aa'").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("a"));
            assert!(y.is_none());
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aa"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from(""));
            g.delete(ghost)
        });
    }

    #[test]
    fn test_2() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,&mut ParserState::new(),"'aa'|'bc'").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("a"));
            assert!(y.is_none());
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aa"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from(""));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("bc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from(""));

            g.delete(ghost)
        });
    }


    #[test]
    fn test_3() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,&mut ParserState::new(),"'aa' 'bc' :'de'").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aa"));
            assert!(y.is_none());
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aabc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("de"));
            g.delete(ghost)
        });
    }


    #[test]
    fn test_4() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,&mut ParserState::new(),"'aa':'xx' | 'bc' :'de'").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aa"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("xx"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("bc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("de"));
            g.delete(ghost)
        });
    }

    #[test]
    fn test_5() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,&mut ParserState::new(),"('aa':'xx')+ | ('bc' :'de')*").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aaaa"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("xxxx"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("bcbcbc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("dedede"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from(""));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from(""));

            g.delete(ghost)
        });
    }

    #[test]
    fn test_6() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,&mut ParserState::new(),"[bcd]:'re'").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("a"));
            assert!(y.is_none());

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("b"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("re"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("c"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("re"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("d"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("re"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("e"));
            assert!(y.is_none());

            g.delete(ghost)
        });
    }

    #[test]
    fn test_7() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,&mut ParserState::new(),"[b-d]:'re'").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("a"));
            assert!(y.is_none());

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("b"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("re"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("c"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("re"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("d"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("re"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("e"));
            assert!(y.is_none());

            g.delete(ghost)
        });
    }
}