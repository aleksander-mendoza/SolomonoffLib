use ranged_graph::{RangedGraph, Transition};
use v::V;
use ranged_evaluation::StateToIndexTable;
use int_seq::A;

pub enum Pipeline{
    Automaton(RangedGraph<Transition>,V),
    Alternative(Box<Pipeline>,Box<Pipeline>,V),
    Composition(Box<Pipeline>,Box<Pipeline>,V),
}


impl Pipeline{

    pub fn new_automaton(r:RangedGraph<Transition>,pos:V)->Self{
        Self::Automaton(r,pos)
    }

    pub fn new_alternative(lhs:Pipeline,rhs:Pipeline,pos:V)->Self{
        Self::Alternative(Box::new(lhs),Box::new(rhs),pos)
    }

    pub fn new_composition(lhs:Pipeline,rhs:Pipeline,pos:V)->Self{
        Self::Composition(Box::new(lhs),Box::new(rhs),pos)
    }

    // pub fn eval_tabular(&self, state_to_index: &mut StateToIndexTable, output_buffer: &'b mut [A], input: &') -> Option<&'b [A]>{
    //
    // }
}