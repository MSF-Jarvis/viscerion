#!/usr/bin/env bash

mkdir -p setup
curl https://raw.githubusercontent.com/akhilnarang/scripts/master/setup/install_android_sdk.bash > setup/install_android_sdk.bash
curl https://raw.githubusercontent.com/akhilnarang/scripts/master/setup/setup_android_sdk_packages.bash > setup/setup_android_sdk_packages.sh
curl https://raw.githubusercontent.com/akhilnarang/scripts/master/setup/android-sdk-minimal.txt > setup/android-sdk-minimal.txt

chmod +x setup/install_android_sdk.bash setup/setup_android_sdk_packages.sh

./setup/install_android_sdk.bash