use v::V;
use func_arg::{FuncArgs, FuncArg};
use compilation_error::CompErr;
use g::G;
use int_embedding::IntEmbedding;
use learn::ostia_compress::{PTT, insert_informant};
use ghost::Ghost;
use learn::lazy_dataset::*;
use parser_state::ParserState;
use std::io::Error;

pub fn ostia_compress(ghost:&Ghost, pos:V,args:FuncArgs)->Result<G,CompErr>{
    if args.len() == 1{
        if let FuncArg::Informant(i) = &args[0]{
            let alph = IntEmbedding::for_informant(&mut i.iter());
            let mut ptt = PTT::new(alph);
            insert_informant(&mut ptt,i);
            return Ok(ptt.ostia_compress().compile(pos, ghost));
        }
    }
    Err(CompErr::IncorrectFunctionArguments(pos, String::from("Expected a single informant argument")))
}

pub fn ostia_compress_file(ghost:&Ghost, pos:V,args:FuncArgs)->Result<G,CompErr>{
    if args.len() == 1{
        if let FuncArg::Informant(i) = &args[0]{
            if i.len() == 1 {
                let (path,_) = &i[0];
                let path = path.to_string();
                return match infer_alph(&path){
                    Ok(alph) => {
                        let mut ptt = PTT::new(alph);
                        insert_from_dataset(&mut ptt,&path);
                        Ok(ptt.ostia_compress().compile(pos, ghost))
                    }
                    Err(e) => Err(CompErr::Parse(pos,e.to_string()))
                }

            }
        }
    }
    Err(CompErr::IncorrectFunctionArguments(pos, String::from("Expected a single file path argument")))
}

