mod ghost;
mod n;
mod p;
mod v;
mod e;
mod g;
mod collect;
mod void;
mod regular_operations;
mod ranged_graph;
mod debug;
mod compilation_error;
mod ranged_optimisation;
mod ranged_evaluation;

use std::collections::{VecDeque, HashSet, HashMap};
use std::collections::hash_map::Entry::{Occupied, Vacant};
use std::collections::hash_map::Iter;
use std::any::Any;
use std::fmt::{Debug, Formatter};
use std::path::Prefix::UNC;
use std::iter::Map;
use g::G;
use v::V::UNKNOWN;
use e::E;
use ghost::Ghost;
use p::P;





fn main() {
    let mut gh = Ghost::new();
    let g = G::new_singleton(E::new_neutral(3u32, 57u32), P::neutral(), UNKNOWN, &gh);
    let g = g.right_action_on_graph(&P::new(3u32, vec![1u32, 2u32, 3u32]));
    println!("Hello, world! {:#?}", g.debug(&gh));
}
