use std::io::{BufReader, BufRead};
use int_embedding::Alphabet;
use int_seq::IntSeq;
use std::collections::HashSet;
use std::borrow::{Borrow, BorrowMut};
use int_queue::IntQueue;
use int_queue::IntQueue::End;
use seq::Seq;

pub struct Edge {
    output: Seq<u8>,
    target: Option<Box<State>>,
}

impl Edge {
    fn new(output: Seq<u8>) -> Self {
        Self { output, target: None }
    }
    fn empty() -> Self {
        Self::new(Seq::empty())
    }
}

struct State {
    output: Seq<u8>,
    transitions: Vec<Option<Edge>>,
}


// fn insert_ptt_positive<A>(root: &mut State, input: &IntSeq, output: &IntSeq, alphabet: &A) where A: Alphabet {
//     let mut offset = 0;
//     let mut ptt_iter = root;
//     for c in input.iter().map(|c|alphabet.embed(c)) {
//         let edge = &mut ptt_iter.transitions[c as usize];
//         if let Some(edge) = edge {
//             let end_of_common_prefix = edge.output.lcp(&output.as_bytes()[offset..]);
//         } else {
//             *edge = Some(Edge::new(output));
//         }
//     }
// }


fn build_ptt<I, A>(informant: I, alphabet: &A) where I: Iterator<Item=(String, String)>,
                                                     A: Alphabet {
    // let mut root = State{output:0};
}
