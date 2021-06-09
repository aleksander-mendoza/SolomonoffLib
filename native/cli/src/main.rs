extern crate compiler;

use compiler::g::G;
use std::process::{Command, Stdio, Child};
use rustyline::error::ReadlineError;
use rustyline::Editor;
use clap::{AppSettings, Clap};
use std::io::{BufRead, BufReader, Lines};
use compiler::learn::ostia_compress::PTT;
use compiler::int_embedding::IntEmbedding;
use std::iter::Enumerate;
use compiler::int_seq::IntSeq;
use compiler::v::V;
use compiler::ranged_evaluation::new_output_buffer;
use compiler::ghost::Ghost;
use std::time::{Duration, SystemTime};
use compiler::parser_state::ParserState;
use compiler::solomonoff::Solomonoff;
use compiler::repl::Repl;
use compiler::logger::{StdoutLogger, ToggleableLogger};
use compiler::repl_command::{ReplCommand, ReplArg, args_to_optional_value};
use compiler::compilation_error::CompErr;

#[derive(Clap)]
#[clap(version = "1.0", author = "Aleksander Mendoza <aleksander.mendoza.drosik@gmail.com>")]
#[clap(setting = AppSettings::ColoredHelp)]
struct Opts {
    dataset: Option<String>,
}

fn main() {
    Ghost::with_mock(|ghost|{
        let opts: Opts = Opts::parse();
        let mut solomonoff = Repl::new_with_standard_commands();
        // `()` can be used when no completer is required
        let mut rl = Editor::<()>::new();
        if rl.load_history("history.txt").is_err() {
            println!("No previous history.");
        }
        type L = ToggleableLogger;
        let mut log = L::new();
        let mut debug = L::new();
        solomonoff.attach_cmd(String::from("verbose"), ReplCommand{
            description: "Turn verbosity on/off",
            f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
                if let Some(arg) = args_to_optional_value(args, "BOOL")?{
                    if arg=="true"{
                        debug.toggle(true);
                        Ok(None)
                    }else if arg=="false"{
                        debug.toggle(false);
                        Ok(None)
                    }else{
                        Err(CompErr::IncorrectCommandArguments(String::from("Specify true or false!")))
                    }
                } else{
                    Ok(Some(debug.is_silent().to_string()))
                }

            }
        });
        loop {
            let readline = rl.readline("> ");
            match readline {
                Ok(line) => {
                    rl.add_history_entry(line.as_str());
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