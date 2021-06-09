use std::ptr::{Unique};
use core::{mem};
use std::fmt::{Display, Formatter, Debug, Pointer};
use exact_size_chars::ExactSizeChars;
use utf8::utf8_is_cont_byte;
use compilation_error::CompErr;
use v::V;
use util::{shrink, allocate, grow};
use util;
use std::convert::TryFrom;
use std::str::Utf8Error;
use std::ops::{Index, IndexMut};
use std::mem::ManuallyDrop;


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
impl <A> Index<usize> for Seq<A>{
    type Output = A;

    fn index(&self, index: usize) -> &Self::Output {
       &self.as_slice()[index]
    }
}
impl <A> IndexMut<usize> for Seq<A>{
    fn index_mut(&mut self, index: usize) -> &mut Self::Output {
        &mut self.as_mut_slice()[index]
    }
}
impl<A> Seq<A> {
    fn from_iter<I>(s:I) -> Self where I:Iterator<Item=A>+ExactSizeIterator{
        let len = s.len();
        if len==0 {
            return Self::empty();
        }
        if let Ok(len8) = u8::try_from(len) {
            unsafe {
                let bytes = allocate(len);
                for (i, a) in s.enumerate() {
                    std::ptr::write(bytes.offset(i as isize), a);
                }
                Self { content: Unique::new_unchecked(bytes), len: len8 }
            }
        }else{
            panic!("Sequence is too long!");
        }
    }

    pub fn filled<F:Fn(usize)->A>(val:F,len:usize)->Self{
        if let Ok(len8) = u8::try_from(len) {
            unsafe {
                let ptr = allocate(len);
                for offset in 0..len {
                    std::ptr::write(ptr.offset(offset as isize), val(offset));
                }

                Self { content: Unique::new_unchecked(ptr), len:len8 }
            }
        }else{
            panic!("Sequence too large: {}",len);
        }
    }
    pub unsafe fn with_len(len:u8)->Self{
        Self { content: Unique::new_unchecked(allocate(len as usize)), len }
    }

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
        Self::from_iter(std::iter::once(elem))
    }

    pub fn iter(&self) -> std::slice::Iter<A> {
        self.as_slice().iter()
    }
    pub fn iter_mut(&mut self) -> std::slice::IterMut<A> {
        self.as_mut_slice().iter_mut()
    }
    pub fn concat(&self, other: &Seq<A>) -> Seq<A> {
        self.concat_slice(other.as_slice())
    }

    pub fn concat_slice(&self, other: &[A]) -> Seq<A> {
        Self::concat_slices(self.as_slice(),other)
    }

    pub fn concat_slices(lhs:&[A], rhs: &[A]) -> Seq<A> {
        if let Ok(len) = u8::try_from(lhs.len()+rhs.len()) {
            unsafe {
                let ptr = allocate(len as usize);
                lhs.as_ptr().copy_to(ptr, lhs.len());
                rhs.as_ptr().copy_to(ptr.offset(lhs.len() as isize), rhs.len());
                Self { content: Unique::new_unchecked(ptr), len }
            }
        } else {
            panic!("Concatenation yields too long sequence");
        }
    }

    pub fn extend_slice(&mut self, other: &[A]) {
        if other.is_empty() { return; }
        if let Some(len) = self.len.checked_add(u8::try_from(other.len()).unwrap()) {
            unsafe {
                let ptr = if self.is_empty() {
                    allocate(other.len())
                } else {
                    grow(self.content.as_ptr(), self.len(), len as usize)
                };
                other.as_ptr().copy_to(ptr.offset(self.len as isize), other.len());
                self.len = len;
                self.content = Unique::new_unchecked(ptr);
            }
        } else {
            panic!("Concatenation yields too long sequence");
        }
    }

    pub fn extend(&mut self, other: &Seq<A>) {
        self.extend_slice(other.as_slice())
    }
    pub fn cut_off(&mut self, offset: usize) -> Self {
        if offset > self.len() {
            panic!("offset {} out of bounds {}!", offset, self.len());
        }
        if offset == 0 {
            let mut moved = Self::empty();
            mem::swap(&mut moved, self);
            return moved;
        }
        if offset == self.len() {
            return Self::empty();
        }
        unsafe {
            let ptr = self.content.as_ptr();
            let remaining_len = self.len() - offset;
            let ptr1 = allocate(remaining_len);
            ptr.offset(offset as isize).copy_to(ptr1, remaining_len);
            let ptr0 = shrink(ptr, self.len(), offset);
            self.len = offset as u8;
            self.content = Unique::new_unchecked(ptr0);
            Self { content: Unique::new_unchecked(ptr1), len: remaining_len as u8 }
        }
    }
    pub fn prepend(&mut self, prefix: &Self) {
        self.prepend_slice(prefix.as_slice())
    }
    pub fn prepend_slice(&mut self, prefix: &[A]) {
        if prefix.is_empty() { return; }
        let mut concatenated = Self::concat_slices(prefix,self.as_slice());
        std::mem::swap(self,&mut concatenated)
    }
    pub fn split(mut self, offset: usize) -> (Self, Self) {
        let suffix = self.cut_off(offset);
        (self, suffix)
    }

    pub fn as_slice(&self) -> &[A] {
        unsafe { std::slice::from_raw_parts(self.content.as_ptr(), self.len as usize) }
    }

    pub fn as_mut_slice(&mut self) -> &mut [A] {
        unsafe { std::slice::from_raw_parts_mut(self.content.as_ptr(), self.len as usize) }
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
pub struct IntoIter<A>{
    content: Unique<A>,
    len: isize,
    offset: isize
}
impl <A> Iterator for IntoIter<A>{
    type Item = A;

    fn next(&mut self) -> Option<Self::Item> {
        if self.offset == self.len{
            None
        }else if mem::size_of::<A>() == 0 {
            // Make up a value of this ZST.
            self.offset += 1;
            Some(unsafe { mem::zeroed() })
        } else {
            let e= unsafe { std::ptr::read(self.content.as_ptr().offset(self.offset)) };
            self.offset += 1;
            Some(e)
        }
    }
}
impl <A> Drop for IntoIter<A>{
    fn drop(&mut self) {
        unsafe{
            util::drop_shallow(self.content.as_ptr(),self.len as usize);
        }
    }
}
impl <A> IntoIterator for Seq<A>{
    type Item = A;
    type IntoIter = IntoIter<A>;

    fn into_iter(self) -> Self::IntoIter {
        let mut me = ManuallyDrop::new(self);
        IntoIter{content:me.content,len:me.len.clone() as isize,offset:0}
    }
}
impl<A:Clone> From<&mut [A]> for Seq<A> {
    fn from(s: &mut [A]) -> Self {
        Self::from(s as &[A])
    }
}

impl<A:Clone> From<&[A]> for Seq<A> {
    fn from(s: &[A]) -> Self {
        Self::from_iter(s.into_iter().cloned())
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

impl<A> std::ops::Add<&[A]> for &Seq<A> {
    type Output = Seq<A>;

    fn add(self, rhs: &[A]) -> Self::Output {
        self.concat_slice(rhs)
    }
}

impl<A> std::ops::AddAssign<&Seq<A>> for Seq<A> {
    fn add_assign(&mut self, rhs: &Seq<A>) {
        self.extend(rhs)
    }
}

impl<A> std::ops::AddAssign<&[A]> for Seq<A> {
    fn add_assign(&mut self, rhs: &[A]) {
        self.extend_slice(rhs)
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
                util::drop_deep(self.content.as_ptr(), self.len());
            }
        }
    }
}
impl Seq<u8>{
    pub fn as_str(&self) -> Result<&str, Utf8Error> {
        std::str::from_utf8(self.as_slice())
    }
}
impl Display for Seq<u8>{
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        self.as_str().fmt(f)
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
        let (pre, post) = Seq::from("ab".as_bytes()).split(1);
        assert_eq!(pre.as_slice(), "a".as_bytes());
        assert_eq!(post.as_slice(), "b".as_bytes());
    }

    #[test]
    fn test_split2() {
        let (pre, post) = Seq::from("abcdefg".as_bytes()).split(4);
        assert_eq!(pre.as_slice(), "abcd".as_bytes());
        assert_eq!(post.as_slice(), "efg".as_bytes());
    }

    #[test]
    fn test_split3() {
        let (pre, post) = Seq::from("abcdefg".as_bytes()).split(0);
        assert_eq!(pre.as_slice(), "".as_bytes());
        assert_eq!(post.as_slice(), "abcdefg".as_bytes());
    }

    #[test]
    fn test_split4() {
        let (pre, post) = Seq::from("abcdefg".as_bytes()).split(7);
        assert_eq!(pre.as_slice(), "abcdefg".as_bytes());
        assert_eq!(post.as_slice(), "".as_bytes());
    }

    #[test]
    fn test_drop1() {
        let mut x = 0;
        struct X<'x>{
            x:&'x mut i32
        }
        impl <'a> Drop for X<'a>{
            fn drop(&mut self) {
                *self.x+=1;
            }
        }
        {
            let arr = Seq::singleton(X { x: &mut x });
        }
        assert_eq!(x,1);
    }

    #[test]
    fn test_drop2() {
        let mut x = 0;
        struct X{
            x:*mut i32
        }
        impl Drop for X{
            fn drop(&mut self) {
                unsafe{
                    *self.x+=1;
                }
            }
        }
        {
            let vec = vec![X { x:  &mut x },X { x:  &mut x }];
            let arr = Seq::from_iter(vec.into_iter());
            for (i,e) in arr.into_iter().enumerate(){
                assert_eq!(x,i as i32);
            }
        }
        assert_eq!(x,2);
    }
}