/*
 * Copyright (c) 2021 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.app.mainExecutor
import me.zhanghai.android.files.app.storageManager
import me.zhanghai.android.files.compat.registerReceiverCompat
import me.zhanghai.android.files.compat.storageVolumesCompat

object StorageVolumeListLiveData : LiveData<List<StorageVolume>>() {
    // A USB mass storage device is only mounted as a storage volume some time after it is
    // attached, and some devices don't reliably send the media broadcasts for it, so we also poll
    // a few times after a USB device is attached or detached.
    private val USB_RELOAD_DELAYS_MILLIS = longArrayOf(500, 1500, 3000, 6000, 10000)

    private val handler = Handler(Looper.getMainLooper())

    private val reloadRunnable = Runnable { loadValue() }

    init {
        loadValue()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED, UsbManager.ACTION_USB_DEVICE_DETACHED ->
                        scheduleUsbReloads()
                    else -> loadValue()
                }
            }
        }
        application.registerReceiverCompat(
            receiver, IntentFilter().apply {
                // @see android.os.storage.VolumeInfo#sEnvironmentToBroadcast
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_CHECKING)
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_UNMOUNTABLE)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                // The "file" data scheme is required to receive these broadcasts.
                // @see https://stackoverflow.com/a/7143298
                addDataScheme(ContentResolver.SCHEME_FILE)
            }, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        application.registerReceiverCompat(
            receiver, IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            registerStorageVolumeCallback()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun registerStorageVolumeCallback() {
        storageManager.registerStorageVolumeCallback(
            mainExecutor, object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(volume: StorageVolume) {
                    loadValue()
                }
            }
        )
    }

    private fun scheduleUsbReloads() {
        handler.removeCallbacks(reloadRunnable)
        for (delayMillis in USB_RELOAD_DELAYS_MILLIS) {
            handler.postDelayed(reloadRunnable, delayMillis)
        }
    }

    // Storage volumes may have changed while we weren't able to receive broadcasts.
    fun reload() {
        loadValue()
    }

    private fun loadValue() {
        value = try {
            storageManager.storageVolumesCompat
        } catch (e: Exception) {
            e.printStackTrace()
            value ?: emptyList()
        }
    }
}
