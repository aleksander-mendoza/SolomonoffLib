#![feature(ptr_internals)]
#![feature(allocator_api)]
#![feature(str_internals)]
extern crate nonmax;
extern crate alloc;
extern crate core;

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
mod ostia_compress;
mod utf8;
mod exact_size_chars;


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
use int_seq::IntSeq;

fn main() {
    

    struct C {
        a:u32,
        b:u64,
        c:String
    }
    let c = C{a:4,b:6,c:String::from("tre")};
}
