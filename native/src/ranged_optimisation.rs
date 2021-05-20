use e::{E, FullEdge};
use ranged_graph::{RangedGraph, Transitions, Range, Transition, Trans, State};
use g::G;
use n::N;
use v::V::UNKNOWN;
use ghost::Ghost;
use std::collections::{HashSet, HashMap};
use p::P;
use std::collections::hash_map::Entry;
use int_seq::A;
use nonmax::NonMaxUsize;

struct IBE {
    /**input symbol*/
    i: u32,
    /**true if edge begins at i (exclusive), false if edge ends at i (inclusive)*/
    b: bool,
    /**edge*/
    e: usize,

}


fn for_each_without_duplicates<'a, F: FnMut(&'a mut IBE)>(points: &'a Vec<&'a mut IBE>,
                                                          callback: F) {}


fn optimise<'a, Tr: Trans, F: Fn(&E, &*mut N) -> Tr>(state: *mut N, map: F, ghost: &Ghost) -> Transitions<Tr> {
    let mut points: Vec<IBE> = Vec::new();
    let outgoing = N::outgoing(state, ghost);
    for edge_idx in 0..outgoing.len() {
        assert!(outgoing[edge_idx].0.from_exclusive() < outgoing[edge_idx].0.to_inclusive());
        points.push(IBE { i: outgoing[edge_idx].0.from_exclusive(), e: edge_idx, b: true });
        points.push(IBE { i: outgoing[edge_idx].0.to_inclusive(), e: edge_idx, b: false });
    }
    points.sort_by(|a, b| a.i.cmp(&b.i));
    let mut transitions = Transitions::with_capacity(points.len() + points.last().map_or_else(|| 0, |last| if last.i == u32::MAX { 0 } else { 1 }));
    let mut accumulated = Vec::<usize>::new();
    let mut curr_input = points[0].i;
    if 0 < curr_input {
        transitions.push(Range::empty(curr_input));
    }
    for IBE { i, e, b } in points {
        if curr_input == i {
            if b {
                accumulated.push(e);
            } else {
                accumulated.remove(accumulated.iter().position(|&x| x == e).unwrap());
            }
        } else {
            transitions.push(Range::new(i, accumulated.iter().map(|&edge_idx| {
                    let (edge, target) = &outgoing[edge_idx];
                    map(edge, target)
                }).collect()));
            curr_input = i;
        }
    }
    assert!(accumulated.is_empty());
    if transitions.last().map_or_else(|| true, |last| last.input() != A::MAX) {
        transitions.push(Range::empty(A::MAX));
    }
    // assert!(isFullSigmaCovered(transitions));
    transitions
}


fn optimise_graph(graph: G,
                  should_continue_per_state: fn(*mut N) -> Option<()>,
                  should_continue_per_edge: fn(*mut N, &E) -> Option<()>,
                  ghost: &Ghost) -> RangedGraph<Transition> {
    let initial = graph.make_unique_initial_state(UNKNOWN, ghost);
    let mut states = HashMap::<*mut N, NonMaxUsize>::new();
    N::collect(true, initial, |n| {
        let len = states.len();
        match states.entry(n) {
            Entry::Occupied(_) => false,
            Entry::Vacant(e) => {
                e.insert(NonMaxUsize::new(len).unwrap());
                true
            }
        }
    }, should_continue_per_state, should_continue_per_edge, ghost);
    let init_idx: usize = 0;
    assert_eq!(states.get(&initial).unwrap().get(), init_idx);

    let mut graph_transitions = Vec::with_capacity(states.len());
    unsafe { graph_transitions.set_len(states.len()); }
    let mut index_to_state = Vec::with_capacity(states.len());
    unsafe { index_to_state.set_len(states.len()); }
    let mut accepting = Vec::with_capacity(states.len());
    unsafe { accepting.set_len(states.len()); }
    for (&state_n, &state_idx) in &states {
        let state_idx = state_idx.get();
        let transitions = optimise(state_n, |e, n| Transition::from(e.clone(), states.get(n).cloned()), ghost);
        index_to_state[state_idx] = N::meta(state_n, ghost).clone();
        graph_transitions[state_idx] = transitions;
        accepting[state_idx] = graph.outgoing().get(&state_n).cloned();
    }
    accepting[init_idx] = graph.epsilon().clone();
    RangedGraph::new(graph_transitions, accepting, index_to_state, NonMaxUsize::new(init_idx).unwrap())
}
