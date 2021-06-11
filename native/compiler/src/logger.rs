pub trait Logger {
    fn print(&mut self, msg: String);
    fn println(&mut self, msg: String);

}


pub struct AccumulatingLogger {
    data:String
}

impl AccumulatingLogger {
    pub fn new() -> Self { Self {data:String::new()} }
    pub fn get(&self)->&String{
        &self.data
    }
    pub fn reset(&mut self){
        self.data.clear()
    }
}

impl Logger for AccumulatingLogger {

    fn print(&mut self, msg: String) {
        self.data += &msg
    }

    fn println(&mut self, msg: String) {
        self.data += &msg;
        self.data.push('\n');
    }
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

    fn print(&mut self, msg: String) {
        if !self.silent{
            print!("{}", msg)
        }
    }

    fn println(&mut self, msg: String) {
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
    fn print(&mut self, msg: String) {
        print!("{}", msg)
    }

    fn println(&mut self, msg: String) {
        println!("{}", msg)
    }
}

pub struct SilentLogger {}

impl SilentLogger {
    pub fn new() -> Self { Self {} }
}

impl Logger for SilentLogger {
    fn print(&mut self, msg: String) {}

    fn println(&mut self, msg: String) {}
}
