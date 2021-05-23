use std::collections::{HashSet, LinkedList};
use int_queue::IntQueue::{Next, End};
use std::iter::Peekable;

pub struct IntQueueNode(Box<(u32, IntQueue)>);

pub enum IntQueue {
    End,
    Next(IntQueueNode),
}

impl IntQueueNode {
    pub fn borrow(&self) -> &(u32, IntQueue) {
        self.borrow()
    }
    pub fn borrow_mut(&self) -> &mut (u32, IntQueue) {
        self.borrow_mut()
    }
    pub fn as_ptr(&self) -> *const (u32, IntQueue) {
        &*self.0
    }
    pub fn target(&self) -> &IntQueue {
        &self.borrow().1
    }
}

impl IntQueue {
    pub fn has_cycle(&self) -> bool {
        let mut nodes = HashSet::new();
        let mut q = self;
        while let Next(next) = q {
            if nodes.insert(next.as_ptr()) {
                q = next.target();
            } else {
                return true;
            }
        }
        false
    }

    pub fn end() -> IntQueue {
        Self::End
    }

    pub fn cut(&mut self) -> IntQueue {
        let mut q = End;
        unsafe {
            std::mem::swap(&mut q, self);
        }
        q
    }
    pub fn unwrap(&self) -> &IntQueueNode {
        match self {
            Next(n) => { n }
            End => { panic!("attempted to unwrap an end node") }
        }
    }
    pub fn is_end(&self) -> bool {
        match self {
            Next(_) => false,
            End => true
        }
    }
    pub fn unwrap_mut(&mut self) -> &mut IntQueueNode {
        match self {
            Next(n) => { n }
            End => { panic!("attempted to unwrap an end node") }
        }
    }

    pub fn next(val: u32, next: IntQueue) -> IntQueue {
        Self::Next(IntQueueNode(Box::new((val, next))))
    }

    pub fn collect<I>(str: &mut I) -> IntQueue where I: DoubleEndedIterator<Item=u32> {
        let mut q = Self::end();
        for i in str.rev() {
            q = IntQueue::next(i, q);
        }
        q
    }


    // pub fn match_prefix_mut<'a, I>(&'a mut self, s: &mut Peekable<I>) -> &'a mut IntQueue where I: Iterator<Item=u32> {
    //     let mut q = self;
    //     while let (Some(&c), Next(n)) = (s.peek(), q) {
    //         let content: &'a mut (u32, IntQueue) = n.borrow_mut();
    //         if content.0 == c {
    //             let m: &'a mut IntQueue = &mut content.1;
    //             q = m;
    //             s.next();
    //         } else {
    //             return q;
    //         }
    //     }
    //     // assert!(q.is_end()||s.peek().is_none()||q.unwrap().borrow().0!=*s.peek().unwrap());
    //     q
    // }
}


impl<'a> From<&'a str> for IntQueue {
    fn from(str: &'a str) -> IntQueue {
        Self::collect(&mut str.chars().map(|c| c as u32))
    }
}
