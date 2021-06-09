use e::{E, FullEdge};
use ranged_graph::{RangedGraph, Transitions, Range, Transition, Trans};
use g::G;
use n::N;
use v::V::UNKNOWN;
use ghost::Ghost;
use std::collections::{HashMap};
use std::collections::hash_map::Entry;
use int_seq::{A, REFLECT};
use nonmax::NonMaxUsize;

struct IBE {
    /**input symbol*/
    i: A,
    /**true if edge begins at i (exclusive), false if edge ends at i (inclusive)*/
    b: bool,
    /**edge*/
    e: usize,

}


fn optimise<'a, Tr: Trans, F: Fn(&E, &*mut N) -> Tr>(state: *mut N, map: F, ghost: &Ghost) -> Transitions<Tr> {
    let outgoing = N::outgoing(state, ghost);
    if outgoing.is_empty() {
        return Transitions::blank();
    }
    let mut points: Vec<IBE> = Vec::new();
    for edge_idx in 0..outgoing.len() {
        assert!(outgoing[edge_idx].0.from_exclusive() < outgoing[edge_idx].0.to_inclusive());
        points.push(IBE { i: outgoing[edge_idx].0.from_exclusive(), e: edge_idx, b: true });
        points.push(IBE { i: outgoing[edge_idx].0.to_inclusive(), e: edge_idx, b: false });
    }
    points.sort_by(|a, b| a.i.cmp(&b.i));
    let mut transitions = Transitions::with_capacity(points.len() + points.last().map_or_else(|| 0, |last| if last.i == A::MAX { 0 } else { 1 }));
    let mut accumulated = Vec::<usize>::new();
    let mut curr_input = points[0].i;
    if REFLECT < curr_input {
        transitions.push(Range::empty(curr_input));
    }
    for IBE { i, e, b } in points {
        if curr_input != i {
            transitions.push(Range::new(i, accumulated.iter().map(|&edge_idx| {
                let (edge, target) = &outgoing[edge_idx];
                map(edge, target)
            }).collect()));
            curr_input = i;
        }
        if b {
            accumulated.push(e);
        } else {
            accumulated.remove(accumulated.iter().position(|&x| x == e).unwrap());
        }
    }
    assert!(accumulated.is_empty(), "Accumulated edges {:?}", accumulated);
    if transitions.last().map_or_else(|| true, |last| last.input() != A::MAX) {
        transitions.push(Range::empty(A::MAX));
    }
    // assert!(isFullSigmaCovered(transitions));
    transitions
}

impl G {
    pub fn optimise_graph(&self,
                          ghost: &Ghost) -> RangedGraph<Transition> {
        self.optimise_and_collect_graph(&mut |x| None, &mut |x, y| None, ghost)
    }

    pub fn optimise_and_collect_graph<FE, FS>(&self,
                                              should_continue_per_state: &mut FS,
                                              should_continue_per_edge: &mut FE,
                                              ghost: &Ghost) -> RangedGraph<Transition>
        where FS: FnMut(*mut N) -> Option<()>, FE: FnMut(*mut N, &E) -> Option<()> {
        let initial = self.make_unique_initial_state(UNKNOWN, ghost);
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
        let graph_transitions_ptr = graph_transitions.as_mut_ptr();
        let index_to_state_ptr = index_to_state.as_mut_ptr();
        let accepting_ptr = accepting.as_mut_ptr();
        for (&state_n, &state_idx) in &states {
            let state_idx = state_idx.get();
            let transitions = optimise(state_n, |e, n| Transition::from(e.clone(), states.get(n).cloned()), ghost);
            unsafe {
                std::ptr::write(graph_transitions_ptr.offset(state_idx as isize), transitions);
                std::ptr::write(index_to_state_ptr.offset(state_idx as isize), N::meta(state_n, ghost).clone());
                std::ptr::write(accepting_ptr.offset(state_idx as isize), self.outgoing().get(&state_n).cloned());
            }
            
        }
        accepting[init_idx] = self.epsilon().clone();
        N::delete(initial, ghost);
        RangedGraph::new(graph_transitions, accepting, index_to_state, NonMaxUsize::new(init_idx).unwrap())
    }
}

