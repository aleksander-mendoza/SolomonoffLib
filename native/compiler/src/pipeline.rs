use ranged_graph::{RangedGraph, Transition};
use v::V;
use ranged_evaluation::StateToIndexTable;
use int_seq::{A, IntSeq};
use compilation_error::CompErr;
use std::alloc::Global;
use std::rc::Rc;


#[derive(Clone)]
enum PipelineNode {
    Automaton(Rc<RangedGraph<Transition>>, V),
    External(Rc<dyn Fn(&Vec<A>, &mut Vec<A>)->Result<bool,CompErr>>, V),
    Alternative(Pipeline, Pipeline),
}
#[derive(Clone)]
pub struct Pipeline {
    pipes: Vec<PipelineNode>,
    max_states: usize,
}


impl Pipeline {
    pub fn new(r: RangedGraph<Transition>, pos: V) -> Self {
        let max_states = r.len();
        Self { pipes: vec![PipelineNode::Automaton(Rc::new(r), pos)], max_states  }
    }

    pub fn external(f:Rc<dyn Fn(&Vec<A>, &mut Vec<A>)->Result<bool,CompErr>>, pos: V) -> Self {
        Self { pipes: vec![PipelineNode::External(f, pos)], max_states:0  }
    }

    pub fn alternative(self, rhs: Self) -> Self {
        let max_states = self.max_states.max(rhs.max_states);
        Self { pipes: vec![PipelineNode::Alternative(self, rhs)], max_states }
    }

    pub fn composition(mut self, mut rhs: Self) -> Self {
        self.max_states = self.max_states.max(rhs.max_states);
        self.pipes.append(&mut rhs.pipes);
        self
    }
    pub fn max_states(&self)->usize{
        self.max_states
    }
    pub fn make_state_to_index_table(&self) -> StateToIndexTable {
        StateToIndexTable::new(self.max_states())
    }
    pub fn ensure_length(&self, state_to_index: &mut StateToIndexTable) {
        state_to_index.ensure_length(self.max_states())
    }
    pub fn evaluate_to_string<I>(&self, input: I) -> Option<String>
        where I: DoubleEndedIterator<Item=A> + Clone {
        let mut state_to_index = self.make_state_to_index_table();
        self.evaluate_tabular(&mut state_to_index, input).map(|v| {
            let s: String = v.into_iter().rev().collect();
            s
        })
    }
    pub fn evaluate_to_int_seq<I>(&self, input: I) -> Option<IntSeq>
        where I: DoubleEndedIterator<Item=A> + Clone {
        let mut state_to_index = self.make_state_to_index_table();
        self.evaluate_tabular(&mut state_to_index, input).map(|v| IntSeq::from_iter(v.into_iter().rev()))
    }

    pub fn evaluate_tabular<I>(&self, state_to_index: &mut StateToIndexTable, input: I) -> Option<Vec<A>>
        where  I: DoubleEndedIterator<Item=A> + Clone{
        assert!(state_to_index.len()>=self.max_states);
        let mut output_buffer = Vec::<A>::with_capacity(512);
        let mut input_buffer = Vec::<A>::with_capacity(512);
        input_buffer.extend(input.rev());
        if self.evaluate_with_buffer(state_to_index,&mut output_buffer,&mut input_buffer){
            Some(output_buffer)
        }else{
            None
        }
    }
    /**The input provided in input_buffer should be reversed beforehand!*/
    pub fn evaluate_with_buffer(&self, state_to_index: &mut StateToIndexTable, output_buffer: &mut Vec<A>, input_buffer: &mut Vec<A>) -> bool {
        for node in &self.pipes {
            match node {
                PipelineNode::Automaton(r, _) => {
                    assert!(output_buffer.is_empty());
                    if !r.evaluate_with_buffer(state_to_index, output_buffer, input_buffer.iter().cloned().rev()) {
                        return false;
                    }
                }
                PipelineNode::Alternative(lhs, rhs) => {
                    let mut tmp_copy = input_buffer.clone();
                    assert!(output_buffer.is_empty());
                    if !lhs.evaluate_with_buffer(state_to_index, output_buffer, input_buffer){
                        input_buffer.clear();
                        input_buffer.append(&mut tmp_copy);
                        assert!(output_buffer.is_empty());
                        if !rhs.evaluate_with_buffer(state_to_index, output_buffer, input_buffer) {
                            return false;
                        }
                    }
                }
                PipelineNode::External(f,pos) => {
                    match f(input_buffer,output_buffer){
                        Ok(false) => return false,
                        Ok(true) => {},
                        Err(e) => {
                            eprintln!("{:?}",e);
                            return false;
                        }
                    }
                }
            }
            std::mem::swap(input_buffer, output_buffer);
            output_buffer.clear();
        }
        std::mem::swap(input_buffer, output_buffer);
        true
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
    use ghost::Ghost;
    use solomonoff::Solomonoff;

    #[test]
    fn test_1() {
        struct Test<'a> {
            code: String,
            accepted_inputs: Vec<&'a str>,
            rejected_inputs: Vec<&'a str>,
        }
        fn t<'a>(c: &'a str, i: Vec<&'a str>, r: Vec<&'a str>) -> Test<'a> {
            Test { code: String::from("@f = ") + c, accepted_inputs: i, rejected_inputs: r }
        }
        fn a<'a>(c: &'a str, i: Vec<&'a str>, r: Vec<&'a str>) -> Test<'a> {
            Test { code: String::from(c), accepted_inputs: i, rejected_inputs: r }
        }
        let cases: Vec<Test<'static>> = vec![
            t("'aa'", vec!["aa;"], vec!["a"]),
            a("@f = 'a':'b' ; 'b':'c'", vec!["a;c"], vec!["aa",""]),
            a("@f = 'a':'b' ; 'b':'c' ; 'c':'d'", vec!["a;d"], vec!["aa",""]),
            a("@a = 'a':'b' @b = 'b':'c' @c = 'c':'d' @f = @a ; @b ; @c", vec!["a;d"], vec!["aa",""]),
            a("@a = 'a':'b' @b = 'b':'c' @c = 'c':'d' @f = @(@a ; @b) ; @c", vec!["a;d"], vec!["aa",""]),
            a("@a = 'a':'b' @b = 'b':'c' @c = 'c':'d' @f = @a ; @(@b ; @c)", vec!["a;d"], vec!["aa",""]),
            a("@a = 'a':'b' @b = 'b':'c' @c = 'c':'d' @d = 'd':'e' @f = @a ; @b ; @c || @d", vec!["a;d","d;e"], vec!["aa","","b","c","e"]),
        ];
        for test in cases {
            Ghost::with_mock(|ghost| {
                let mut sol = Solomonoff::new();
                println!("Testing {}", test.code);
                sol.parse(&mut StdoutLogger::new(), test.code.as_str(), ghost).unwrap();
                let g = sol.get_pipeline(&String::from("f")).unwrap();
                for input in test.accepted_inputs {
                    let (input, output) = input.split_once(';').unwrap();
                    let y = g.evaluate_to_string(input.chars());
                    assert!(y.is_some());
                    let y: String = y.unwrap();
                    assert_eq!(y, String::from(output));
                }
                for input in test.rejected_inputs {
                    let y = g.evaluate_to_string(input.chars());
                    assert!(y.is_none());
                }
                sol.delete_all(ghost)
            });
        }
    }
}