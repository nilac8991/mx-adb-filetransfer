package com.zebra.mxadbfiletransfer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

open class FileTransferReceiver : BroadcastReceiver(), ProfileLoaderResultCallback {

    private var mSourceFilePath = ""
    private var mTargetFilePath = ""

    private var mEmdkInstance: EMDKManager? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION && intent.action != TERMINATE_ACTION) {
            return
        }

        if (intent.action == TERMINATE_ACTION) {
            EMDKLoader.getInstance().release()
            exitProcess(0)
        }

        mSourceFilePath = intent.getStringExtra(SOURCE_FILE_PATH)!!
        mTargetFilePath = intent.getStringExtra(TARGET_FILE_PATH)!!

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

        moveFileToEnterprisePartition()
    }

    override fun onProfileLoadFailed(errorObject: EMDKResults) {
        //Nothing to see here..
    }

    override fun onProfileLoadFailed(message: String) {
        Log.e(TAG, "Failed to grant Manage External Storage Access Permission!")
    }

    override fun onProfileLoaded() {
        moveFileToEnterprisePartition()
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

                    moveFileToEnterprisePartition()
                }
            })
    }

    private fun moveFileToEnterprisePartition() {
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
                            <parm name="TargetPathAndFileName" value="$mTargetFilePath" />
                            <parm name="IfDuplicate" value="1" />
                            <parm name="SourceAccessMethod" value="2" />
                            <parm name="SourcePathAndFileName" value="$mSourceFilePath" />
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
                    removeSourceFile()
                }
            })
    }

    private fun removeSourceFile() {
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
                            <parm name="TargetPathAndFolderName" value="$mSourceFilePath" />
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

        const val ACTION = "com.zebra.mxadbfiletransfer.FILE_MOVE_ACTION"
        const val TERMINATE_ACTION = "com.zebra.mxadbfiletransfer.TERMINATE_ACTION"

        const val SOURCE_FILE_PATH = "source_file_path"
        const val TARGET_FILE_PATH = "target_file_path"
    }
}