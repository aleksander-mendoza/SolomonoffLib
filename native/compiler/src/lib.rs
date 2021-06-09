#![feature(ptr_internals)]
#![feature(allocator_api)]
#![feature(str_internals)]
#![feature(try_trait)]
#[macro_use]
extern crate lalrpop_util;
extern crate nonmax;
extern crate alloc;
extern crate core;

pub mod ghost;
pub mod n;
pub mod p;
pub mod v;
pub mod e;
pub mod g;
pub mod collect;
pub mod void;
pub mod regular_operations;
pub mod ranged_graph;
pub mod debug;
pub mod compilation_error;
pub mod ranged_optimisation;
pub mod ranged_evaluation;
pub mod int_seq;
pub mod util;
pub mod submatch;
pub mod utf8;
pub mod exact_size_chars;
pub mod ranged_serializers;
pub mod solomonoff;
pub mod parser_state;
pub mod parser_utils;
pub mod pipeline;
pub mod external_function;
pub mod func_arg;
pub mod learn;
pub mod int_embedding;
pub mod int_queue;
pub mod seq;
pub mod external_functions;
pub mod repl_command;
pub mod repl;

