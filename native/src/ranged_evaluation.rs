use e::E;
use ranged_graph:: RangedGraph;
use g::G;
use n::N;
use v::V::UNKNOWN;
use ghost::Ghost;
use std::collections::{HashSet, HashMap};
use p::P;
use std::collections::hash_map::Entry;



impl RangedGraph{

    fn eval(self, str:Vec<u32>)->Vec<u32>{
        struct Node{
            state:i64,
            prev_node_idx:u32,
        }
        let mut superpositions = Vec::<Vec<Node>>::with_capacity(str.len()+1);
        superpositions.push(vec![Node{state:self.init(),prev_node_idx:-1}]);
        let mut state_to_index = ();

        for i in 0..str.len(){
            for state in superpositions[i]{
                superpositions
            }
        }
        let mut out = Vec::<u32>::with_capacity(str.len());
        out
    }
}