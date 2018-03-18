#!/usr/bin/env sh

APK_NAME=AndroidPify-${MAJOR_MINOR}_${TRAVIS_BUILD_NUMBER}.apk

cp app/build/outputs/apk/release/app-release.apk ${APK_NAME}

CHANGELOG="$(./scripts/changelog.sh)
<a href=\"https://github.com/${TRAVIS_REPO_SLUG}/compare/${TRAVIS_COMMIT_RANGE}\">View on GitHub</a>"

curl -F chat_id="-1001259165513" -F sticker="CAADBQADKAADTBCSGmapM3AUlzaHAg" https://api.telegram.org/bot${BOT_TOKEN}/sendSticker
curl -F chat_id="-1001259165513" -F document=@"${APK_NAME}" https://api.telegram.org/bot${BOT_TOKEN}/sendDocument
curl -F chat_id="-1001259165513" -F text="${CHANGELOG}" -F parse_mode="HTML" https://api.telegram.org/bot${BOT_TOKEN}/sendMessage

echo $(./scripts/changelog.sh)