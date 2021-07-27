extern crate solomonoff_lib;

use solomonoff_lib::g::G;
use std::process::{Command, Stdio, Child};
use rustyline::error::ReadlineError;
use rustyline::Editor;
use clap::{AppSettings, Clap};
use std::io::{BufRead, BufReader, Lines, Error};
use solomonoff_lib::learn::ostia_compress::PTT;
use solomonoff_lib::int_embedding::IntEmbedding;
use std::iter::Enumerate;
use solomonoff_lib::int_seq::IntSeq;
use solomonoff_lib::v::V;
use solomonoff_lib::ranged_evaluation::new_output_buffer;
use solomonoff_lib::ghost::Ghost;
use std::time::{Duration, SystemTime};
use solomonoff_lib::parser_state::ParserState;
use solomonoff_lib::solomonoff::Solomonoff;
use solomonoff_lib::repl::Repl;
use solomonoff_lib::logger::{StdoutLogger, ToggleableLogger, Logger};
use solomonoff_lib::repl_command::{ReplCommand, ReplArg, args_to_optional_value};
use solomonoff_lib::compilation_error::CompErr;
use std::fs::File;

///Solomonoff - a finite state transducer compiler with inductive inference utilities
#[derive(Clap)]
#[clap(version = "1.0", author = "Aleksander Mendoza <aleksander.mendoza.drosik@gmail.com>")]
#[clap(setting = AppSettings::ColoredHelp)]
struct Opts {
    ///Path to script file containing regexes
    #[clap(short='f', long)]
    file: Option<String>,
}

fn main() {
    Ghost::with_mock(|ghost|{
        let opts: Opts = Opts::parse();
        let mut solomonoff = Repl::new_with_standard_commands();
        type L = ToggleableLogger;
        let mut log = L::new();
        let mut debug = L::new();

        if let Some(path) = opts.file{
            match std::fs::read_to_string(&path){
                Ok(s) => solomonoff.parse(&mut log,&s,ghost).expect("Failed loading script"),
                Err(err) => {println!("Failed reading file {}: {:?}",path,err)}
            }

        }

        // `()` can be used when no completer is required
        let mut rl = Editor::<()>::new();
        if rl.load_history("history.txt").is_err() {
            debug.println(String::from("No previous history."));
        }

        solomonoff.attach_cmd(String::from("exit"), ReplCommand{
            description: "Exits REPL",
            f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost|{
                //the implementation is not here
                Ok(Some(String::from("Unexpected argument")))
            }
        });
        solomonoff.attach_cmd(String::from("verbose"), ReplCommand{
            description: "Turn verbosity on/off",
            f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
                if let Some(arg) = args_to_optional_value(args, "BOOL")?{
                    if arg=="true"{
                        debug.toggle(false);
                        Ok(None)
                    }else if arg=="false"{
                        debug.toggle(true);
                        Ok(None)
                    }else{
                        Err(CompErr::IncorrectCommandArguments(String::from("Specify true or false!")))
                    }
                } else{
                    Ok(Some((!debug.is_silent()).to_string()))
                }

            }
        });
        loop {
            let readline = rl.readline("> ");
            match readline {
                Ok(line) => {
                    rl.add_history_entry(line.as_str());
                    if line.trim() == "/exit"{
                        break;
                    }
                    match solomonoff.repl(&mut log,&mut debug,line.as_str(), &ghost){
                        Err(err) => eprintln!("{:?}", err),
                        Ok(Some(out)) => println!("{}", out),
                        Ok(None) => {}
                    }
                }
                Err(ReadlineError::Interrupted) => {
                }
                Err(ReadlineError::Eof) => {
                    break;
                }
                Err(err) => {
                    println!("Error: {:?}", err);
                    break;
                }
            }
        }
        solomonoff.state_mut().delete_all(ghost);
        rl.save_history("history.txt").unwrap();
    });
}