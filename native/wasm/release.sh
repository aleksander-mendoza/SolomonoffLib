#!/bin/bash
set -e
rm -rf pkg
wasm-pack build --release --target web
python3 -m http.server


