package dev.bmcreations.scrcast

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


fun Context?.hasPermissions(vararg permissions: String): Boolean {
    return this?.let { ctx ->
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    ctx,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    } ?: false
}
