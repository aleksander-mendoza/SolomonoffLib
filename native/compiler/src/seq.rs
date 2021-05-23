use std::ptr::{Unique, NonNull};
use std::alloc::{Global, Layout, Allocator, handle_alloc_error};
use core::{mem};
use std::fmt::{Display, Formatter, Debug, Pointer};
use exact_size_chars::ExactSizeChars;
use utf8::utf8_is_cont_byte;
use compilation_error::CompErr;
use v::V;
use util::{shrink, allocate, grow};
use util;


/**This is meant to store short sequences. It's specially made for memory-critical
code (such as inductive inference)*/
pub struct Seq<A> {
    content: Unique<A>,
    len: u8,
}

impl<A> Clone for Seq<A> {
    fn clone(&self) -> Self {
        unsafe {
            let ptr = allocate(self.len());
            self.content.as_ptr().copy_to(ptr, self.len());
            Self { content: Unique::new_unchecked(ptr), len: self.len }
        }
    }
}


impl<A> Seq<A> {
    pub fn empty() -> Self {
        Seq { content: Unique::dangling(), len: 0 }
    }
    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }
    pub fn len(&self) -> usize {
        self.len as usize
    }
    pub fn singleton(elem: A) -> Self {
        let arr = [elem];
        Self::from(&arr[..])
    }

    pub fn iter(&self) -> std::slice::Iter<A> {
        self.as_slice().iter()
    }

    pub fn concat(&self, other: &Seq<A>) -> Seq<A> {
        if let Some(len) = self.len.checked_add(other.len) {
            unsafe {
                let ptr = allocate(len as usize);
                self.content.as_ptr().copy_to(ptr, self.len as usize);
                other.content.as_ptr().copy_to(ptr.offset(self.len as isize), other.len as usize);
                Self { content: Unique::new_unchecked(ptr), len }
            }
        } else {
            panic!("Concatenation yields too long sequence");
        }
    }

    pub fn extend(&mut self, other: &Seq<A>) {
        if other.is_empty() { return; }
        unsafe {
            if let Some(len) = self.len.checked_add(other.len) {
                let ptr = if self.is_empty() {
                    allocate(other.len as usize)
                } else {
                    grow(self.content.as_ptr(),self.len(),len as usize)
                };
                other.content.as_ptr().copy_to(ptr.offset(self.len as isize), other.len as usize);
                self.len = len;
                self.content = Unique::new_unchecked(ptr);
            } else {
                panic!("Concatenation yields too long sequence");
            }
        }
    }

    pub fn split(mut self, offset: usize) -> (Self, Self) {
        if offset>self.len(){
            panic!("offset {} out of bounds {}!", offset, self.len());
        }
        if offset == 0 { return (Self::empty(), self); }
        if offset == self.len() { return (self, Self::empty()); }
        unsafe {
            let ptr = self.content.as_ptr();
            let remaining_len = self.len()-offset;
            let ptr1 = allocate(remaining_len);
            ptr.offset(offset as isize).copy_to(ptr1, remaining_len);
            let ptr0 = shrink(ptr,self.len(), offset);
            self.len = offset as u8;
            self.content = Unique::new_unchecked(ptr0);
            (self,Self{content:Unique::new_unchecked(ptr1),len:remaining_len as u8})
        }
    }

    pub fn as_slice(&self) -> &[A] {
        unsafe { std::slice::from_raw_parts(self.content.as_ptr(), self.len as usize) }
    }
}

impl<A: Eq> Seq<A> {
    pub fn lcp(&self, other: &[A]) -> usize {
        let s = self.as_slice();
        let mut i = 0;
        while i < s.len().min(other.len()) && s[i] == other[i] {
            i += 1;
        }
        return i;
    }
}

impl<A> From<&mut [A]> for Seq<A> {
    fn from(s: &mut [A]) -> Self {
        Self::from(s as &[A])
    }
}

impl<A> From<&[A]> for Seq<A> {
    fn from(s: &[A]) -> Self {
        if s.is_empty() {
            return Self::empty();
        }
        if s.len() >= u8::MAX as usize {
            panic!("Sequence is too long!");
        }
        unsafe {
            let bytes = allocate(s.len());
            bytes.copy_from(s.as_ptr(), s.len());
            Self { content: Unique::new_unchecked(bytes), len: s.len() as u8 }
        }
    }
}

impl<A: Debug> Debug for Seq<A> {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        self.as_slice().fmt(f)
    }
}

impl<A> std::ops::Add<&Seq<A>> for &Seq<A> {
    type Output = Seq<A>;

    fn add(self, rhs: &Seq<A>) -> Self::Output {
        self.concat(rhs)
    }
}

impl<A> std::ops::AddAssign<&Seq<A>> for Seq<A> {
    fn add_assign(&mut self, rhs: &Seq<A>) {
        self.extend(rhs)
    }
}

impl<A: PartialEq> PartialEq for Seq<A> {
    fn eq(&self, other: &Self) -> bool {
        if self.len != other.len {
            false
        } else {
            self.as_slice() == other.as_slice()
        }
    }
}

impl<A: Eq> Eq for Seq<A> {}

impl<A> Drop for Seq<A> {
    fn drop(&mut self) {
        if !self.is_empty() {
            unsafe {
                util::drop(self.content.as_ptr(),self.len());
            }
        }
    }
}


#[cfg(test)]
mod tests {
    // Note this useful idiom: importing names from outer (for mod tests) scope.
    use super::*;

    #[test]
    fn test_eq1() {
        assert_eq!(Seq::from("a".as_bytes()), Seq::from("a".as_bytes()));
    }

    #[test]
    fn test_eq2() {
        assert_ne!(Seq::from("aa".as_bytes()), Seq::from("a".as_bytes()));
    }
    #[test]
    fn test_eq3() {
        assert_ne!(Seq::from("".as_bytes()), Seq::from("a".as_bytes()));
    }
    #[test]
    fn test_eq4() {
        assert_ne!(Seq::from("aa".as_bytes()), Seq::from("".as_bytes()));
    }
    #[test]
    fn test_eq5() {
        assert_eq!(Seq::from("".as_bytes()), Seq::from("".as_bytes()));
    }
    #[test]
    fn test_split1() {
        let (pre,post) = Seq::from("ab".as_bytes()).split(1);
        assert_eq!(pre.as_slice(), "a".as_bytes());
        assert_eq!(post.as_slice(), "b".as_bytes());
    }
    #[test]
    fn test_split2() {
        let (pre,post) = Seq::from("abcdefg".as_bytes()).split(4);
        assert_eq!(pre.as_slice(), "abcd".as_bytes());
        assert_eq!(post.as_slice(), "efg".as_bytes());
    }
    #[test]
    fn test_split3() {
        let (pre,post) = Seq::from("abcdefg".as_bytes()).split(0);
        assert_eq!(pre.as_slice(), "".as_bytes());
        assert_eq!(post.as_slice(), "abcdefg".as_bytes());
    }
    #[test]
    fn test_split4() {
        let (pre,post) = Seq::from("abcdefg".as_bytes()).split(7);
        assert_eq!(pre.as_slice(), "abcdefg".as_bytes());
        assert_eq!(post.as_slice(), "".as_bytes());
    }
}