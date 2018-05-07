# iqchannels-android

main: build upload

build:
	./gradlew -p iqchannels-sdk build

upload:
	./gradlew -p iqchannels-sdk bintrayUpload

install:
	./gradlew install
