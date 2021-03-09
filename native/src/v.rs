
#[derive(Debug, Clone)]
pub enum V {
    UNKNOWN,
    POS(usize,usize),
    FILE(String,usize,usize)
}