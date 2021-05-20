use p::{P, W, PartialEdge};
use string_interner::symbol::SymbolU32;
use v::V;

use int_seq::{A, EPSILON, IntSeq};
use std::ops::{Index, IndexMut};
use alloc::vec::IntoIter;
use string_interner::StringInterner;

pub type NonSink = nonmax::NonMaxUsize;

pub type State = Option<NonSink>;

pub trait Trans: Sized {
    fn weight(&self) -> W;

    fn output(&self) -> &IntSeq;

    fn target(&self) -> State;
}

pub struct Transition {
    weight: W,
    output: IntSeq,
    target: State,
}


impl Trans for Transition {
    fn weight(&self) -> W {
        self.weight
    }

    fn output(&self) -> &IntSeq {
        &self.output
    }

    fn target(&self) -> State {
        self.target
    }
}

impl Transition {
    pub fn new(weight: W, output: IntSeq, target: State) -> Self {
        Transition { weight, output, target }
    }

    pub fn from<P:PartialEdge>(edge: P, target: State) -> Self {
        let (weight, output) = edge.destruct();
        Self::new(weight, output, target)
    }

    pub fn neutral(edge: P, target: State) -> Self {
        Self::new(0, EPSILON, target)
    }
}

pub struct Range<Tr: Trans> {
    input: A,
    edges: Vec<Tr>,
}

impl<Tr: Trans> Range<Tr> {
    pub fn new(input: A, edges: Vec<Tr>) -> Self {
        Self { input, edges }
    }
    pub fn empty(input: A) -> Self {
        Self::new(input,Vec::with_capacity(0))
    }
    pub fn input(&self) -> A {
        self.input
    }

    pub fn edges(&self) -> &Vec<Tr> {
        &self.edges
    }

    pub fn transition(&self, idx: usize) -> &Tr {
        &self.edges[idx]
    }

    pub fn edges_mut(&mut self, idx: usize) -> &mut Tr {
        &mut self.edges[idx]
    }
}

pub struct Transitions<Tr: Trans>(Vec<Range<Tr>>);

impl<Tr: Trans> Transitions<Tr> {
    pub fn with_capacity(cap: usize) -> Self {
        Self(Vec::with_capacity(cap))
    }
    pub fn push(&mut self, range: Range<Tr>) {
        self.0.push(range)
    }
    pub fn last(&self) -> Option<&Range<Tr>> {
        self.0.last()
    }
}

impl<Tr: Trans> Index<usize> for Transitions<Tr> {
    type Output = Range<Tr>;

    fn index(&self, index: usize) -> &Self::Output {
        &self.0[index]
    }
}

impl<Tr: Trans> IndexMut<usize> for Transitions<Tr> {
    fn index_mut(&mut self, index: usize) -> &mut Self::Output {
        &mut self.0[index]
    }
}

impl<Tr: Trans> IntoIterator for Transitions<Tr> {
    type Item = Range<Tr>;
    type IntoIter = IntoIter<Range<Tr>>;

    fn into_iter(self) -> Self::IntoIter {
        self.0.into_iter()
    }
}

impl<Tr: Trans> Transitions<Tr> {
    pub fn len(&self) -> usize {
        self.0.len()
    }
    pub fn binary_search(&self, input_symbol: A) -> &Vec<Tr> {
        &self[self.binary_search_index(input_symbol)].edges
    }
    pub fn binary_search_mut(&mut self, input_symbol: A) -> &mut Vec<Tr> {
        let i = self.binary_search_index(input_symbol);
        &mut self[i].edges
    }
    pub fn binary_search_index(&self, input_symbol: A) -> usize {
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

    pub fn assert_full_sigma_covered(&self) {
        assert!(self.len() > 0);
        for w in self.0.windows(2) {
            assert!(w[0].input < w[1].input);
        }
        assert!(0 < self[0].input);
        assert_eq!(self[self.len() - 1].input, A::MAX);
    }
}

pub struct RangedGraph<Tr: Trans> {
    graph: Vec<Transitions<Tr>>,
    accepting: Vec<Option<P>>,
    index_to_state: Vec<V>,
    initial: NonSink,
}

impl<Tr: Trans> RangedGraph<Tr> {
    pub fn new(graph: Vec<Transitions<Tr>>,
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

    pub fn graph(&self) -> &Vec<Transitions<Tr>> {
        &self.graph
    }

    pub fn transitions(&self, state: NonSink) -> &Transitions<Tr> {
        &self.graph[state.get()]
    }

    pub fn accepting(&self, state: NonSink) -> &Option<P> {
        &self.accepting[state.get()]
    }

    pub fn index_to_state(&self, state: NonSink) -> &V {
        &self.index_to_state[state.get()]
    }

    pub fn len(&self) -> usize {
        self.graph.len()
    }
}
