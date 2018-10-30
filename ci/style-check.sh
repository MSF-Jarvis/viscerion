#!/bin/bash

aria2c https://github.com/shyiko/ktlint/releases/download/0.29.0/ktlint
chmod +x ktlint
./ktlint app/src/main/**/*.kt
