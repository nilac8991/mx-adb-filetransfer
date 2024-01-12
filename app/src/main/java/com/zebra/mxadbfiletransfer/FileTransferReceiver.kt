package com.zebra.mxadbfiletransfer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.zebra.nilac.emdkloader.EMDKLoader
import com.zebra.nilac.emdkloader.ProfileLoader
import com.zebra.nilac.emdkloader.interfaces.EMDKManagerInitCallBack
import com.zebra.nilac.emdkloader.interfaces.ProfileLoaderResultCallback
import java.io.File
import kotlin.system.exitProcess

open class FileTransferReceiver : BroadcastReceiver() {

    private var mSourceFilePath: Uri? = null
    private var mTargetFilePath: Uri? = null
    private var mFileName: String? = null

    private var mAction = ""

    private var mEmdkInstance: EMDKManager? = null

    override fun onReceive(context: Context, intent: Intent) {
        //Discard Intents with Actions which are not ours
        if (intent.action != FILE_MOVE_ACTION &&
            intent.action != FILE_COPY_ACTION &&
            intent.action != FILE_PULL_ACTION &&
            intent.action != FILE_DELETE_ACTION &&
            intent.action != TERMINATE_ACTION
        ) {
            return
        }

        mAction = intent.action!!

        if (mAction == FILE_PULL_ACTION) {
            mFileName = intent.getStringExtra(FILE_NAME)
        } else {
            mSourceFilePath = Uri.parse(intent.getStringExtra(SOURCE_FILE_PATH))
            mTargetFilePath = Uri.parse(intent.getStringExtra(TARGET_FILE_PATH))
        }

//        if (!File(mSourceFilePath).exists()) {
//            Log.e(
//                TAG,
//                "Unable to move file as it doesn't exist at the following location $mSourceFilePath"
//            )
//            return
//        }

        mEmdkInstance = EMDKLoader.getInstance().getManager()
        if (mEmdkInstance == null) {
            initEMDKManager()
            return
        }

        processAction()
    }

    private fun initEMDKManager() {
        //Initialising EMDK First...
        Log.i(TAG, "Initialising EMDK Manager")

        EMDKLoader.getInstance().initEMDKManager(
            DefaultApplication.getInstance().applicationContext,
            object : EMDKManagerInitCallBack {
                override fun onFailed(message: String) {
                    Log.e(TAG, "Failed to initialise EMDK Manager\n$message")
                }

                override fun onSuccess() {
                    Log.i(TAG, "EMDK Manager was successfully initialised")

                    processAction()
                }
            })
    }

    private fun processAction() {
        when (mAction) {
            FILE_MOVE_ACTION -> {
                Log.i(TAG, "Moving file from $mSourceFilePath to $mTargetFilePath")
                moveFileToEnterprisePartition(true)
            }
            FILE_COPY_ACTION -> {
                Log.i(TAG, "Copying file from $mSourceFilePath to $mTargetFilePath")
                moveFileToEnterprisePartition(false)
            }
            FILE_PULL_ACTION -> {
                Log.i(TAG, "Pulling file: $mFileName from the enterprise partition")
                pullFileFromEnterprisePartition()
            }
            FILE_DELETE_ACTION -> {
                Log.i(TAG, "Deleting file from location: $mSourceFilePath")
                removeFile()
            }
            TERMINATE_ACTION -> {
                Log.i(TAG, "Terminating Receiver..")
                EMDKLoader.getInstance().release()
                exitProcess(0)
            }
        }
    }

    private fun moveFileToEnterprisePartition(shouldDeleteSourceFile: Boolean) {
        val profile =
            """
            <wap-provisioningdoc>
                <characteristic type="Profile">
                    <parm name="ProfileName" value="FileTransfer" />
                    <parm name="ModifiedDate" value="2022-08-17 10:20:36" />
                    <parm name="TargetSystemVersion" value="10.1" />

                    <characteristic version="10.1" type="FileMgr">
                        <parm name="FileAction" value="1" />
                        <characteristic type="file-details">
                            <parm name="TargetAccessMethod" value="2" />
                            <parm name="TargetPathAndFileName" value="${mTargetFilePath?.path}" />
                            <parm name="SourceAccessMethod" value="2" />
                            <parm name="SourcePathAndFileName" value="${mSourceFilePath?.path}" />
                        </characteristic>
                    </characteristic>
                </characteristic>
            </wap-provisioningdoc>"""

        ProfileLoader().processProfile(
            "FileTransfer",
            profile,
            object : ProfileLoaderResultCallback {
                override fun onProfileLoadFailed(errorObject: EMDKResults) {
                    //Nothing to see here..
                }

                override fun onProfileLoadFailed(message: String) {
                    Log.e(TAG, "Failed to move file!\n$message")
                }

                override fun onProfileLoaded() {
                    if (shouldDeleteSourceFile) {
                        removeFile()
                    }
                }
            })
    }

    private fun pullFileFromEnterprisePartition() {
        val profile =
            """
            <wap-provisioningdoc>
                <characteristic type="Profile">
                    <parm name="ProfileName" value="FilePull" />
                    <parm name="ModifiedDate" value="2022-08-17 10:20:36" />
                    <parm name="TargetSystemVersion" value="10.1" />

                    <characteristic version="10.1" type="FileMgr">
                        <parm name="FileAction" value="1" />
                        <characteristic type="file-details">
                            <parm name="TargetAccessMethod" value="2" />
                            <parm name="TargetPathAndFileName" value="${Environment.getExternalStorageDirectory().path}/$mFileName" />
                            <parm name="SourceAccessMethod" value="2" />
                            <parm name="SourcePathAndFileName" value="/enterprise/usr/$mFileName" />
                        </characteristic>
                    </characteristic>
                </characteristic>
            </wap-provisioningdoc>"""

        ProfileLoader().processProfile(
            "FilePull",
            profile,
            object : ProfileLoaderResultCallback {
                override fun onProfileLoadFailed(errorObject: EMDKResults) {
                    //Nothing to see here..
                }

                override fun onProfileLoadFailed(message: String) {
                    Log.e(TAG, "Failed to move file!\n$message")
                }

                override fun onProfileLoaded() {
                    removeFile()
                }
            })
    }

    private fun removeFile() {
        val profile =
            """
            <wap-provisioningdoc>
                <characteristic type="Profile">
                    <parm name="ProfileName" value="FileRemoval" />
                    <parm name="ModifiedDate" value="2022-08-17 10:20:36" />
                    <parm name="TargetSystemVersion" value="10.1" />
                    
                    <characteristic version="10.1" type="FileMgr">
                        <parm name="FileAction" value="4" />
                        <characteristic type="file-details">
                            <parm name="TargetPathAndFolderName" value="${mSourceFilePath?.path}" />
                        </characteristic>
                    </characteristic>
                </characteristic>
            </wap-provisioningdoc>"""

        ProfileLoader().processProfile(
            "FileRemoval",
            profile,
            object : ProfileLoaderResultCallback {
                override fun onProfileLoadFailed(errorObject: EMDKResults) {
                    //Nothing to see here..
                }

                override fun onProfileLoadFailed(message: String) {
                    Log.e(TAG, "Failed to remove file!\n$message")
                }

                override fun onProfileLoaded() {
                    //Nothing to see here..
                }
            })
    }

    companion object {
        const val TAG = "FileTransferReceiver"

        const val FILE_MOVE_ACTION = "com.zebra.mxadbfiletransfer.FILE_MOVE_ACTION"
        const val FILE_COPY_ACTION = "com.zebra.mxadbfiletransfer.FILE_COPY_ACTION"
        const val FILE_DELETE_ACTION = "com.zebra.mxadbfiletransfer.FILE_DELETE_ACTION"
        const val FILE_PULL_ACTION = "com.zebra.mxadbfiletransfer.FILE_PULL_ACTION"

        const val TERMINATE_ACTION = "com.zebra.mxadbfiletransfer.TERMINATE_ACTION"

        const val FILE_NAME = "file_name"
        const val SOURCE_FILE_PATH = "source_file_path"
        const val TARGET_FILE_PATH = "target_file_path"
    }
}