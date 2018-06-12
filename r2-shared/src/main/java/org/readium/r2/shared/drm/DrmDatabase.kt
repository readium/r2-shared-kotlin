package org.readium.r2.shared.drm

import android.content.Context
import java.io.Serializable

interface DrmDatabase:Serializable {
}

interface DrmDatabaseOpenHelper {
    companion object {
        private var instance: DrmDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): DrmDatabaseOpenHelper {
            return instance!!
        }
    }

}