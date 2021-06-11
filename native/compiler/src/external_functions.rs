use v::V;
use func_arg::{FuncArgs, FuncArg};
use compilation_error::CompErr;
use g::G;
use int_embedding::IntEmbedding;
use learn::ostia_compress::{PTT, insert_informant, insert, insert_sample};
use ghost::Ghost;
use learn::lazy_dataset::*;
use parser_state::ParserState;
use std::io::Error;
use logger::Logger;
use int_seq::IntSeq;
use std::alloc::Global;
#[cfg(target_arch = "wasm32")]
use wasm_timer::SystemTime;

#[cfg(not(target_arch = "wasm32"))]
use std::time::SystemTime;

pub fn ostia_compress<L:Logger>(ghost:&Ghost, pos:V,logger:&mut L,args:FuncArgs)->Result<G,CompErr>{
    if args.len() == 1{
        if let FuncArg::Informant(i) = &args[0]{
            let alph = IntEmbedding::for_informant(&mut i.iter()).0;
            let mut ptt = PTT::new(alph);
            insert_informant(&mut ptt,i);
            return Ok(ptt.ostia_compress().compile(pos, ghost));
        }
    }
    Err(CompErr::IncorrectFunctionArguments(pos, String::from("Expected a single informant argument")))
}

pub fn ostia_compress_file<L:Logger>(ghost:&Ghost, pos:V,logger:&mut L, args:FuncArgs)->Result<G,CompErr>{
    if args.len() == 1{
        if let FuncArg::Informant(i) = &args[0]{
            if i.len() == 1 {
                let (path,_) = &i[0];
                let path = String::from(path.as_str());
                logger.println(String::from("Inferring alphabet"));
                return match infer_alph(&path){
                    Ok((alph,count)) => {
                        let mut ptt = PTT::new(alph);
                        logger.println(format!("Compressing {} samples",count));
                        match iter_dataset(&path){
                            Ok(i) => {
                                let mut now = SystemTime::now();
                                for (idx,sample) in i.enumerate(){
                                    if now.elapsed().map(|d|d.as_secs()>8).unwrap_or(false){
                                        now = SystemTime::now();
                                        logger.println(format!("Compressed {}/{} samples",idx,count));
                                    }
                                    insert_sample(&mut ptt, &sample);
                                }
                                Ok(ptt.ostia_compress().compile(pos, ghost))
                            }
                            Err(e) => Err(CompErr::Parse(pos,format!("{}:{}",e,path)))
                        }
                    }
                    Err(e) => Err(CompErr::Parse(pos,format!("{}:{}",e,path)))
                }

            }
        }
    }
    Err(CompErr::IncorrectFunctionArguments(pos, String::from("Expected a single file path argument")))
}

