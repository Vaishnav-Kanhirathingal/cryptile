package com.example.cryptile.app_data.room_files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safe_database")
class SafeData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "safe_name") var safeName: String,
    @ColumnInfo(name = "safe_password") var safePassword: String,
    @ColumnInfo(name = "encryption_type") var encryptionType: String,
    @ColumnInfo(name = "safe_location") var safeLocation: String,
    @ColumnInfo(name = "safe_owner") var safeOwner: String
)