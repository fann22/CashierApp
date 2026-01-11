build:
	clear && ./gradlew assembleDebug && echo "\nInstalling..."  && su -c "pm install --user 0 ./app/build/outputs/apk/debug/app-debug.apk"