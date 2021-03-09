use e::E;
use ranged_graph:: RangedGraph;
use g::G;
use n::N;
use v::V::UNKNOWN;
use ghost::Ghost;
use std::collections::{HashSet, HashMap};
use p::P;
use std::collections::hash_map::Entry;

struct IBE {
    /**input symbol*/
    i: u32,
    /**edges beginning (exclusive) at input symbol*/
    b: Vec<usize>,
    /**edges ending (inclusive) at input symbol*/
    e: Vec<usize>,
}

impl IBE {
    fn new_b(i:u32,b: usize) -> IBE {
        IBE { i,b: vec![b], e: vec![] }
    }
    fn new_e(i:u32,e: usize) -> IBE {
        IBE { i, b: vec![], e: vec![e] }
    }
}

fn for_each_without_duplicates< F: FnMut(&&mut IBE)>(points: &Vec<&mut IBE>,
                                                      callback: F) {
    if points.len() >= 1 {
        let mut prev = &points[0];
        for j in 1..points.len() {
            let curr = &points[j];
            if prev.i == curr.i {
                prev.b.extend(&curr.b);
                prev.e.extend(&curr.e);
            } else {
                callback(prev);
                prev = curr;
            }
        }
        callback(prev);
    }
}


fn optimise<'a, M, F: Fn(&E,&*mut N) -> M>(state: *mut N, map: F, ghost: &Ghost) -> Vec<(u32, Vec<M>)> {
    let mut point_buffer: Vec<IBE> = Vec::new();
    let outgoing = N::outgoing(state, ghost);
    for edge_idx in 0..outgoing.len() {
        assert!(outgoing[edge_idx].0.from_exclusive() < outgoing[edge_idx].0.to_inclusive());
        point_buffer.push(IBE::new_b(outgoing[edge_idx].0.from_exclusive(),edge_idx));
        point_buffer.push(IBE::new_e(outgoing[edge_idx].0.to_inclusive(), edge_idx));
    }
    let mut points: Vec<&mut IBE> = point_buffer.iter_mut().collect();
    points.sort_by(|a, b| a.i.cmp(&b.i));
    let mut transitions = Vec::<(u32, Vec<M>)>::with_capacity(points.len() + points.last().map_or_else(|| 0, |last| if last.i == u32::MAX { 0 } else { 1 }));
    let mut accumulated = Vec::<usize>::new();
    for_each_without_duplicates(&points, |ibe| {
        let end_inclusive = ibe.i;
        let mut edges_in_range = Vec::with_capacity(accumulated.len());
        for edge_idx in accumulated {
            let (edge, target) = &outgoing[edge_idx];
            edges_in_range.push( map(edge,target));
        }
        if 0 < end_inclusive {
            transitions.push((end_inclusive, edges_in_range));
        }
        accumulated.retain(|e| !ibe.e.contains(e));
        accumulated.extend(ibe.b.iter());
    });
    if transitions.last().map_or_else(|| true, |last| last.0 != u32::MAX) {
        transitions.push((u32::MAX, vec![]));
    }
    assert!(accumulated.is_empty());
    // assert!(isFullSigmaCovered(transitions));
    transitions
}


fn optimise_graph(graph: G,
                  should_continue_per_state: fn(*mut N) -> Option<()>,
                  should_continue_per_edge: fn(*mut N, &E) -> Option<()>,
                  ghost: &Ghost) -> RangedGraph{
    let initial = graph.make_unique_initial_state(UNKNOWN, ghost);
    let mut states = HashMap::<*mut N, i64>::new();
    N::collect(true, initial, |n| {
        let len = states.len() as i64;
        match states.entry(n) {
            Entry::Occupied(_) => false,
            Entry::Vacant(e) => {
                e.insert(len);
                true
            }
        }
    }, should_continue_per_state, should_continue_per_edge,ghost);
    let init_idx = *states.get(&initial).unwrap();
    assert_eq!(init_idx, 0);
    let mut graph_transitions = Vec::with_capacity(states.len());
    unsafe { graph_transitions.set_len(states.len()); }
    let mut index_to_state = Vec::with_capacity(states.len());
    unsafe { index_to_state.set_len(states.len()); }
    let mut accepting = Vec::with_capacity(states.len());
    unsafe { accepting.set_len(states.len()); }
    for (state_n, state_idx ) in states {
        let state_idx = state_idx as usize;
        let transitions = optimise(state_n, |e,n|(e.clone(), *states.get(n).unwrap()), ghost);
        index_to_state[state_idx] = N::meta(state_n, ghost).clone();
        graph_transitions[state_idx] = transitions;
        accepting[state_idx] = graph.outgoing().get(&state_n).cloned();
    }
    accepting[init_idx as usize] = graph.epsilon().clone();
    RangedGraph::new(graph_transitions, accepting, index_to_state, init_idx)
}
