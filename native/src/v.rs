
#[derive(Debug,Clone,Eq, PartialEq)]
pub enum V {
    UNKNOWN,
    POS(usize,usize),
    FILE(String,usize,usize)
}