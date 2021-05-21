use std::collections::HashMap;

use e::E;
use ghost::Ghost;
use int_seq::{A, IntSeq};
use n::N;
use p::P;
use v::V;

pub struct G {
    incoming: Vec<(E, *mut N)>,
    outgoing: HashMap<*mut N, P>,
    epsilon: Option<P>,
}

impl G {
    pub fn epsilon_and_incoming(&mut self) -> (&Option<P>, &mut Vec<(E, *mut N)>) {
        (&self.epsilon, &mut self.incoming)
    }
    pub fn epsilon_and_outgoing(&mut self) -> (&Option<P>, &mut HashMap<*mut N, P>) {
        (&self.epsilon, &mut self.outgoing)
    }
    pub fn epsilon(&self) -> &Option<P> {
        &self.epsilon
    }
    pub fn set_epsilon(&mut self, eps: Option<P>) -> () {
        self.epsilon = eps;
    }
    pub fn incoming(&self) -> &Vec<(E, *mut N)> {
        &self.incoming
    }
    pub fn incoming_mut(&mut self) -> &mut Vec<(E, *mut N)> {
        &mut self.incoming
    }
    pub fn outgoing(&self) -> &HashMap<*mut N, P> {
        &self.outgoing
    }
    pub fn outgoing_mut(&mut self) -> &mut HashMap<*mut N, P> {
        &mut self.outgoing
    }
    pub fn set_outgoing(&mut self, outgoing: HashMap<*mut N, P>) -> () {
        self.outgoing = outgoing;
    }
    pub fn rip_out_outgoing(self) -> HashMap<*mut N, P> {
        let G {
            incoming,
            outgoing,
            epsilon,
        } = self;
        outgoing
    }
    pub fn new_empty() -> G {
        G { incoming: vec![], outgoing: HashMap::new(), epsilon: None }
    }
    pub fn new_singleton(incoming: E, outgoing: P, meta: V, ghost: &Ghost) -> G {
        let v = N::new(meta, ghost);
        let mut map = HashMap::new();
        map.insert(v, outgoing);
        G { incoming: vec![(incoming, v)], outgoing: map, epsilon: None }
    }
    pub fn new_epsilon(epsilon: P) -> G {
        G { incoming: vec![], outgoing: HashMap::new(), epsilon: Some(epsilon) }
    }
    pub fn new_neutral_epsilon() -> G {
        G::new_epsilon(P::neutral())
    }
    pub fn new_from_ranges(ranges: impl Iterator<Item=(A, A)>, meta: V, ghost: &Ghost) -> G {
        let v = N::new(meta, ghost);
        let mut map = HashMap::new();
        map.insert(v, P::neutral());
        G { incoming: ranges.map(|(from, to)| (E::new_neutral(from, to), v)).collect(), outgoing: map, epsilon: None }
    }
    pub fn new_from_symbol(symbol: A, meta: V, ghost: &Ghost) -> G {
        assert!(symbol > 0);
        let v = N::new(meta, ghost);
        let mut map = HashMap::new();
        map.insert(v, P::neutral());
        G { incoming: vec![(E::new_neutral_from_symbol(symbol), v)], outgoing: map, epsilon: None }
    }
    pub fn new_from_iter<I>(mut str: I, edge_producer: fn(A) -> E, meta: V, ghost: &Ghost) -> G where
        I: Iterator<Item=A>{
        if let Some(first_symbol) = str.next() {
            let init = N::new(meta, ghost);
            let mut last = init;
            while let Some(next_symbol) = str.next() {
                let next = N::new(N::meta(init, ghost).clone(), ghost);
                N::outgoing_mut(last, ghost).push((edge_producer(next_symbol), next));
                last = next;
            }
            let mut map = HashMap::new();
            map.insert(last, P::neutral());
            G { incoming: vec![(edge_producer(first_symbol), init)], outgoing: map, epsilon: None }
        } else {
            G::new_neutral_epsilon()
        }
    }
    pub fn new_from_string<I>(str: I, meta: V, ghost: &Ghost) -> G where
        I: Iterator<Item=A> {
        Self::new_from_iter(str, E::new_neutral_from_symbol, meta, ghost)
    }
    pub fn new_from_reflected_string<I>(str: I, meta: V, ghost: &Ghost) -> G where
        I: Iterator<Item=A>{
        Self::new_from_iter(str, |a| E::new_from_symbol(a, P::new(0, IntSeq::singleton(a).unwrap())), meta, ghost)
    }
    pub fn is_empty(&self) -> bool {
        self.epsilon.is_none() && (self.incoming.is_empty() || self.outgoing.is_empty())
    }
    pub fn make_unique_initial_state(&self, state: V, ghost: &Ghost) -> *mut N {
        let init = N::new(state, ghost);
        for i in self.incoming() {
            N::push(init, i.clone(), ghost);
        }
        init
    }
    pub fn delete(&mut self, ghost: &Ghost) {
        for v in self.collect_whole_graph(ghost) {
            N::delete(v, ghost);
        }
    }
}