#!/bin/bash

function build {
    local BUILT_APK FINAL_APK GIT_DESCRIBE CHANGELOG MSF_TG_ID
    BUILT_APK="app/build/outputs/apk/debug/wg-android_TEST.apk"
    FINAL_APK="app/build/outputs/apk/debug/wg-android_${GIT_DESCRIBE}.apk"
    GIT_DESCRIBE="$(git describe --tags)"
    CHANGELOG="${1:-"No changelog found"}"
    MSF_TG_ID="211931420"
    ./gradlew :app:assemble :app:check
    if [[ ! -f "${BUILT_APK}" ]]; then
        reportError "APK Compilation failed"
    else
        mv "${BUILT_APK}" "${FINAL_APK}"
    fi
    curl -F chat_id="${MSF_TG_ID}" -F document="@${FINAL_APK}" -F caption="#${TRAVIS_BRANCH}" -F parse_mode="Markdown" "https://api.telegram.org/bot${TG_BOT_ID}/sendDocument" >/dev/null 2>&1
    curl -F chat_id="${MSF_TG_ID}" -F parse_mode="markdown" -F text="${CHANGELOG}" "https://api.telegram.org/bot${TG_BOT_ID}/sendMessage" >/dev/null 2>&1
}

function generate_changelog {
    touch changelog.txt
    GIT_COMMIT_LOG="$(git log --format='%s (by %an)' "${TRAVIS_COMMIT_RANGE}")"
    if [ -z "${GIT_COMMIT_LOG}" ]; then
        echo "Skipping empty changelog"
    else
        echo "${GIT_COMMIT_LOG}" > changelog.txt
    fi
}

git fetch --tags
generate_changelog
build "$(cat changelog.txt)"