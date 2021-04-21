use p::{P, W, PartialEdge};
use v::V;

use int_seq::{A, IntSeq};
use std::ops::{Index, IndexMut};
use alloc::vec::IntoIter;

pub type NonSink = nonmax::NonMaxUsize;

pub type State = Option<NonSink>;



pub struct Transition {
    edge: P,
    target: State,
}

impl PartialEdge for Transition{
    fn weight(&self) -> i32 {
        self.edge.weight()
    }

    fn output(&self) -> &IntSeq {
        self.edge.output()
    }
}

impl Transition {
    pub fn edge(&self) -> &P {
        &self.edge
    }

    pub fn target(&self) -> State {
        self.target
    }
}

pub struct Range {
    input: A,
    edges: Vec<Transition>,
}

impl Range {
    pub fn input(&self) -> A {
        self.input
    }

    pub fn edges(&self) -> &Vec<Transition> {
        &self.edges
    }

    pub fn transition(&self, idx: usize) -> &Transition {
        &self.edges[idx]
    }

    pub fn edges_mut(&mut self, idx: usize) -> &mut Transition {
        &mut self.edges[idx]
    }
}

pub struct Transitions(Vec<Range>);
impl Index<usize> for Transitions{
    type Output = Range;

    fn index(&self, index: usize) -> &Self::Output {
        &self.0[index]
    }
}
impl IndexMut<usize> for Transitions{
    fn index_mut(&mut self, index: usize) -> &mut Self::Output {
        &mut self.0[index]
    }
}
impl IntoIterator for Transitions{
    type Item = Range;
    type IntoIter = IntoIter<Range>;

    fn into_iter(self) -> Self::IntoIter {
        self.0.into_iter()
    }
}
impl Transitions{
    pub fn len(&self)->usize{
        self.0.len()
    }
    pub fn binary_search(&self, input_symbol:A) -> &Vec<Transition> {
        &self[self.binary_search_index(input_symbol)].edges
    }
    pub fn binary_search_mut(&mut self, input_symbol:A) -> &mut Vec<Transition> {
        let i = self.binary_search_index(input_symbol);
        &mut self[i].edges
    }
    pub fn binary_search_index(&self, input_symbol:A) -> usize{
        self.assert_full_sigma_covered();
        let mut low = 0;
        let mut high = self.len() - 1;
        while low <= high {
            let mid = (low + high) >> 1;
            let mid_val = &self[mid];
            if mid_val.input() < input_symbol {
                low = mid + 1;
            } else if mid_val.input() > input_symbol {
                high = mid - 1;
            } else {
                return mid; // key found at transitions.get(mid)
            }
        }
        assert!(low < self.len());
        low
    }

    pub fn assert_full_sigma_covered(&self){
        assert!(self.len()>0);
        for w in self.0.windows(2){
            assert!( w[0].input< w[1].input);
        }
        assert!(0<self[0].input);
        assert_eq!(self[self.len() - 1].input, A::MAX);
    }
}

pub struct RangedGraph {
    graph: Vec<Transitions>,
    accepting: Vec<Option<P>>,
    index_to_state: Vec<V>,
    initial: NonSink,
}

impl RangedGraph {

    pub fn new(graph: Vec<Transitions>,
               accepting: Vec<Option<P>>,
               index_to_state: Vec<V>,
               initial: NonSink) -> Self {
        assert_eq!(graph.len(), accepting.len());
        assert_eq!(graph.len(), index_to_state.len());
        assert!(initial.get() < graph.len());
        RangedGraph { graph, accepting, index_to_state, initial }
    }

    pub fn init(&self) -> NonSink {
        self.initial
    }

    pub fn graph(&self) -> &Vec<Transitions> {
        &self.graph
    }

    pub fn transitions(&self, state: NonSink) -> &Transitions {
        &self.graph[state.get()]
    }

    pub fn accepting(&self, state: NonSink) -> &Option<P> {
        &self.accepting[state.get()]
    }

    pub fn index_to_state(&self, state: NonSink) -> &V {
        &self.index_to_state[state.get()]
    }

    pub fn len(&self)->usize{
        self.graph.len()
    }
}
