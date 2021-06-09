use ranged_graph::{RangedGraph, NonSink, Trans};
use p::PartialEdge;
use int_seq::{REFLECT, A, IntSeq};


pub struct StateToIndexTable(Vec<u8>);

impl StateToIndexTable {
    pub fn new(size: usize) -> Self {
        let mut v = Vec::with_capacity(size);
        unsafe { v.set_len(size) };
        Self(v)
    }
    pub fn ensure_length(&mut self, len:usize) {
        if len>self.0.len(){
            self.0.reserve(len-self.0.len())
        }
        unsafe { self.0.set_len(self.0.capacity()) };
    }
}

pub fn new_output_buffer(size: usize) -> Vec<u32> {
    let mut v = Vec::with_capacity(size);
    unsafe { v.set_len(size) };
    v
}
impl<Tr: Trans> RangedGraph<Tr> {

    pub fn make_state_to_index_table(&self) -> StateToIndexTable {
        StateToIndexTable::new(self.len())
    }
    pub fn ensure_length(&self, state_to_index:&mut StateToIndexTable) {
        state_to_index.ensure_length(self.len())
    }
    pub fn evaluate_to_string<I>(&self, input: I) -> Option<String>
        where  I: DoubleEndedIterator<Item=A> + Clone{
        let mut state_to_index = self.make_state_to_index_table();
        self.evaluate_tabular(&mut state_to_index,input).map(|v|{
            let s:String = v.into_iter().rev().collect();
            s
        })
    }
    pub fn evaluate_to_int_seq<I>(&self, input: I) -> Option<IntSeq>
        where  I: DoubleEndedIterator<Item=A> + Clone{
        let mut state_to_index = self.make_state_to_index_table();
        self.evaluate_tabular(&mut state_to_index,input).map(|v|IntSeq::from_iter(v.into_iter().rev()))
    }

    pub fn evaluate_tabular<I>(&self, state_to_index: &mut StateToIndexTable, input: I) -> Option<Vec<A>>
        where  I: DoubleEndedIterator<Item=A> + Clone {
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
        let mut backtracking_table = Vec::<Vec<BacktrackingNode<Tr>>>::with_capacity(input.size_hint().0*4+1);
        // This vector encodes sparse set of currently active states (current configuartion of states).
        let mut prev_column = vec![init_node];

        for input_symbol in input.clone() {
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
        let mut output_buffer = Vec::<A>::with_capacity(512);
        // Now we collect the output from final accepting state. The string
        // is printed in reverse
        for out_symbol in fin_edge.output().iter().rev() {
            if out_symbol != REFLECT {
                output_buffer.push(out_symbol);
            }
        }
        // Now we collect the output from every traversed transition but we go
        //in reverse order and we print revered strings. Everything is inserted
        //into the output buffer from the end, hence we end up with the correct result
        //and we don't need to manually reverse the output later.
        let mut input_symbol_idx = backtracking_table.len()-1;
        for input_symbol in input.rev() {
            for out_symbol in node.edge.unwrap().output().iter().rev() {
                output_buffer.push(if out_symbol == REFLECT { input_symbol } else { out_symbol });
            }
            assert!(input_symbol_idx>0);
            input_symbol_idx -= 1;
            let column = &backtracking_table[input_symbol_idx];
            node = &column[node.prev_index as usize];
        }
        assert_eq!(input_symbol_idx,0);
        Some(output_buffer)
    }
}