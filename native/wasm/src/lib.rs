mod utils;

use wasm_bindgen::prelude::*;

use solomonoff_lib::repl::Repl;
use solomonoff_lib::logger::{AccumulatingLogger, Logger};
use solomonoff_lib::ghost::Ghost;
// When the `wee_alloc` feature is enabled, use `wee_alloc` as the global
// allocator.
#[cfg(feature = "wee_alloc")]
#[global_allocator]
static ALLOC: wee_alloc::WeeAlloc = wee_alloc::WeeAlloc::INIT;

#[wasm_bindgen]
extern {
    fn alert(s: &str);
}


#[wasm_bindgen]
pub struct Solomonoff {
    repl: Repl<JsLogger>,
    log: JsLogger,
    debug: JsLogger,
    ghost: Ghost,
}

struct JsLogger {
    f: js_sys::Function
}

impl JsLogger {
    fn new(f: js_sys::Function) -> Self {
        Self { f }
    }
}

impl Logger for JsLogger {
    fn print(&mut self, msg: String) {
        let this = JsValue::null();
        let x = JsValue::from(msg);
        if let Err(e) = self.f.call1(&this,&x){
            panic!("Could not call JS closure: {:?}",e)
        }
    }

    fn println(&mut self, mut msg: String) {
        msg.push('\n');
        self.print(msg)
    }
}

#[wasm_bindgen]
impl Solomonoff {
    pub fn new(log: js_sys::Function, debug: js_sys::Function) -> Self {
        let ghost = Ghost::new();
        Self {
            repl: Repl::new_with_standard_commands(&ghost),
            log: JsLogger::new(log),
            debug: JsLogger::new(debug),
            ghost
        }
    }
    pub fn repl(&mut self, input: String) -> Result<Option<String>,JsValue> {
        let Solomonoff { repl, log, debug,ghost } = self;
        repl.repl(log, debug, &input,&ghost).map_err(|e|JsValue::from(format!("{:?}",e)))
    }
}


#[wasm_bindgen]
pub fn greet() {
    alert("Hello, native-wasm!");
}
