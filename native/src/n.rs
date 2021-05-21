use e::E;
use ghost::Ghost;
use v::V;

pub struct N {
    meta: V,
    outgoing: Vec<(E, *mut N)>,
}

impl N {
    pub fn get_meta(&self)-> &V{
        &self.meta
    }
    pub fn get_outgoing(&self)->&Vec<(E, *mut N)>{
        &self.outgoing
    }
    pub fn get_outgoing_mut(&mut self)->&mut Vec<(E, *mut N)>{
        &mut self.outgoing
    }
    pub fn new(meta: V,ghost:&Ghost) -> *mut N {
        Self::with_capacity(2,meta,ghost)
    }
    pub fn with_capacity(cap:usize,meta: V,ghost:&Ghost) -> *mut N {
        let n = Box::into_raw(Box::new(N { outgoing: Vec::with_capacity(cap), meta }));
        ghost.new_n(n);
        n
    }
    pub fn delete(n:*mut N,ghost:&Ghost)->(){
        assert!(ghost.contains_n(n));
        unsafe {
            ghost.delete_n(n);
            drop(Box::from_raw(n));
        }
    }

    pub fn shallow_copy(original: *mut N,ghost: &Ghost) -> *mut N {
        N::with_capacity(unsafe { (*original).outgoing.len() },
                         unsafe { (*original).meta.clone() },
                         ghost)
    }
    pub fn meta(n:*mut N, ghost:&Ghost)-> &'static V{
        unsafe { &(*n).meta }
    }
    pub fn push(n:*mut N,edge:(E, *mut N),ghost:&Ghost)->(){
        assert!(ghost.contains_n(n));
        N::outgoing_mut(n,ghost).push(edge);
    }
    pub fn outgoing(n:*mut N, ghost:&Ghost) -> &'static Vec<(E, *mut N)> {
        assert!(ghost.contains_n(n));
        unsafe { &(*n).outgoing }
    }
    pub fn outgoing_mut(n:*mut N, ghost:&Ghost) -> &'static mut Vec<(E, *mut N)> {
        assert!(ghost.contains_n(n));
        unsafe { &mut (*n).outgoing }
    }
}