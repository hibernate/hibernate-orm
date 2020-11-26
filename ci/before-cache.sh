#! /bin/bash

rm -f  $HOME/.gradle/caches/modules-2/modules-2.lock
rm -fr $HOME/.gradle/caches/*/plugin-resolution/
rm -f  $HOME/.gradle/caches/*/fileHashes/fileHashes.bin
rm -f  $HOME/.gradle/caches/*/fileHashes/fileHashes.lock
rm -fr $HOME/.m2/repository/org/hibernate