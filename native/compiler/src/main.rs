#![feature(ptr_internals)]
#![feature(allocator_api)]
#![feature(str_internals)]
#![feature(try_trait)]
#[macro_use] extern crate lalrpop_util;
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
mod utf8;
mod exact_size_chars;
mod ranged_serializers;
mod parser;
mod parser_state;
mod parser_utils;
mod pipeline;
mod external_function;
mod func_arg;
mod learn;
mod int_embedding;
mod int_queue;
mod seq;


fn main() {
    println!("Hello world");
}
