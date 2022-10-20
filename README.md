# ADB FileTransfer Wrapper

Wrapper Application that handles External Broadcasts from ADB or any other Application with the intent of moving files while preserving the permissions from the External Storage of the device to a different location such as the Enterprise partition by using MX FileMgr APIs.
The application doesn't have any UI so it's entirely considered as a "Bridge" application made for this specific purpouse.
It will not run in the background unless it's going to be invoked externally by an application or by ADB.

## How it Works & Manual Usage

- Install the wrapper on any device running an Android Version with API Level >= 26
- Use one of the available actions to send a broadcast towards the application to perform the required operation:

```xml
<action android:name="com.zebra.mxadbfiletransfer.FILE_MOVE_ACTION" />
<action android:name="com.zebra.mxadbfiletransfer.FILE_DELETE_ACTION" />
```

- The application will also expect 2 parameters when moving a file:

```kotlin
const val SOURCE_FILE_PATH = "source_file_path"
const val TARGET_FILE_PATH = "target_file_path"
```

### Transfer a file from a Terminal Window with ADB

```bash
adb push $FullSourcePathWithFileName /sdcard/

adb shell am broadcast -a com.zebra.mxadbfiletransfer.FILE_MOVE_ACTION\
 --es source_file_path "$FullSourcePathWithFileName"\
 --es target_file_path "$FullTargetPathWithFileName"\
 -n com.zebra.mxadbfiletransfer/.FileTransferReceiver
```

### Delete a file from a Terminal Window with ADB

```bash
adb shell am broadcast -a com.zebra.mxadbfiletransfer.FILE_DELETE_ACTION\
 --es source_file_path "$FullSourcePathWithFileName"\
 -n com.zebra.mxadbfiletransfer/.FileTransferReceiver
```

### Transfer a file from an Application (Assuming the file is already in the specified location)

```kotlin
val intent = Intent().apply {
    action = "com.zebra.mxadbfiletransfer.FILE_MOVE_ACTION"
    component = ComponentName("com.zebra.mxadbfiletransfer", "com.zebra.mxadbfiletransfer.FileTransferReceiver")

    putExtra("source_file_path","/sdcard/test-configuration.xml")
    putExtra("target_file_path", "/enterprise/usr/test-configuration.xml")
}
sendBroadcast(intent)
```


### Delete a file from an Application (Assuming the file is in the specified location)

```kotlin
val intent = Intent().apply {
    action = "com.zebra.mxadbfiletransfer.FILE_DELETE_ACTION"
    component = ComponentName("com.zebra.mxadbfiletransfer", "com.zebra.mxadbfiletransfer.FileTransferReceiver")

    putExtra("source_file_path","/enterprise/usr/test-configuration.xml")
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

#### From an Application

```kotlin
val intent = Intent().apply {
    action = "com.zebra.mxadbfiletransfer.TERMINATE_ACTION"
    component = ComponentName("com.zebra.mxadbfiletransfer", "com.zebra.mxadbfiletransfer.FileTransferReceiver")
}
sendBroadcast(intent)
```

## Automatic Usage

You can also speed up the process by using one of the automated scripts included in the repository.
The Shell script will give you a wizzard-type of experience where you'll just need to input the fields with the required data and the script will do the rest for you.

Just run it like any other Shell Script:

```bash
./adb-file-transfer-wizzard.sh
```

If you want to use the Batch script, that will give you the convenience on Windows to just right click to transfer one or more files and with that, you will just have to send them to the script and everything else will be done in the background.
You can checkout more about how the Batch Script works by following this guide: [Batch-Script-README](https://github.com/nilac8991/mx-adb-filetransfer/files/9828754/Batch-Script-README.pdf)