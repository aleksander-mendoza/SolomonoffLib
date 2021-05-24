use std::io::{BufReader, BufRead};
use int_embedding::Alphabet;
use int_seq::IntSeq;
use std::collections::{HashSet, VecDeque, HashMap};
use std::borrow::{Borrow, BorrowMut};
use int_queue::IntQueue;
use int_queue::IntQueue::End;
use seq::Seq;
use learn::ostia_compress::Kind::{Unknown, Accepting, Rejecting};
use std::ptr::NonNull;
use util::allocate_one;
use g::G;
use n::N;
use ghost::Ghost;
use v::V;
use e::E;
use p::P;


pub enum Link{
    Weak(NonNull<State>),
    Strong(Box<State>)
}

impl Link{
    fn get(&self)->NonNull<State>{
        match self{
            Link::Weak(w) => *w,
            Link::Strong(s) => NonNull::from(s.as_ref())
        }
    }
}


pub struct Edge {
    output: Seq<u8>,
    target: Link,
}

impl Edge {
    fn new(output: Seq<u8>, target: Box<State>) -> Self {
        Self { output, target:Link::Strong(target) }
    }
    fn create(output: Seq<u8>, alphabet_size: usize) -> Self {
        Self::new(output, Box::new(State::new(alphabet_size)))
    }
}

#[derive(Eq, PartialEq, Debug)]
pub enum Kind { Accepting(Seq<u8>), Rejecting, Unknown }

impl Kind {
    pub fn mutate<F: FnOnce(&mut Seq<u8>)>(&mut self, f: F) {
        match self {
            Accepting(s) => { f(s); }
            Kind::Rejecting => {}
            Unknown => {}
        }
    }
}

pub struct State {
    output: Kind,
    transitions: Seq<Option<Edge>>,
}

pub struct Blue(u8, NonNull<State>);


impl Blue {
    fn state(&self) -> NonNull<State> {
        self.parent().transitions[self.0 as usize].as_ref().unwrap().target.get()
    }
    fn set_target(&mut self, red_state:NonNull<State>){
        let i = self.0 as usize;
        self.parent_mut().transitions[i].as_mut().unwrap().target = Link::Weak(red_state);
    }
    fn parent(&self)->&State{
        unsafe{self.1.as_ref()}
    }
    fn parent_mut(&mut self)->&mut State{
        unsafe{self.1.as_mut()}
    }
    fn add_blue_states(parent:NonNull<State>, blue: &mut VecDeque<Blue>) {
        blue.extend(unsafe{parent.as_ref()}.transitions.iter().enumerate().filter_map(|(i, e)| e.as_ref().map(|_| Blue(i as u8, parent))))
    }
    fn add_further_blue_states(&self, blue: &mut VecDeque<Blue>) -> NonNull<State> {
        let state = self.state();
        Self::add_blue_states(state,blue);
        state
    }
}

fn lcp(edge: &mut Edge, output: &[u8]) -> usize {
    let common_prefix_len = edge.output.lcp(output);
    let suffix = &edge.output.as_slice()[common_prefix_len..];
    unsafe{edge.target.get().as_mut()}.pushback(suffix);
    edge.output.cut_off(common_prefix_len);
    common_prefix_len
}

fn develop_tree(edge: &mut Option<Edge>, output: &[u8], alphabet_size: usize) -> usize {
    if let Some(edge) = edge.as_mut() {
        lcp(edge, output)
    } else {
        *edge = Some(Edge::create(Seq::from(output), alphabet_size));
        output.len()
    }
}

impl State {
    pub fn print_tree(&self, indentation:usize){
        println!("{:?}",self.output);
        for (symbol,tr) in self.transitions.iter().enumerate(){
            for _ in 0..=indentation{
                print!("  ");
            }
            print!("{}",symbol);
            if let Some(tr) = tr{
                print!(":{} -> ",tr.output);
                unsafe{tr.target.get().as_ref()}.print_tree(indentation+1);
            }else{
                println!();
            }
        }
    }
    pub fn new(alphabet_size: usize) -> Self {
        Self { output: Unknown, transitions: Seq::filled(|_| None, alphabet_size) }
    }
    pub fn pushback(&mut self, prefix: &[u8]) {

        for tr in self.transitions.iter_mut() {
            if let Some(tr) = tr {
                tr.output.prepend_slice(prefix);
            }
        }
        self.output.mutate(|s| s.prepend_slice(prefix));
    }
    fn insert_ptt_positive<A>(&mut self, input: &IntSeq, output: &IntSeq, alphabet: &A) -> Result<(), String> where A: Alphabet {
        let mut offset = 0;
        let mut ptt_iter = self;
        for c in input.iter().map(|c| alphabet.embed(c) as usize) {
            let lcp_len = develop_tree(&mut ptt_iter.transitions[c], &output.as_bytes()[offset..], alphabet.len());
            offset += lcp_len;
            ptt_iter = unsafe { ptt_iter.transitions[c].as_mut().unwrap().target.get().as_mut() };
        }
        let rem = &output.as_bytes()[offset..];
        match &ptt_iter.output {
            Accepting(s) => {
                return if s.as_slice() != rem {
                    Err(String::from(format!("For input '{}' and output '{}' the state output is '{:?}' but training sample has remaining suffix '{:?}'", input, output, s.as_slice(), rem)))
                } else {
                    Ok(())
                };
            }
            Rejecting => {
                return Err(String::from(format!("For input '{}' and output '{}' the state rejects but training sample has remaining suffix '{:?}'", input, output, rem)));
            }
            Unknown => {}
        }
        ptt_iter.output = Accepting(Seq::from(rem));
        Ok(())
    }


    fn build_ptt<'i, I, A>(informant: &'i mut I, alphabet: &A) -> Self where I: Iterator<Item=(&'i IntSeq, &'i Option<IntSeq>)>,
                                                                   A: Alphabet {
        let mut root = State::new(alphabet.len());
        for (in_sample, out_sample) in informant {
            if let Some(out_sample) = out_sample {
                root.insert_ptt_positive(in_sample, out_sample, alphabet);
            }
        }
        root
    }


    fn ostia_compress(s:NonNull<State>) {
        let mut blue = VecDeque::new();
        let mut red = Vec::new();
        Blue::add_blue_states(s,&mut blue);
        red.push(s);
        while let Some(mut next) = blue.pop_front(){
            match red.iter().find(|&&red_state| Self::ostia_fold(red_state,next.state())){
                None => {
                    red.push(next.add_further_blue_states(&mut blue));
                }
                Some(&red_state) => {
                    next.set_target(red_state);
                }
            }
        }
    }

    fn ostia_fold(red: NonNull<State>, blue: NonNull<State>) -> bool {
        assert_ne!(red, blue);
        let red = unsafe{red.as_ref()};
        let blue = unsafe{blue.as_ref()};
        assert!(blue.output!=Rejecting); // UNKNOWN is treated here as REJECTING by default
        assert!(red.output!=Rejecting);
        if blue.output != red.output { return false; }

        for i in 0..red.transitions.len() {
            if let Some(transition_blue) = &blue.transitions[i] {
                if let Some(transition_red) = &red.transitions[i] {
                    if transition_blue.output != transition_red.output{
                        return false;
                    }

                    if !Self::ostia_fold(transition_red.target.get(),
                                         transition_blue.target.get()){
                        return false;
                    }
                }else{
                    return false;
                }
            }else{
                if red.transitions[i].is_some() {
                    return false;
                }
            }
        }
        true
    }

    fn walk_states<F:FnMut(&State)>(&self,f:&mut F){
        f(self);
        for tr in self.transitions.iter(){
            if let Some(tr) = tr {
                if let Link::Strong(tr) = &tr.target {
                    tr.walk_states(f);
                }
            }
        }
    }

    fn walk_edges<F:FnMut(&State,u8,&Edge)>(&self,f:&mut F){
        for (symbol,tr) in self.transitions.iter().enumerate(){
            if let Some(tr) = tr {
                f(self,symbol as u8,tr);
                if let Link::Strong(tr) = &tr.target {
                    tr.walk_edges( f);
                }
            }
        }
    }

    fn compile<A:Alphabet>(self,alph:&A,pos:V, ghost:&Ghost)->G{
        let mut states = HashMap::<NonNull<State>, *mut N>::new();
        self.walk_states(&mut |s|{
            let prev = states.insert(NonNull::from(s),N::new(pos.clone(),ghost));
            assert!(prev.is_none());
        });
        let init = NonNull::from(&self);
        let &init_n = states.get(&init).unwrap();
        let mut init_has_incoming = false;
        self.walk_edges(&mut |s,symbol,e|{
            let &src = states.get(&NonNull::from(s)).unwrap();
            let dst_s = e.target.get();
            let &dst = states.get(&dst_s).unwrap();
            if dst_s == init{
                assert_eq!(init_n,dst);
                init_has_incoming = true;
            }
            let a = alph.retrieve(symbol);
            let str = IntSeq::from(e.output.as_str().unwrap());
            let p = P::new(0,str);
            let ed = E::new_from_symbol(a,p);
            N::push(src,(ed,dst),ghost);
        });
        let mut g = G::new_empty();
        if let Accepting(out) = &self.output{
            let p = P::new(0,IntSeq::from(out.as_str().unwrap()));
            g.set_epsilon(Some(p));
        }

        for (symbol,tr) in self.transitions.into_iter().enumerate(){
            if let Some(tr) = tr{
                let &n = states.get(&tr.target.get()).unwrap();
                let symbol = alph.retrieve(symbol as u8);
                let str = IntSeq::from(tr.output.as_str().unwrap());
                let e = E::new_from_symbol(symbol,P::new(0,str));
                g.incoming_mut().push((e,n));
            }
        }

        for (s,n) in states{
            let s = unsafe{s.as_ref()};
            if let Accepting(out) = &s.output{
                let p = P::new(0,IntSeq::from(out.as_str().unwrap()));
                g.outgoing_mut().insert(n,p);
            }
        }
        if !init_has_incoming{
            N::delete(init_n,ghost);
        }
        g
    }

    fn infer<'i, I, A>(pos:V,ghost:&Ghost,informant: &'i mut I, alphabet: &A) -> G where I: Iterator<Item=(&'i IntSeq, &'i Option<IntSeq>)>,
                                                                             A: Alphabet {
        let root = Self::build_ptt(informant,alphabet);
        Self::ostia_compress(NonNull::from(&root));
        root.compile(alphabet,pos,ghost)
    }
}



#[cfg(test)]
mod tests {
    // Note this useful idiom: importing names from outer (for mod tests) scope.
    use super::*;
    use int_embedding::IntEmbedding;
    use ranged_graph::RangedGraph;
    use ranged_evaluation::new_output_buffer;

    #[test]
    fn test_eq1() {
        Ghost::with_mock(|ghost|{
            let informant = [(IntSeq::from("a"),Some(IntSeq::from("a")))];
            let alph = IntEmbedding::for_informant(&mut informant.iter().by_ref().map(|(a,b)|(a,b)));
            let mut g = State::infer(V::UNKNOWN,ghost,&mut informant.iter().by_ref().map(|(a,b)|(a,b)),&alph);
            let r = g.optimise_graph(ghost);
            let mut t = r.make_state_to_index_table();
            let mut b = new_output_buffer(255);
            let y = r.evaluate_tabular(&mut t,&mut b, &IntSeq::from("a"));
            let y:String = y.unwrap().iter().map(|&x| unsafe{char::from_u32_unchecked(x)}).collect();
            assert_eq!(String::from("a"), y);
            g.delete(ghost);
        });
    }

    #[test]
    fn test_eq2() {
        Ghost::with_mock(|ghost|{
            let informant = [(IntSeq::from("aa"),Some(IntSeq::from("a"))),(IntSeq::from("ab"),Some(IntSeq::from("b")))];
            let alph = IntEmbedding::for_informant(&mut informant.iter().by_ref().map(|(a,b)|(a,b)));
            let mut g = State::infer(V::UNKNOWN,ghost,&mut informant.iter().by_ref().map(|(a,b)|(a,b)),&alph);
            let r = g.optimise_graph(ghost);
            let mut t = r.make_state_to_index_table();
            let mut b = new_output_buffer(255);
            let y = r.evaluate_tabular(&mut t,&mut b, &IntSeq::from("aa"));
            let y:String = y.unwrap().iter().map(|&x| unsafe{char::from_u32_unchecked(x)}).collect();
            assert_eq!(String::from("a"), y);
            let y = r.evaluate_tabular(&mut t,&mut b, &IntSeq::from("ab"));
            let y:String = y.unwrap().iter().map(|&x| unsafe{char::from_u32_unchecked(x)}).collect();
            assert_eq!(String::from("b"), y);
            let y = r.evaluate_tabular(&mut t,&mut b, &IntSeq::from("a"));
            assert!(y.is_none());
            let y = r.evaluate_tabular(&mut t,&mut b, &IntSeq::from("aaa"));
            assert!(y.is_none());
            let y = r.evaluate_tabular(&mut t,&mut b, &IntSeq::from("b"));
            assert!(y.is_none());
            g.delete(ghost);
        });
    }
}