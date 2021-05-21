use std::ptr::{Unique, NonNull};
use std::alloc::{Global, Layout, Allocator, handle_alloc_error};
use core::{mem};
use std::fmt::{Display, Formatter, Debug};
use exact_size_chars::ExactSizeChars;

pub type A = u32; // Sigma set/Alphabet character/Symbol type

pub const REFLECT: A = 0;
pub const EPSILON: IntSeq = IntSeq { content: Unique::dangling(), bytes_len: 0, chars_len: 0 };

pub struct IntSeq {
    content: Unique<u8>,
    bytes_len: u16,
    // realistically this should be more than enough to store any string
    chars_len: u16, // on any transition of automaton. User might not always be able
    // to read large files as input to automaton, but it shouldn't matter as for such
    // operations a different data structure should be used.
}

impl Clone for IntSeq {
    fn clone(&self) -> Self {
        unsafe {
            let ptr = allocate_bytes(self.bytes_len as usize);
            self.content.as_ptr().copy_to(ptr, self.bytes_len as usize);
            Self { content: Unique::new_unchecked(ptr), bytes_len: self.bytes_len, chars_len: self.chars_len }
        }
    }
}

unsafe fn allocate_bytes(bytes_len: usize) -> *mut u8 {
    let ptr = Global.allocate(Layout::array::<u8>(bytes_len).unwrap());
    if ptr.is_err() {
        handle_alloc_error(Layout::from_size_align_unchecked(
            bytes_len,
            mem::align_of::<u8>(),
        ))
    }
    ptr.unwrap().as_ptr() as *mut u8
}

impl IntSeq {
    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }
    pub fn len(&self) -> usize {
        self.chars_len as usize
    }
    pub fn singleton(elem:A) -> Option<Self> {
        char::from_u32(elem).map(|c|{
            let mut tmp = [0; 4];
            Self::from(c.encode_utf8(&mut tmp))
        })
    }

    pub fn iter(&self) -> ExactSizeChars {
        let slice = unsafe { std::slice::from_raw_parts(self.content.as_ptr(), self.bytes_len as usize) };
        ExactSizeChars::new(slice, self.chars_len as usize)
    }

    pub fn concat(&self, other: &IntSeq) -> IntSeq {
        if let Some(bytes_len) = self.bytes_len.checked_add(other.bytes_len) {
            unsafe {
                let ptr = allocate_bytes(bytes_len as usize);
                self.content.as_ptr().copy_to(ptr, self.bytes_len as usize);
                other.content.as_ptr().copy_to(ptr.offset(self.bytes_len as isize), other.bytes_len as usize);
                let chars_len = self.chars_len + other.chars_len;
                Self { content: Unique::new_unchecked(ptr), bytes_len, chars_len }
            }
        } else {
            panic!("Concatenation of '{}' and '{}' yields too long string", self, other);
        }
    }

    pub fn extend(&mut self, other: &IntSeq) {
        if other.is_empty() { return; }
        unsafe {
            if let Some(bytes_len) = self.bytes_len.checked_add(other.bytes_len) {
                let chars_len = self.chars_len + other.chars_len;
                let c: NonNull<u8> = self.content.into();
                let ptr = if self.is_empty() {
                    allocate_bytes(other.bytes_len as usize)
                } else {
                    let ptr = Global.grow(c.cast(),
                                          Layout::array::<u8>(self.bytes_len as usize).unwrap(),
                                          Layout::array::<u8>(bytes_len as usize).unwrap());
                    if ptr.is_err() {
                        handle_alloc_error(Layout::from_size_align_unchecked(
                            bytes_len as usize,
                            mem::align_of::<u8>(),
                        ))
                    }
                    ptr.unwrap().as_ptr() as *mut u8
                };
                other.content.as_ptr().copy_to(ptr.offset(self.bytes_len as isize), other.bytes_len as usize);
                self.bytes_len = bytes_len;
                self.chars_len = chars_len;
                self.content = Unique::new_unchecked(ptr);
            } else {
                panic!("Concatenation of '{}' and '{}' yields too long string", self, other);
            }
        }
    }

    pub fn as_bytes(&self) -> &[u8] {
        unsafe { std::slice::from_raw_parts(self.content.as_ptr(), self.bytes_len as usize) }
    }

    pub fn as_str(&self) -> &str {
        unsafe { std::str::from_utf8_unchecked(self.as_bytes()) }
    }
}
impl From<&mut str> for IntSeq {
    fn from(s: &mut str) -> Self {
        Self::from(s as &str)
    }
}
impl From<&str> for IntSeq {
    fn from(s: &str) -> Self {
        let bytes_len = s.len();
        if bytes_len == 0 {
            return EPSILON;
        }
        if bytes_len >= u16::MAX as usize {
            panic!("String '{}' is too long!", s);
        }
        let chars_len = s.chars().count();
        unsafe {
            let bytes = allocate_bytes(bytes_len);
            bytes.copy_from(s.as_bytes().as_ptr(), s.len());
            Self { content: Unique::new_unchecked(bytes), bytes_len: bytes_len as u16, chars_len: chars_len as u16 }
        }
    }
}


impl<'a> IntoIterator for &'a IntSeq {
    type Item = A;
    type IntoIter = ExactSizeChars<'a>;

    fn into_iter(self) -> Self::IntoIter {
        self.iter()
    }
}

impl Debug for IntSeq {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.as_str())
    }
}

impl Display for IntSeq {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.as_str())
    }
}

impl std::ops::Add<&IntSeq> for &IntSeq {
    type Output = IntSeq;

    fn add(self, rhs: &IntSeq) -> Self::Output {
        self.concat(rhs)
    }
}

impl std::ops::AddAssign<&IntSeq> for IntSeq {
    fn add_assign(&mut self, rhs: &IntSeq) {
        self.extend(rhs)
    }
}

impl PartialEq for IntSeq {
    fn eq(&self, other: &Self) -> bool {
        if self.chars_len != other.chars_len {
            false
        } else {
            self.as_bytes() == other.as_bytes()
        }
    }
}

impl Eq for IntSeq {}

#[cfg(test)]
mod tests {
    // Note this useful idiom: importing names from outer (for mod tests) scope.
    use super::*;

    #[test]
    fn test_eq1() {
        assert_eq!(IntSeq::from("a"), IntSeq::from("a"));
    }

    #[test]
    fn test_eq2() {
        assert_eq!(IntSeq::from("ab"), IntSeq::from("ab"));
    }

    #[test]
    fn test_eq3() {
        assert_eq!(IntSeq::from("😀"), IntSeq::from("😀"));
    }

    #[test]
    fn test_eq4() {
        assert_eq!(IntSeq::from("Emoji ☺ 😂 🚀 😘 😈"), IntSeq::from("Emoji ☺ 😂 🚀 😘 😈"));
    }

    #[test]
    fn test_eq5() {
        assert_eq!(IntSeq::from(""), EPSILON);
    }

    #[test]
    fn test_ne1() {
        assert_ne!(IntSeq::from("ab"), IntSeq::from("b"));
    }

    #[test]
    fn test_ne2() {
        assert_ne!(IntSeq::from(""), IntSeq::from("b"));
    }

    #[test]
    fn test_add1() {
        assert_eq!(IntSeq::from("ab"), &IntSeq::from("a") + &IntSeq::from("b"));
    }

    #[test]
    fn test_add2() {
        assert_eq!(IntSeq::from("b"), &IntSeq::from("") + &IntSeq::from("b"));
    }

    #[test]
    fn test_add3() {
        assert_eq!(IntSeq::from("a"), &IntSeq::from("a") + &IntSeq::from(""));
    }

    #[test]
    fn test_add4() {
        let mut a = IntSeq::from("a");
        a += &IntSeq::from("b");
        assert_eq!(IntSeq::from("ab"), a);
    }

    #[test]
    fn test_add5() {
        let mut a = IntSeq::from("a");
        a += &IntSeq::from("");
        assert_eq!(IntSeq::from("a"), a);
    }

    #[test]
    fn test_add6() {
        let mut a = IntSeq::from("");
        a += &IntSeq::from("b");
        assert_eq!(IntSeq::from("b"), a);
    }

    #[test]
    fn test_clone1() {
        let mut a = IntSeq::from("");
        assert_eq!(a, a.clone());
    }

    #[test]
    fn test_clone2() {
        let mut a = IntSeq::from("advrv");
        assert_eq!(a, a.clone());
    }

    #[test]
    fn test_clone3() {
        let mut a = IntSeq::from("Emoji ☺ 😂 🚀 😘 😈");
        assert_eq!(a, a.clone());
    }
}