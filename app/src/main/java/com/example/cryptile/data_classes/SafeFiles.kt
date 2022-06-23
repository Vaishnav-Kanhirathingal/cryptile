package com.example.cryptile.data_classes

import android.util.Log
import com.example.cryptile.app_data.room_files.SafeData.Companion.ivSpec
import com.google.gson.Gson
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


private const val TAG = "SafeFiles"

data class SafeFiles(
    val fileNameUpperCase: String,
    val extension: String,
    val fileAdded: String,
    val fileSize: String,
    val fileType: FileType,
    val fileDirectory: String
) {
    companion object {

        /**
         * gets extension string. eg - ".mp4", ".acc", etc. then categorizes it into one of multiple
         * enum file types.
         */
        fun getFileType(extension: String): FileType {
            return when (extension.uppercase(Locale.getDefault())) {
                in listOf(
                    ".WEBM", ".MPG", ".MPEG", ".MPV", ".OGG", ".MP4", ".AVI", ".MOV", ".SWF"
                ) -> FileType.VIDEO
                in listOf(
                    ".M4A", ".FLAC", ".MP3", ".WAV", ".AAC", ".PCM", ".AIFF", ".OGG", ".WMA",
                    ".ALAC"
                ) -> FileType.AUDIO
                in listOf(
                    ".GIF", ".JPG", ".PNG", ".GIF", ".WEBP", ".TIFF", ".PSD", ".RAW", ".BMP",
                    ".HEIF", ".INDD", ".JPEG", ".SVG", ".AI", ".EPS", ".PDF"
                ) -> FileType.IMAGE
                in listOf(
                    ".PDF", ".WORDX", ".XLS", ".XLSX", ".XLSB", ".DOC", ".DOCX"
                ) -> FileType.DOCUMENT
                in listOf(
                    ".ZIP", ".7Z", ".ARJ", ".DEB", ".PKG", ".RAR", ".RPM", ".TAR", ".GZ", ".Z"
                ) -> return FileType.COMPRESSED
                in listOf(".TXT") -> FileType.TEXT
                else -> FileType.UNKNOWN
            }
        }

        /**
         * encrypts byte array using key given.
         */
        fun encrypt(byteArray: ByteArray, key: List<SecretKey>): ByteArray? {
            try {
                var returnable = byteArray
                for (i in key) {
                    val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.ENCRYPT_MODE, i, ivSpec)
                    returnable = Base64.getEncoder().encode(cipher.doFinal(returnable))
                }
                return returnable
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * decrypts encrypted byte array using key given.
         */
        fun decrypt(byteArray: ByteArray, key: List<SecretKey>): ByteArray? {
            try {
                var returnable = byteArray
                for (i in key.reversed()) {
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                    cipher.init(Cipher.DECRYPT_MODE, i, ivSpec)
                    returnable = cipher.doFinal(Base64.getDecoder().decode(byteArray))
                }
                return returnable
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * gets size of file as long and outputs human readable size formats.
         * example - 123456789 gives 123.45 MB
         */
        fun getSize(size: Long): String {
            var x = size
            val measureLimit: Long = 1000
            var i = 0
            val typeArray = listOf("B", "KB", "MB", "GB", "TB")
            var afterPoint = 0
            while (x > measureLimit && i < 5) {
                Log.d(TAG, "x = $x, i = $i")
                i++
                afterPoint = (x % 100).toInt()
                x /= measureLimit
            }
            return "${x}.$afterPoint ${typeArray[i]}"
        }

        /**
         * creates a random string of some fixed length.This can be used to get a string of required
         * length to be a key Use this function twice if required.
         */
        fun createRandomPartialKey(): String {
            KeyGenerator.getInstance("AES").apply {
                init(256)
                return Base64.getEncoder().encodeToString(generateKey().encoded)
            }
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

enum class FileType {
    UNKNOWN, IMAGE, VIDEO, AUDIO, DOCUMENT, COMPRESSED, TEXT,
}