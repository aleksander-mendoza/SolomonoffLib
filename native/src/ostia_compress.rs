use std::num::NonZeroU64;
use std::io::{BufReader, BufRead};

struct Edge {
    output: u128,
    target: Box<State>,
}

struct State {
    output: u128,
    transitions: Vec<Edge>,
}

trait Alphabet {
    fn char_as_idx(c: char) -> u8;
    fn idx_as_char(idx: u8) -> char;
    fn len()->usize;
}

fn build_ptt_onward<A>(root:&mut State, input: String, output: String, alphabet: &A) where A: Alphabet {

    for c in input.chars(){
    }
}


fn build_ptt<I, A>(informant: I, alphabet: &A) where I: Iterator<Item=(String, String)>,
                                                     A: Alphabet {
    // let mut root = State{output:0};
}
