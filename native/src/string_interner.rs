use std::num::NonZeroU128;
use int_seq::IntSeq;
use string_interner::Str::Code;

pub enum Str<'a> {
    Code(NonZeroU128),
    Str(&'a [u32]),
}

pub const INTERN_ZERO: u32 = 0;
pub const INTERN_NL: u32 = INTERN_ZERO + 1;
pub const INTERN_TAB: u32 = INTERN_NL + 1;
pub const INTERN_SPACE: u32 = INTERN_TAB + 1;
pub const INTERN_LOWER_A: u32 = INTERN_SPACE + 1;
pub const INTERN_LOWER_Z: u32 = INTERN_LOWER_A + ('z' - 'a');
pub const INTERN_UPPER_A: u32 = INTERN_LOWER_Z + 1;
pub const INTERN_UPPER_Z: u32 = INTERN_UPPER_A + ('Z' - 'A');
pub const INTERN_0: u32 = INTERN_UPPER_Z + 1;
pub const INTERN_9: u32 = INTERN_0 + ('9' - '0');
pub const INTERN_SPECIAL_1: u32 = INTERN_9 + 1;
pub const INTERN_SPECIAL_2: u32 = INTERN_SPECIAL_1 + ('/' - '!');
pub const INTERN_SPECIAL_3: u32 = INTERN_SPECIAL_2 + 1;
pub const INTERN_SPECIAL_4: u32 = INTERN_SPECIAL_3 + ('@' - ':');
pub const INTERN_SPECIAL_5: u32 = INTERN_SPECIAL_4 + 1;
pub const INTERN_SPECIAL_6: u32 = INTERN_SPECIAL_5 + ('`' - '[');
pub const INTERN_SPECIAL_7: u32 = INTERN_SPECIAL_6 + 1;
pub const INTERN_SPECIAL_8: u32 = INTERN_SPECIAL_7 + ('~' - '{');
pub const INTERN_SPECIAL_9: u32 = INTERN_SPECIAL_8 + 1;
pub const INTERN_SPECIAL_10: u32 = INTERN_SPECIAL_9 + (31 - 1);
pub const INTERN_SPECIAL_DEL: u32 = INTERN_SPECIAL_10 + 1;


pub const ASCII_ZERO: u32 = '\0' as u32;
pub const ASCII_NL: u32 = '\n' as u32;
pub const ASCII_TAB: u32 = '\t' as u32;
pub const ASCII_SPACE: u32 = ' ' as u32;
pub const ASCII_LOWER_A: u32 = 'a' as u32;
pub const ASCII_LOWER_Z: u32 = 'z' as u32;
pub const ASCII_UPPER_A: u32 = 'A' as u32;
pub const ASCII_UPPER_Z: u32 = 'Z' as u32;
pub const ASCII_0: u32 = '0' as u32;
pub const ASCII_9: u32 = '9' as u32;
pub const ASCII_SPECIAL_1: u32 = '!' as u32;
pub const ASCII_SPECIAL_2: u32 = '/' as u32;
pub const ASCII_SPECIAL_3: u32 = ':' as u32;
pub const ASCII_SPECIAL_4: u32 = '@' as u32;
pub const ASCII_SPECIAL_5: u32 = '[' as u32;
pub const ASCII_SPECIAL_6: u32 = '`' as u32;
pub const ASCII_SPECIAL_7: u32 = '{' as u32;
pub const ASCII_SPECIAL_8: u32 = '~' as u32;
pub const ASCII_SPECIAL_9: u32 = 1;
pub const ASCII_SPECIAL_10: u32 = 31;
pub const ASCII_SPECIAL_DEL: u32 = 127;

pub struct Interner {
    pool: Vec<u32>
}

impl Interner {
    // /**The order of ASCII symbols is fucking stupid. I'll rearrange it better*/
    // fn ascii_to_intern(ascii: u32) -> u32 {
    //     match ascii {
    //         ASCII_ZERO => INTERN_ZERO,
    //         ASCII_NL => INTERN_NL,
    //         ASCII_TAB => INTERN_TAB,
    //         ASCII_SPACE => INTERN_SPACE,
    //         ASCII_SPECIAL_DEL => INTERN_SPECIAL_DEL,
    //         _ => if ASCII_LOWER_A <= ascii && ascii <= ASCII_LOWER_Z { INTERN_LOWER_A + ascii - ASCII_LOWER_A }
    //         else if ASCII_UPPER_A <= ascii && ascii <= ASCII_UPPER_Z { INTERN_UPPER_A + ascii - ASCII_UPPER_A }
    //         else if ASCII_0 <= ascii && ascii <= ASCII_9 { INTERN_0 + ascii - ASCII_0 }
    //         else if ASCII_SPECIAL_1 <= ascii && ascii <= ASCII_SPECIAL_2 { INTERN_SPECIAL_1 + ascii - ASCII_SPECIAL_1}
    //         else if ASCII_SPECIAL_3 <= ascii && ascii <= ASCII_SPECIAL_4 { INTERN_SPECIAL_3 + ascii - ASCII_SPECIAL_3}
    //         else if ASCII_SPECIAL_5 <= ascii && ascii <= ASCII_SPECIAL_6 { INTERN_SPECIAL_5 + ascii - ASCII_SPECIAL_5}
    //         else if ASCII_SPECIAL_7 <= ascii && ascii <= ASCII_SPECIAL_8 { INTERN_SPECIAL_7 + ascii - ASCII_SPECIAL_7}
    //         else if ASCII_SPECIAL_9 <= ascii && ascii <= ASCII_SPECIAL_10 { INTERN_SPECIAL_9 + ascii - ASCII_SPECIAL_9}
    //         else {
    //             assert!(ascii>ASCII_SPECIAL_DEL);
    //             assert_eq!(INTERN_SPECIAL_DEL, ASCII_SPECIAL_DEL);
    //             ascii
    //         }
    //     }
    // }
    //
    // fn intern_to_ascii(intern: u32) -> u32 {
    //     match intern {
    //         INTERN_ZERO => ASCII_ZERO,
    //         INTERN_NL => ASCII_NL,
    //         INTERN_TAB => ASCII_TAB,
    //         INTERN_SPACE => ASCII_SPACE,
    //         INTERN_SPECIAL_DEL => ASCII_SPECIAL_DEL,
    //         _ => if INTERN_LOWER_A <= intern && intern <= INTERN_LOWER_Z { ASCII_LOWER_A + intern - INTERN_LOWER_A }
    //         else if INTERN_UPPER_A <= intern && intern <= INTERN_UPPER_Z { ASCII_UPPER_A + ascii - INTERN_UPPER_A }
    //         else if INTERN_0 <= ascii && ascii <= INTERN_9 { ASCII_0 + ascii - INTERN_0 }
    //         else if INTERN_SPECIAL_1 <= intern && intern <= INTERN_SPECIAL_2 { ASCII_SPECIAL_1 + intern - INTERN_SPECIAL_1}
    //         else if INTERN_SPECIAL_3 <= intern && intern <= INTERN_SPECIAL_4 { ASCII_SPECIAL_3 + intern - INTERN_SPECIAL_3}
    //         else if INTERN_SPECIAL_5 <= intern && intern <= INTERN_SPECIAL_6 { ASCII_SPECIAL_5 + intern - INTERN_SPECIAL_5}
    //         else if INTERN_SPECIAL_7 <= intern && intern <= INTERN_SPECIAL_8 { ASCII_SPECIAL_7 + intern - INTERN_SPECIAL_7}
    //         else if INTERN_SPECIAL_9 <= intern && intern <= INTERN_SPECIAL_10 { ASCII_SPECIAL_9 + intern - INTERN_SPECIAL_9}
    //         else {
    //             assert!(intern>INTERN_SPECIAL_DEL);
    //             intern
    //         }
    //     }
    // }

    fn is_alphanumeric(x: u32) -> bool {
        (ASCII_LOWER_A <= x && x <= ASCII_LOWER_Z)
            || (ASCII_UPPER_A <= x && x <= ASCII_UPPER_Z)
            || (ASCII_0 <= x && x <= ASCII_9)
            || x == ASCII_SPACE
    }
    // 26*2+1 = 53
    /**The ASCII symbols are the most common. Hence they will be compressed
    (it minimises information entropy)*/
    pub fn intern(&mut self, seq: &IntSeq) -> Str {
        // u128::MAX == 340282366920938463463374607431768211455
        // a-zA-Z0-9[SPACE] = 26+26+10+1 = 63
        let alphanumeric_symbols = (ASCII_LOWER_Z - ASCII_LOWER_A + 1 +
            ASCII_UPPER_Z - ASCII_UPPER_A + 1 + ASCII_9 - ASCII_0 + 1 + 1) as u128;
        assert_eq!(63, alphanumeric_symbols);
        // log_63(u128::MAX) = 21.414422888
        // if we intern all alphanumeric strings up to length 20
        // we will have to reserve so many integer values:
        // 63^20 = 970087679866349716790969219380140801
        let alphanumeric_encodings = 970087679866349716790969219380140801u128;
        // that will leave us with spare
        // u128::MAX - 63^20 = 339312279241072113746583638212388070654
        // then we can encode all strings over (almost) entire ASCII
        let almost_entire_ascii = (alphanumeric_symbols + ASCII_SPECIAL_2 - ASCII_SPECIAL_1 + 1 +
            ASCII_SPECIAL_4 - ASCII_SPECIAL_3 + 1 +
            ASCII_SPECIAL_6 - ASCII_SPECIAL_5 + 1 +
            ASCII_SPECIAL_7 - ASCII_SPECIAL_6) as u128;
        assert_eq!(63, alphanumeric_symbols);

        let alphanumeric = 1;
        let almost_entire_ascii = 2;
        let entire_ascii = 2;
        let non_ascii = 2;
        let mut required_symbols = 0;
        if seq.len() == 0 {
            return Code(NonZeroU128::new(1).unwrap());
        }
        if seq.len() < 20 && seq.iter().all(Self::is_alphanumeric) {
            let mut encoding = 0u128;
            for &symbol in seq {}
            return Code(NonZeroU128::new(1 +).unwrap());
        }
        let offset = self.pool.len();
        self.pool.extend(seq);
        let end = self.pool.len();
        Str(&self.pool[offset..end])
    }
}