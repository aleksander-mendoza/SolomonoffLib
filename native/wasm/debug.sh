#!/bin/bash
set -e
rm -rf pkg
wasm-pack build --debug --target web
python3 -m http.server


