#!/bin/bash
for test in batch_tests/test*.mealy ; do
  ./solomonoff --file "$test" > "$test.output"
  if ! cmp --silent "$test.expected" "$test.output" ; then
    echo "Fail: $test"
  fi
done

for test in batch_tests/test*.line ; do
  ./solomonoff --exec "$test" > "$test.output"
  if ! cmp --silent "$test.expected" "$test.output" ; then
    echo "Fail: $test"
  fi
done

for test in batch_tests/test*.sh ; do
  $test > "$test.output"
  if ! cmp --silent "$test.expected" "$test.output" ; then
    echo "Fail: $test"
  fi
done