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

#[derive(Clap)]
#[clap(version = "1.0", author = "Aleksander Mendoza <aleksander.mendoza.drosik@gmail.com>")]
#[clap(setting = AppSettings::ColoredHelp)]
struct Opts {
    dataset: String,
}

fn main() {
    let opts: Opts = Opts::parse();
    println!("Using dataset: {}", opts.dataset);
    let cmd = Command::new("python3")
        .arg(opts.dataset.clone())
        .stdout(Stdio::piped())
        .spawn()
        .expect("failed to execute process");
    let mut lines = BufReader::new(cmd.stdout.unwrap()).lines().filter_map(|e| e.ok()).filter_map(|line| line.split_once('\t').map(|(l, r)| IntSeq::from(l)));
    let alph = IntEmbedding::for_strings(&mut lines);

    let cmd = Command::new("python3")
        .arg(opts.dataset)
        .stdout(Stdio::piped())
        .spawn()
        .expect("failed to execute process");
    let lines = BufReader::new(cmd.stdout.unwrap()).lines();

    let mut ptt = PTT::new(alph);
    for line in lines {
        if let Ok(line) = line {
            if let Some((l,r)) = line.split_once('\t') {
                println!("{:?} -> {:?}", l, r);
                ptt.insert_positive(&IntSeq::from(l),&IntSeq::from(r));
            }
        }
    }
    let ghost = Ghost::new();
    let mut ptt = ptt.ostia_compress().compile(V::UNKNOWN, &ghost);
    let r= ptt.optimise_graph(&ghost);
    let mut state_to_index = r.make_state_to_index_table();
    let mut out_buff = new_output_buffer(256);
    ptt.delete(&ghost);
    assert!(ghost.is_empty(),"Ghost not empty!");
    // `()` can be used when no completer is required
    let mut rl = Editor::<()>::new();
    if rl.load_history("history.txt").is_err() {
        println!("No previous history.");
    }
    loop {
        let readline = rl.readline("> ");
        match readline {
            Ok(line) => {
                rl.add_history_entry(line.as_str());
                println!("Line: {}", line);
                let y = r.evaluate_tabular(&mut state_to_index,&mut out_buff,&IntSeq::from(line.as_str()));
                let y:String = y.unwrap().iter().map(|&x| unsafe{char::from_u32_unchecked(x)}).collect();
                println!("Output: {}", y);
            }
            Err(ReadlineError::Interrupted) => {
                println!("CTRL-C");
                break;
            }
            Err(ReadlineError::Eof) => {
                println!("CTRL-D");
                break;
            }
            Err(err) => {
                println!("Error: {:?}", err);
                break;
            }
        }
    }
    rl.save_history("history.txt").unwrap();
}