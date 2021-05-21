use ranged_graph::{RangedGraph, NonSink, Trans};
use p::PartialEdge;
use int_seq::{REFLECT, A};
use nonmax::NonMaxUsize;
use std::convert::TryFrom;


pub struct StateToIndexTable(Vec<u8>);

impl StateToIndexTable {
    pub fn new(size: usize) -> Self {
        let mut v = Vec::with_capacity(size);
        unsafe { v.set_len(size) };
        Self(v)
    }
}


impl<Tr: Trans> RangedGraph<Tr> {
    pub fn new_output_buffer(size: usize) -> Vec<u32> {
        let mut v = Vec::with_capacity(size);
        unsafe { v.set_len(size) };
        v
    }
    pub fn make_state_to_index_table(&self) -> StateToIndexTable {
        StateToIndexTable::new(self.len())
    }
    pub fn evaluate_tabular<'a, 'b, I: 'a, R>(&self, state_to_index: &mut StateToIndexTable, output_buffer: &'b mut [A], input: &'a I) -> Option<&'b [A]>
        where &'a I: IntoIterator<IntoIter=R, Item=A>, R: DoubleEndedIterator<Item=A> + ExactSizeIterator {
        assert!(state_to_index.0.len() >= self.len());
        #[derive(Debug, Eq, PartialEq)]
        struct BacktrackingNode<'a, Tr> {
            prev_index: u8,
            state: NonSink,
            edge: Option<&'a Tr>, //Only the very first edge in table is None
        }
        fn new_node<Tr>(prev_index: u8,
                        state: NonSink,
                        edge: Option<&Tr>) -> BacktrackingNode<Tr> {
            BacktrackingNode { prev_index, state, edge }
        }
        let init_node = new_node(0, self.init(), None);
        let mut backtracking_table = Vec::<Vec<BacktrackingNode<Tr>>>::with_capacity(input.into_iter().size_hint().0+1);
        // This vector encodes sparse set of currently active states (current configuartion of states).
        let mut prev_column = vec![init_node];

        for input_symbol in input.into_iter() {
            // Given the current configuation of states, we now compute the next configuration after reading next input symbol
            if prev_column.len() == 0 { return None; }
            let mut next_column = Vec::<BacktrackingNode<Tr>>::with_capacity(prev_column.len() * 2);

            for src_state_idx in 0..prev_column.len() {
                let src_state = prev_column[src_state_idx].state;
                let transitions = self.transitions(src_state).binary_search(input_symbol as A);
                for transition in transitions {
                    let dest_state = match transition.target() {
                        None => continue,
                        Some(dest_state) => dest_state
                    };
                    let state_idx = state_to_index.0[dest_state.get()] as usize;
                    if state_idx < next_column.len() && next_column[state_idx].state == dest_state {
                        let conflicting_node = &mut next_column[state_idx];
                        if conflicting_node.edge.unwrap().weight() < transition.weight() {
                            conflicting_node.prev_index = src_state_idx as u8;
                            conflicting_node.edge = Some(transition);
                        }
                    } else {
                        state_to_index.0[dest_state.get()] = next_column.len() as u8;
                        next_column.push(new_node(src_state_idx as u8, dest_state, Some(transition)));
                    }
                }
            }

            backtracking_table.push(prev_column);
            prev_column = next_column;
        }
        backtracking_table.push(prev_column);
        let mut fin_weight = i32::MIN;
        let mut fin_edge = None;
        let mut node = &backtracking_table[0][0]; //just some dummy value
        //Now we scan the last column to find the final state with largest weight
        for node_candidate in backtracking_table.last().unwrap() {
            match self.is_accepting(node_candidate.state) {
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
        // Now we collect the output from final accepting state. The string
        // is printed in reverse
        let mut output_buffer_idx = output_buffer.len();
        for out_symbol in fin_edge.output().iter().rev() {
            if out_symbol != REFLECT {
                output_buffer_idx -= 1;
                output_buffer[output_buffer_idx] = out_symbol;
            }
        }
        // Now we collect the output from every traversed transition but we go
        //in reverse order and we print revered strings. Everything is inserted
        //into the output buffer from the end, hence we end up with the correct result
        //and we don't need to manually reverse the output later.
        for (input_symbol_idx, input_symbol) in input.into_iter().enumerate().rev() {
            for out_symbol in node.edge.unwrap().output().iter().rev() {
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
        Some(&output_buffer[output_buffer_idx..])
    }
}