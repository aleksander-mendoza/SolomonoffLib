use std::collections::HashSet;
use n::N;
use std::cell::RefCell;

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
        self.n.borrow_mut().insert(n);
    }
    pub fn delete_n(&self, n: *mut N) -> () {
        assert!(self.contains_n(n));
        self.n.borrow_mut().remove(&n);
    }
    pub fn contains_n(&self, n: *mut N) -> bool {
        self.n.borrow().contains(&n)
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
}
