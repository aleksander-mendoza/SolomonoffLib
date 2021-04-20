use e::E;
use p::P;
use v::V;

pub struct RangedGraph {
    graph: Vec<Vec<(u32, Vec<(E,i64)>)>>,
    accepting: Vec<Option<P>>,
    index_to_state: Vec<V>,
    initial: i64,
}

impl RangedGraph {
    pub fn new(graph: Vec<Vec<(u32, Vec<(E,i64)>)>>,
           accepting: Vec<Option<P>>,
           index_to_state: Vec<V>,
           initial: i64) -> Self {
        assert_eq!(graph.len(), accepting.len());
        assert_eq!(graph.len(), index_to_state.len());
        assert!(initial < graph.len() as i64);
        RangedGraph { graph, accepting, index_to_state, initial }
    }

    pub fn init(&self)->i64{
        self.initial
    }

    pub fn graph(&self)->&Vec<Vec<(u32, Vec<(E,i64)>)>>{
        &self.graph
    }

    pub fn accepting(&self, index:usize)->&Option<P>{
        &self.accepting[index]
    }

    pub fn index_to_state(&self, index:usize)->&V{
        &self.index_to_state[index]
    }
}
