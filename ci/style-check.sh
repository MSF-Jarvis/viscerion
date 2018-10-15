#!/bin/bash

aria2c https://github.com/shyiko/ktlint/releases/download/0.29.0/ktlint -o ~/bin/ktlint
chmod +x ~/bin/ktlint
export PATH="~/bin/:$PATH"
ktlint "${TRAVIS_BUILD_DIR}"/app/src/main/**/*.kt
