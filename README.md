# ADB FileTransfer Wrapper

Wrapper Application which handles Broadcasts coming externally from ADB or any other Application wanting to use the customized ACTION to move files while preserving the permissions from the External Storage of the device to a different location such as the Enterprise partition by using MX FileMgr APIs.
The application doesn't have any UI so it's considered entirely as a "Bridge" application made for this specific purpouse.
It will not run in background unless it's being invoked externally by an application or by ADB.

## How it Works & Manual Usage

- Install the wrapper on any device running an Android Version with API Level >= 26
- Use the available action to send a broadcast towards the app to transfer a file

```xml
<action android:name="com.zebra.mxadbfiletransfer.FILE_TRANSFER_ACTION" />
```

- The application will also expect 2 parameters:

```kotlin
const val SOURCE_FILE_PATH = "source_file_path"
const val TARGET_FILE_PATH = "target_file_path"
```

### Transfer the file from a Terminal Window with ADB

```bash
adb push $FullPathName /sdcard/

adb shell am broadcast -a com.zebra.mxadbfiletransfer.FILE_TRANSFER_ACTION\
 --es source_file_path "$FullSourcePathWithFileName"\
 --es target_file_path "$FullTargetPathWithFileName"\
 -n com.zebra.mxadbfiletransfer/.FileTransferReceiver
```

### Transfer the file from an External Application (Assuming the file is already in the specified location)

```kotlin
val intent = Intent().apply {
    action = "com.zebra.mxadbfiletransfer.FILE_TRANSFER_ACTION"
    component = ComponentName("com.zebra.mxadbfiletransfer", "com.zebra.mxadbfiletransfer.FileTransferReceiver")

    putExtra("source_file_path","/sdcard/test-configuration.xml")
    putExtra("target_file_path", "/enterprise/usr/test-configuration.xml")
}
sendBroadcast(intent)
```

### Terminate the connection with the Wrapper Application

Since the Wrapper is using an EMDK Manager instance to process the profiles for the transfer operations, it doesn't reinitialize every time the instance as it preserves it for further use in case there are multiple files to be transfered/moved.
When you finish with the operations it is recommended to terminate the Wrapper and at the same time recycle the available EMDK Manager instance.

#### With ADB:

```bash
adb shell am broadcast -a com.zebra.mxadbfiletransfer.TERMINATE_ACTION\
 -n com.zebra.mxadbfiletransfer/.FileTransferReceiver
```

#### Within an Application

```kotlin
val intent = Intent().apply {
    action = "com.zebra.mxadbfiletransfer.TERMINATE_ACTION"
    component = ComponentName("com.zebra.mxadbfiletransfer", "com.zebra.mxadbfiletransfer.FileTransferReceiver")
}
sendBroadcast(intent)
```

## Automatic Usage

You can also speed up the process by using the automated script included in the repository, where you only need to input the fields with the required data and the script will do the rest for you.

Just run it like any other Shell Script:

```bash
./adb-file-transfer-wrapper.sh
```