pub trait Logger {
    fn print(&self, msg: String);
    fn println(&self, msg: String);
}


pub struct ToggleableLogger {
    silent:bool
}

impl ToggleableLogger {
    pub fn new() -> Self { Self {silent:false} }
    pub fn toggle(&mut self, silent:bool){
        self.silent = silent;
    }
    pub fn is_silent(& self)->bool{
        self.silent
    }
}

impl Logger for ToggleableLogger {

    fn print(&self, msg: String) {
        if !self.silent{
            print!("{}", msg)
        }
    }

    fn println(&self, msg: String) {
        if !self.silent {
            println!("{}", msg)
        }
    }
}

pub struct StdoutLogger {}

impl StdoutLogger {
    pub fn new() -> Self { Self {} }
}

impl Logger for StdoutLogger {
    fn print(&self, msg: String) {
        print!("{}", msg)
    }

    fn println(&self, msg: String) {
        println!("{}", msg)
    }
}

pub struct SilentLogger {}

impl SilentLogger {
    pub fn new() -> Self { Self {} }
}

impl Logger for SilentLogger {
    fn print(&self, msg: String) {}

    fn println(&self, msg: String) {}
}
