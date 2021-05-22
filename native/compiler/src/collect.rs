use std::collections::{HashSet, HashMap, VecDeque};
use std::collections::hash_map::Entry::{Occupied, Vacant};
use n::N;
use e::E;
use ghost::Ghost;
use void::Void;
use g::G;

trait InOut<X> {
    fn add(&mut self, x: X);
    fn remove(&mut self) -> Option<X>;
}

impl<X> InOut<X> for Vec<X> {
    fn add(&mut self, x: X) {
        self.push(x)
    }

    fn remove(&mut self) -> Option<X> {
        self.pop()
    }
}

impl<X> InOut<X> for VecDeque<X> {
    fn add(&mut self, x: X) {
        self.push_back(x)
    }

    fn remove(&mut self) -> Option<X> {
        self.pop_front()
    }
}

impl N {
    pub fn collect<Y, F,FS,FE>(depth_first_search: bool, startpoint: *mut N, mut collect: F,
                         should_continue_per_state: &mut FS,
                         should_continue_per_edge: &mut FE, ghost: &Ghost) -> Option<Y>
        where F: FnMut(*mut N) -> bool, FS: FnMut(*mut N) -> Option<Y>, FE: FnMut(*mut N, &E) -> Option<Y>  {
        let mut to_visit: Box<dyn InOut<*mut N>> = if depth_first_search { Box::new(VecDeque::new()) } else { Box::new(Vec::new()) };
        if collect(startpoint) {
            let y = should_continue_per_state(startpoint);
            if y.is_some() { return y; }
            to_visit.add(startpoint);
        }

        while let Some(state) = to_visit.remove() {
            for (e, n) in N::outgoing(state, ghost) {
                let ye = should_continue_per_edge(*n, e);
                if ye.is_some() { return ye; }
                if collect(*n) {
                    to_visit.add(*n);
                    let y = should_continue_per_state(*n);
                    if y.is_some() { return y; }
                }
            }
        }
        return Option::None;
    }

    pub fn collect_set<Y, FE, FS>(depth_first_search: bool, startpoint: *mut N,
                                  collected: &mut HashSet<*mut N>,
                                  should_continue_per_state: &mut FS,
                                  should_continue_per_edge: &mut FE, ghost: &Ghost) -> Option<Y>
        where FS: FnMut(*mut N) -> Option<Y>, FE: FnMut(*mut N, &E) -> Option<Y> {
        N::collect(depth_first_search, startpoint, |ptr| collected.insert(ptr), should_continue_per_state, should_continue_per_edge, ghost)
    }

    pub fn collect_all_to_set<Y,FE,FS>(depth_first_search: bool, startpoint: *mut N,
                                 should_continue_per_state: &mut FS,
                                 should_continue_per_edge: &mut FE, ghost: &Ghost) -> HashSet<*mut N>
        where FS:FnMut(*mut N) -> Option<Y>, FE:FnMut(*mut N, &E) -> Option<Y>{
        let mut collected = HashSet::new();
        N::collect_set(depth_first_search, startpoint, &mut collected, should_continue_per_state, should_continue_per_edge, ghost);
        collected
    }


    pub fn deep_clone(original: *mut N, cloned: &mut HashMap<*mut N, *mut N>, ghost: &Ghost) -> *mut N {
        if let Some(previously_cloned) = cloned.get(&original) {//nothing to be done!
            return *previously_cloned;
        }
        let clone_init = N::shallow_copy(original, ghost);// create new clone
        cloned.insert(original, clone_init);
        let mut stack = Vec::new();
        stack.push((original, clone_init));
        while let Some((original_v, cloned_v)) = stack.pop() {
            for (edge, other_connected_original) in N::outgoing(original_v, ghost) {
                let other_connected_copied = match cloned.entry(*other_connected_original) {
                    Occupied(already_cloned) => {
                        *already_cloned.get()
                    }
                    Vacant(entry) => {
                        *entry.insert(N::shallow_copy(*other_connected_original, ghost))
                    }
                };
                N::outgoing_mut(cloned_v, ghost).push((edge.clone(), other_connected_copied));
            }
        }
        return clone_init;
    }
}

impl G {
    pub fn collect_graph<FE, FS, Y>(&self,
                                    should_continue_per_state: &mut FS,
                                    should_continue_per_edge: &mut FE, ghost: &Ghost) -> HashSet<*mut N>
        where FS: FnMut(*mut N) -> Option<Y>, FE: FnMut(*mut N, &E) -> Option<Y> {
        let mut collected = HashSet::new();
        for (_, v) in self.incoming() {
            N::collect_set(true, *v, &mut collected, should_continue_per_state, should_continue_per_edge, ghost);
        }
        collected
    }
    pub fn collect_whole_graph(&self, ghost: &Ghost) -> HashSet<*mut N> {
        self.collect_graph::<_,_,Void>(&mut |_| None, &mut |_, _| None, ghost)
    }
}