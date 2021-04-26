use e::E;
use ranged_graph::{RangedGraph, NonSink, Transition};
use g::G;
use n::N;
use v::V::UNKNOWN;
use ghost::Ghost;
use std::collections::{HashSet, HashMap};
use p::{P, PartialEdge};
use std::collections::hash_map::Entry;
use int_seq::{IntSeq, REFLECT};
use nonmax::NonMaxUsize;
use std::convert::TryFrom;


impl RangedGraph {


    fn evaluate_tabular(self, state_to_index: &mut [u8], output_buffer: &mut [u32], input: &IntSeq) -> Option<nonmax::NonMaxUsize> {
        assert!(state_to_index.len() >= self.len());

        struct BacktrackingNode<'a> {
            prev_index: u8,
            state: NonSink,
            edge: Option<&'a Transition>, //Only the very first edge in table is None
        }
        fn new_node(prev_index: u8,
                    state: NonSink,
                    edge: Option<&Transition>) -> BacktrackingNode {
            BacktrackingNode { prev_index, state, edge }
        }
        let init_node = new_node(0, self.init(), None );
        let mut backtracking_table = Vec::<Vec<BacktrackingNode>>::with_capacity(input.len());
        let mut prev_column = vec![init_node];

        for i in 1..=input.len() {
            if prev_column.len() == 0 { return None; }
            let input_symbol = input[i - 1];
            let mut next_column = Vec::<BacktrackingNode>::with_capacity(prev_column.len() * 2);

            for src_state_idx in 0..prev_column.len() {
                let src_state = prev_column[src_state_idx].state;
                let transitions = self.transitions(src_state).binary_search(input_symbol);
                for transition in transitions {
                    let dest_state = match transition.target() {
                        None => continue,
                        Some(dest_state) => dest_state
                    };
                    let state_idx = state_to_index[dest_state.get()] as usize;
                    if state_idx < next_column.len() && next_column[state_idx].state == dest_state {
                        let conflicting_node = &mut next_column[state_idx];
                        if conflicting_node.edge.unwrap().weight() < transition.weight() {
                            conflicting_node.prev_index = src_state_idx as u8;
                            conflicting_node.edge = Some(transition);
                        }
                    } else {
                        state_to_index[dest_state.get()] = next_column.len() as u8;
                        next_column.push(new_node(src_state_idx as u8, dest_state, transition.edge()));
                    }
                }
            }

            backtracking_table.push(prev_column);
            prev_column = next_column;
        }
        assert_eq!(backtracking_table.len(), input.len());
        let mut fin_weight = i32::MIN;
        let mut fin_edge = None;
        let mut node = &backtracking_table[0][0]; //just some dummy value
        for i in 0..prev_column.len() {
            let node_candidate = &prev_column[i];
            match self.accepting(node_candidate.state) {
                None => (),
                Some(fin_edge_candidate) => if fin_edge_candidate.weight() > fin_weight {
                    fin_weight = fin_edge_candidate.weight();
                    fin_edge = Some(fin_edge_candidate);
                    node = node_candidate;
                },
            }
        }

        let fin_edge = match fin_edge {
            None => return None,
            Some(p) => p
        };

        let mut output_buffer_idx = output_buffer.len();
        for &out_symbol in fin_edge.output().iter().rev() {
            if out_symbol != REFLECT {
                output_buffer_idx -= 1;
                output_buffer[output_buffer_idx] = out_symbol;
            }
        }

        for input_symbol_idx in (0..input.len()).rev(){
            let input_symbol= input[input_symbol_idx];
            for &out_symbol in node.edge.output().iter().rev() {
                output_buffer_idx -= 1;
                if out_symbol == REFLECT {
                    output_buffer[output_buffer_idx] = input_symbol;
                } else {
                    output_buffer[output_buffer_idx] = out_symbol;
                }
            }
            let column = &backtracking_table[input_symbol_idx];
            node = &column[node.prev_index as usize];
        }
        let out_len = output_buffer.len() - output_buffer_idx;
        let out_len = NonMaxUsize::try_from(out_len).unwrap();
        Some(out_len)
    }
}