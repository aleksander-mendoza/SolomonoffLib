use g::G;
use int_seq::IntSeq;

pub struct FuncArgs(Vec<FuncArg>);
pub type Informant = Vec<(IntSeq,Option<IntSeq>)>;
pub enum FuncArg{
    Informant(Informant),
    Expression(G),
}

impl FuncArg{
    pub fn new_informant(informant:Informant)->Self{
        Self::Informant(informant)
    }
    pub fn new_expression(g:G)->Self{
        Self::Expression(g)
    }
}

impl FuncArgs{

}