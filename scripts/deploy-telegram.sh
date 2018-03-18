#!/usr/bin/env sh

APK_NAME=AndroidPify-${MAJOR_MINOR}_${TRAVIS_BUILD_NUMBER}.apk

cp app/build/outputs/apk/debug/app-debug.apk ${APK_NAME}

curl -F chat_id="-1001259165513" -F document=@"${APK_NAME}" https://api.telegram.org/bot${BOT_TOKEN}/sendDocument
curl -F chat_id="-1001259165513" -F text="$(./scripts/changelog.sh)" -F parse_mode="HTML" https://api.telegram.org/bot${BOT_TOKEN}/sendMessage

echo $(./scripts/changelog.sh)