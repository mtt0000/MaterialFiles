package me.zhanghai.android.files.util

import android.os.storage.StorageVolume
import me.zhanghai.android.files.compat.directoryCompat
import me.zhanghai.android.files.compat.idCompat
import me.zhanghai.android.files.compat.isEmulatedCompat
import me.zhanghai.android.files.compat.isRemovableCompat
import me.zhanghai.android.files.compat.pathCompat

val StorageVolume.isMounted: Boolean
    get() = directoryCompat != null

// Public volumes that aren't visible to apps (e.g. USB drives on many devices) only have their raw
// mount point under /mnt/media_rw as their path, which apps can't access, so they can only be
// accessed via the Storage Access Framework.
// @see https://cs.android.com/android/platform/superproject/+/main:system/vold/model/PublicVolume.cpp
val StorageVolume.isAccessibleByPath: Boolean
    get() = !pathCompat.startsWith("/mnt/media_rw/")

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
