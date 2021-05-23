use func_arg::FuncArgs;
use compilation_error::CompErr;
use g::G;
use v::V;

pub type ExternalFunction = Fn(V,FuncArgs)->Result<G,CompErr>;