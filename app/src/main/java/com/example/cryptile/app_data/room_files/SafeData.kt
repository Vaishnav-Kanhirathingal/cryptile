package com.example.cryptile.app_data.room_files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safe_database")
data class SafeData(
    // TODO: randomize id to avoid safe conflicts
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "safe_name") var safeName: String,
    @ColumnInfo(name = "safe_owner") var safeOwner: String,
    @ColumnInfo(name = "safe_uses_multiple_password") var safeUsesMultiplePassword: Boolean,
    @ColumnInfo(name = "safe_partial_key") var safePartialKey: String,
    @ColumnInfo(name = "personal_access_only") var personalAccessOnly: Boolean,
    @ColumnInfo(name = "encryption_algorithm") var encryptionAlgorithm: String,
    @ColumnInfo(name = "safe_created") var safeCreated: Long,
    @ColumnInfo(name = "safe_absolute_location") var safeAbsoluteLocation: String,
    @ColumnInfo(name = "safe_salt") var safeSalt: String,
) {
    override fun toString(): String {
        return "data received = \n{" +
                "\n\tid\" = " + id +
                "\n\tsafeName\" = " + safeName +
                "\n\tsafeOwner\" = " + safeOwner +
                "\n\tsafeUsesMultiplePassword\" = " + safeUsesMultiplePassword +
                "\n\tsafePartialKey\" = " + safePartialKey +
                "\n\tpersonalAccessOnly\" = " + personalAccessOnly +
                "\n\tencryptionAlgorithm\" = " + encryptionAlgorithm +
                "\n\tsafeCreated\" = " + safeCreated +
                "\n\tsafeAbsoluteLocation\" = " + safeAbsoluteLocation +
                "\n\tsafeSalt\" = " + safeSalt +
                "\n}"
    }

    override fun equals(other: Any?): Boolean {
        return try {
            val x = other as SafeData
            return id == x.id &&
                    safeName == x.safeName &&
                    safeOwner == x.safeOwner &&
                    safeUsesMultiplePassword == x.safeUsesMultiplePassword &&
                    safePartialKey == x.safePartialKey &&
                    personalAccessOnly == x.personalAccessOnly &&
                    encryptionAlgorithm == x.encryptionAlgorithm &&
                    safeCreated == x.safeCreated &&
                    safeAbsoluteLocation == x.safeAbsoluteLocation &&
                    safeSalt == x.safeSalt
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}