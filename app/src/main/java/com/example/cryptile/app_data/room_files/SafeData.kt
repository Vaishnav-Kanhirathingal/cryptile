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
    @ColumnInfo(name = "safe_partial_password_one") var safePartialPasswordOne: String,
    @ColumnInfo(name = "safe_partial_password_two") var safePartialPasswordTwo: String?,
    @ColumnInfo(name = "personal_access_only") var personalAccessOnly: Boolean,
    @ColumnInfo(name = "encryption_algorithm") var encryptionAlgorithm: String,
    @ColumnInfo(name = "safe_created") var safeCreated: Long,
    
    @ColumnInfo(name = "test_plain") var testPlain: String,
    @ColumnInfo(name = "test_cipher") var testCipher: String,
    @ColumnInfo(name = "safe_absolute_location") var safeAbsoluteLocation: String,
) {
    override fun toString(): String {
        return "data received = \n{" +
                "\n\tid = $id" +
                "\n\tsafeName = $safeName" +
                "\n\tsafeOwner = $safeOwner" +
                "\n\tsafeUsesMultiplePassword = $safeUsesMultiplePassword" +
                "\n\tsafePartialPasswordOne = $safePartialPasswordOne" +
                "\n\tsafePartialPasswordTwo = $safePartialPasswordTwo" +
                "\n\tpersonalAccessOnly = $personalAccessOnly" +
                "\n\tencryptionAlgorithm = $encryptionAlgorithm" +
                "\n\tsafeCreated = $safeCreated" +
                "\n\ttestPlain = $testPlain" +
                "\n\ttestCipher = $testCipher" +
                "\n\tsafeAbsoluteLocation = $safeAbsoluteLocation" +
                "\n}"
    }
}