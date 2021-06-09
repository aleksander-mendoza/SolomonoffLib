use int_seq::A;
use utf8;
use core::str::next_code_point;

#[derive(Clone)]
pub struct ExactSizeChars<'a> {
    iter: std::slice::Iter<'a, u8>,
    remaining_chars: usize,
}

impl <'a> ExactSizeChars<'a>{
    pub fn new(bytes:&'a[u8], chars:usize)->Self{
        Self{iter:bytes.iter(), remaining_chars:chars}
    }

}

impl <'a> From<&'a str> for ExactSizeChars<'a>{
    fn from(s:&'a str)->Self{
        let remaining_chars = s.chars().count();
        Self{iter:s.as_bytes().iter(), remaining_chars}
    }
}

impl <'a> From<&'a String> for ExactSizeChars<'a>{
    fn from(s:&'a String)->Self{
        let remaining_chars = s.chars().count();
        Self{iter:s.as_bytes().iter(), remaining_chars}
    }
}


impl<'a> Iterator for ExactSizeChars<'a> {
    type Item = A;

    #[inline]
    fn next(&mut self) -> Option<A> {
        if self.remaining_chars > 0 {
            self.remaining_chars -= 1;
        }
        next_code_point(&mut self.iter).map(|ch| {
            // SAFETY: `str` invariant says `ch` is a valid Unicode Scalar Value.
            unsafe { char::from_u32_unchecked(ch) }
        })
    }


    #[inline]
    fn size_hint(&self) -> (usize, Option<usize>) {
        (self.remaining_chars, Some(self.remaining_chars))
    }

    #[inline]
    fn count(self) -> usize {
        // length in `char` is equal to the number of non-continuation bytes
        assert_eq!(self.remaining_chars, self.iter.filter(|&&byte| !utf8::utf8_is_cont_byte(byte)).count());
        self.remaining_chars
    }

    #[inline]
    fn last(mut self) -> Option<A> {
        // No need to go through the entire string.
        self.next_back()
    }
}

impl<'a> ExactSizeIterator for ExactSizeChars<'a> {}

impl<'a> DoubleEndedIterator for ExactSizeChars<'a> {

    #[inline]
    fn next_back(&mut self) -> Option<A> {
        if self.remaining_chars > 0 {
            self.remaining_chars -= 1;
        }
        utf8::next_code_point_reverse(&mut self.iter).map(|ch| {
            // SAFETY: `str` invariant says `ch` is a valid Unicode Scalar Value.
            unsafe { char::from_u32_unchecked(ch) }
        })
    }
}