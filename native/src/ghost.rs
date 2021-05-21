use std::collections::HashSet;
use n::N;
use std::cell::RefCell;

/**The way Solomonoff handles graphs G is by dynamically allocating
instances of nodes N that hold arrays pointers to other nodes.
Hence the graph is singly linked. Unfortunately such a data structure
cannot be handled by Rusts borrow checker. The memory management
must be done manually. This Ghost data structure is meant to work like
a simplified garbage collector or memory pool. Of course, there is no
garbage collection in production code, we only use it in testing
environment (hence the conditional compilation with no_ghosts flag).
Essentially it is an array keeping track of all allocated nodes N.
Allocation and deallocation can only be done via N::new and N::delete
which take Ghost as argument and insert/remove nodes from the pool.
During testing we could then easily verify if no memory leaks occurred
by inspecting this pool and making sure it's empty.*/
#[cfg(not(no_ghosts))]
pub struct Ghost {
    n: RefCell<HashSet<*mut N>>
}


#[cfg(not(no_ghosts))]
impl Ghost {
    pub fn new() -> Ghost {
        Ghost { n: RefCell::new(HashSet::new()) }
    }
    pub fn new_n(&self, n: *mut N) -> () {
        assert!(!self.contains_n(n));
        #[cfg(not(quiet))] {
            println!("Create {:p}:{:?}", n, unsafe { (*n).get_outgoing() });
        }
        self.n.borrow_mut().insert(n);
    }
    pub fn delete_n(&self, n: *mut N) -> () {
        #[cfg(not(quiet))] {
            println!("Delete {:p}", n);
        }
        assert!(self.contains_n(n));
        self.n.borrow_mut().remove(&n);
    }
    pub fn contains_n(&self, n: *mut N) -> bool {
        self.n.borrow().contains(&n)
    }

    pub fn is_empty(&self) -> bool {
        self.n.borrow().is_empty()
    }
}


#[cfg(no_ghosts)]
#[derive(Copy, Clone)]
pub struct Ghost {}

#[cfg(no_ghosts)]
impl Ghost {
    pub fn new() -> Ghost {
        Ghost {}
    }
    pub fn new_n(&self, n: *mut N) -> () {}
    pub fn delete_n(&self, n: *mut N) -> () {}
    pub fn contains_n(&self, n: *mut N) -> bool {
        true
    }
    pub fn is_empty(&self) -> bool {
        true
    }
}

impl Ghost {
    pub fn with_mock<Y, F: FnOnce(&Ghost) -> Y>(f: F) -> Y {
        let ghost = Ghost::new();
        let y = f(&ghost);
        assert!(ghost.is_empty());
        y
    }
}