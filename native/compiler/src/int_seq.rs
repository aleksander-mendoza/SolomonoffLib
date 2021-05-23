use std::ptr::{Unique, NonNull};
use std::alloc::{Global, Layout, Allocator, handle_alloc_error};
use core::{mem};
use std::fmt::{Display, Formatter, Debug};
use exact_size_chars::ExactSizeChars;
use utf8::utf8_is_cont_byte;
use compilation_error::CompErr;
use v::V;
use util::{allocate, grow};
use ranged_serializers::{unescape_u32, unescape_u8};

pub type A = u32; // Sigma set/Alphabet character/Symbol type

pub const REFLECT: A = 0;


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
            let ptr = allocate(self.bytes_len as usize);
            self.content.as_ptr().copy_to(ptr, self.bytes_len as usize);
            Self { content: Unique::new_unchecked(ptr), bytes_len: self.bytes_len, chars_len: self.chars_len }
        }
    }
}


impl IntSeq {
    pub const EPSILON: IntSeq = IntSeq { content: Unique::dangling(), bytes_len: 0, chars_len: 0 };
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
                let ptr = allocate(bytes_len as usize);
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
                let ptr = if self.is_empty() {
                    allocate(other.bytes_len as usize)
                } else {
                    grow(self.content.as_ptr(),self.bytes_len as usize,bytes_len as usize)
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

    pub fn bytes_len(&self) -> usize {
        self.bytes_len as usize
    }

    pub fn from_literal(pos:V,s: &str) -> Result<Self,CompErr> {
        let bytes_len = s.len();
        if bytes_len == 0 {
            return Ok(Self::EPSILON);
        }
        let mut bytes_len = 0;
        let mut i =0 ;
        let mut chars_len = 0;
        while i < s.len(){
            if !utf8_is_cont_byte(s.as_bytes()[i]){
                chars_len+=1
            }
            if s.as_bytes()[i] == '\\' as u8{
                i+=2;
            }else{
                i+=1;
            }
            bytes_len+=1;
        }
        let bytes_len = bytes_len;
        let chars_len = chars_len;
        if i > s.len(){
            return Err(CompErr::Parse(pos,String::from("String literal has dangling backslash")));
        }
        if bytes_len >= u16::MAX as usize {
            panic!("String '{}' is too long!", s);
        }

        unsafe {
            let bytes = allocate(bytes_len);
            let mut i = 0;
            let mut byte_i = 0;
            while i < s.len(){
                let next_byte = if s.as_bytes()[i] == '\\' as u8{
                    i+=1;
                    unescape_u8(s.as_bytes()[i])
                }else{
                    s.as_bytes()[i]
                };
                bytes.offset(byte_i).write(next_byte);
                byte_i+=1;
                i+=1;
            }
            assert_eq!(byte_i, bytes_len as isize);
            assert_eq!(i, s.len());
            Ok(Self {
                content: Unique::new_unchecked(bytes),
                bytes_len: bytes_len as u16,
                chars_len: chars_len as u16
            })
        }
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
            return Self::EPSILON;
        }
        if bytes_len >= u16::MAX as usize {
            panic!("String '{}' is too long!", s);
        }
        let chars_len = s.chars().count();
        unsafe {
            let bytes = allocate(bytes_len);
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

impl Drop for IntSeq {
    fn drop(&mut self) {
        if !self.is_empty(){
            unsafe {
                let c: NonNull<u8> = self.content.into();
                Global.deallocate(c,Layout::array::<u8>(self.bytes_len as usize).unwrap());
            }
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
        assert_eq!(IntSeq::from(""), IntSeq::EPSILON);
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
    #[test]
    fn test_literal1() {
        let mut a = IntSeq::from_literal(V::UNKNOWN,"\\n\\t\\r\\\\\\b\\f\\0");
        let a = a.unwrap();
        let mut exp = String::from("\n\t\r\\");
        exp.push(7 as char);
        exp.push(12 as char);
        exp.push(0 as char);
        assert_eq!(a.as_bytes(), exp.as_bytes());
    }
    #[test]
    fn test_literal2() {
        let mut a = IntSeq::from_literal(V::UNKNOWN,"a\\n b \\t c \\r d \\\\ e \\b f \\f g \\0 h");
        let a = a.unwrap();
        let mut exp = String::from("a\n b \t c \r d \\ e ");
        exp.push(7 as char);
        exp.push_str(" f ");
        exp.push(12 as char);
        exp.push_str(" g \0 h");
        assert_eq!(a.as_bytes(), exp.as_bytes());
    }

    #[test]
    fn test_literal3() {
        let mut a = IntSeq::from_literal(V::UNKNOWN,"\\a\\n\\ b\\ \\t\\ \\c\\ \\r d \\\\ e \\b f \\f g \\0 h");
        let a = a.unwrap();
        let mut exp = String::from("a\n b \t c \r d \\ e ");
        exp.push(7 as char);
        exp.push_str(" f ");
        exp.push(12 as char);
        exp.push_str(" g \0 h");
        assert_eq!(a.as_bytes(), exp.as_bytes());
    }
}