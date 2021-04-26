extern crate nonmax;
extern crate string_interner;
extern crate alloc;

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
mod int_seq;
mod util;
mod submatch;
mod string_interner;


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
use string_interner::StringInterner;

fn main() {
    

    struct C {
        a:u32,
        b:u64,
        c:String
    }
    let c = C{a:4,b:6,c:String::from("tre")};
    let mut interner = StringInterner::default();
    let sym0 = interner.get_or_intern("Elephant");
    let sym1 = interner.get_or_intern("Tiger");
    let sym2 = interner.get_or_intern("Horse");
    let sym3 = interner.get_or_intern("Tiger");
    for (sym, str) in &interner {
        println!("{:?} {}",sym, str)
    }
    
    assert_ne!(sym0, sym1);
    assert_ne!(sym0, sym2);
    assert_ne!(sym1, sym2);
    assert_eq!(sym1, sym3); // same!

    let mut gh = Ghost::new();
    let g = G::new_singleton(E::new_neutral(3, 57), P::neutral(), UNKNOWN, &gh);
    let g = g.right_action_on_graph(&P::new(3, vec![1, 2, 3]));
    println!("Hello, world! {:#?}", g.debug(&gh));
}
