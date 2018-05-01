#!/usr/bin/env sh

CHANGELOG="$(./scripts/changelog.sh)
<a href=\"https://github.com/${TRAVIS_REPO_SLUG}/compare/${TRAVIS_COMMIT_RANGE}\">View on GitHub</a>"

curl -F chat_id="-1001259165513" -F sticker="CAADBAAD8R8AAmSKPgABCOk3mg-Zvf0C" https://api.telegram.org/bot${BOT_TOKEN}/sendSticker
curl -F chat_id="-1001259165513" -F document=@"${APK_NAME}" https://api.telegram.org/bot${BOT_TOKEN}/sendDocument
curl -F chat_id="-1001259165513" -F text="${CHANGELOG}" -F parse_mode="HTML" -F disable_web_page_preview="true" https://api.telegram.org/bot${BOT_TOKEN}/sendMessage

echo $(./scripts/changelog.sh)
