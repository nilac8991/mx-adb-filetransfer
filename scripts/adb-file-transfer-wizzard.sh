#!/bin/bash

# Copyright (C) 2022 Daniel Neamţu for Zebra Technologies

	echo -e '\0033\0143'
	clear

function TRANSFERFILE() {
	echo -e "Insert source path of the file to transfer"
	read sourceFilePath

	if [[ $sourceFilePath = "" ]]; then
		TRANSFERFILE
	fi

	echo -e "Insert target path for the file to transfer (Ex: /enterprise/usr)"
	read targetFilePath

	if [[ $targetFilePath = "" ]]; then
		TRANSFERFILE
	fi

	echo -e "Insert file name to be used for transfer (Ex: test-configuration.xml)"
	read transferFileName

	if [[ $transferFileName = "" ]]; then
		TRANSFERFILE
	fi

	adb push "$sourceFilePath" /sdcard/"$transferFileName"

	adb shell am broadcast -a com.zebra.mxadbfiletransfer.FILE_MOVE_ACTION\
 	--es source_file_path /sdcard/"$transferFileName"\
 	--es target_file_path "$targetFilePath"/"$transferFileName"\
 	-n com.zebra.mxadbfiletransfer/.FileTransferReceiver

	echo -e "Transfer another file? (Write Yes or No)"
	read transferAnswer

	if [[ $transferAnswer = "Yes" ]]; then
		TRANSFERFILE
	else 
		TERMINATE
	fi
}

function TERMINATE() {
	echo -e "Terminating connection.."

	adb shell am broadcast -a com.zebra.mxadbfiletransfer.TERMINATE_ACTION\
 	-n com.zebra.mxadbfiletransfer/.FileTransferReceiver
 	exit 0
}


while [[ true ]]; do
	TRANSFERFILE
	TERMINATE
done