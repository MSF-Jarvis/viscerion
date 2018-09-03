#!/bin/bash
source "${TRAVIS_BUILD_DIR}"/ci/transfer.sh

if [[ "${TRAVIS_PULL_REQUEST}" != "false" ]]; then
    BUILT_APK="WireGuard-KT-${TRAVIS_PULL_REQUEST}-${TRAVIS_COMMIT}.apk"
    mv "${TRAVIS_BUILD_DIR}"/app/build/outputs/apk/debug/wg_android-TEST.apk "${BUILT_APK}"
    COMMENT="Test it at $(transfer "${BUILT_APK}")

__This is an automatically posted message from Travis-CI__"
    curl -H "Authorization: token ${GITHUB_TOKEN}" -X POST -d "\{\"body\": \"${COMMENT}\"\}" "https://api.github.com/repos/${TRAVIS_REPO_SLUG}/issues/${TRAVIS_PULL_REQUEST}/comments"
fi
