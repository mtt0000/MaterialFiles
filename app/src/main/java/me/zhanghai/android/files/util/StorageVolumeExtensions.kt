package me.zhanghai.android.files.util

import android.os.storage.StorageVolume
import me.zhanghai.android.files.compat.directoryCompat
import me.zhanghai.android.files.compat.idCompat
import me.zhanghai.android.files.compat.isEmulatedCompat
import me.zhanghai.android.files.compat.isRemovableCompat

val StorageVolume.isMounted: Boolean
    get() = directoryCompat != null

// Public volume IDs look like "public:<major>,<minor>", and SD cards use the MMC block device major
// number 179, while USB mass storage devices use SCSI disk major numbers instead.
// @see android.os.storage.DiskInfo
val StorageVolume.isUsb: Boolean
    get() {
        if (!isRemovableCompat || isEmulatedCompat) {
            return false
        }
        val id = idCompat ?: return false
        if (!id.startsWith("public:")) {
            return false
        }
        val major = id.removePrefix("public:").substringBefore(',').toIntOrNull() ?: return false
        return major != 179
    }
