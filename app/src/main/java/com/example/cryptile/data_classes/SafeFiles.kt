package com.example.cryptile.data_classes

import android.util.Log
import com.example.cryptile.app_data.room_files.SafeData.Companion.ivSpec
import com.example.cryptile.app_data.room_files.SafeData.Companion.rootDirectory
import com.google.gson.Gson
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
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
) {
    companion object {

        /**
         * gets extension string. eg - ".mp4", ".acc", etc. then categorizes it into one of multiple
         * enum file types.
         */
        fun getFileType(extension: String): FileType {
            when (extension.uppercase(Locale.getDefault())) {
                in listOf(
                    ".WEBM", ".MPG", ".MPEG", ".MPV", ".OGG", ".MP4", ".AVI", ".MOV", ".SWF"
                ) -> {
                    return FileType.VIDEO
                }
                in listOf(
                    ".M4A", ".FLAC", ".MP3", ".WAV", ".AAC",
                    ".PCM", ".AIFF", ".OGG", ".WMA", ".ALAC"
                ) -> {
                    return FileType.AUDIO
                }
                in listOf(
                    ".GIF", ".JPG", ".PNG", ".GIF", ".WEBP", ".TIFF", ".PSD", ".RAW",
                    ".BMP", ".HEIF", ".INDD", ".JPEG", ".SVG", ".AI", ".EPS", ".PDF"
                ) -> {
                    return FileType.IMAGE
                }
                in listOf(
                    ".PDF", ".WORDX", ".XLS", ".XLSX", ".XLSB", ".DOC", ".DOCX"
                ) -> {
                    return FileType.DOCUMENT
                }
                in listOf(
                    ".ZIP", ".7Z", ".ARJ", ".DEB", ".PKG", ".RAR", ".RPM", ".TAR", ".GZ", ".Z"
                ) -> {
                    return FileType.COMPRESSED
                }
                in listOf(".TXT") -> {
                    return FileType.TEXT
                }
                else -> {
                    return FileType.UNKNOWN
                }
            }
        }

        /**
         * encrypts byte array using key given.
         */
        fun encrypt(byteArray: ByteArray, key: SecretKey): ByteArray? {
            try {
                val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
                return Base64.getEncoder().encode(cipher.doFinal(byteArray))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * decrypts encrypted byte array using key given.
         */
        fun decrypt(byteArray: ByteArray, key: SecretKey): ByteArray? {
            try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
                return cipher.doFinal(Base64.getDecoder().decode(byteArray))
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
         * takes file's absolute path and returns a SafeFiles object
         */
        fun getSafeFileEnum(fileAbsolutePath: String): SafeFiles {
            val pointerIndex = fileAbsolutePath.lastIndexOf('.')
            val extensionType = fileAbsolutePath.substring(pointerIndex, fileAbsolutePath.length)
            val fileName =
                fileAbsolutePath.substring(fileAbsolutePath.lastIndexOf('/') + 1, pointerIndex)
            val file = File("$rootDirectory/$fileAbsolutePath")
            val size = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).size()
            val bytes = ByteArray(size.toInt())
            BufferedInputStream(FileInputStream(file)).apply {
                this.read(bytes, 0, bytes.size)
                this.close()
            }
            return SafeFiles(
                fileNameUpperCase = fileName.uppercase(Locale.getDefault()),
                fileAdded = SimpleDateFormat("yyyy/MM/dd").format(System.currentTimeMillis()),
                fileSize = getSize(
                    size
                ),
                fileType = getFileType(extensionType),
                extension = extensionType
            )
        }

        //-----------------------------------------------------------------------------above-working
        //-----------------------------------------------------------------------------above-working
        //-----------------------------------------------------------------------------above-working


        // TODO: remove
        fun test(masterKey: SecretKey) {
            val byteArray = ByteArray(20) { (it).toByte() }
            val encrypted = encrypt(byteArray, masterKey)
            Log.d(TAG, "encrypted - ${String(encrypted!!)}")
            val unencrypted = decrypt(encrypted, masterKey)
            if (byteArray.contentEquals(unencrypted)) {
                Log.d(TAG, "test passed, enc, denc correct")
            } else {
                Log.d(
                    TAG, "test fail, enc, denc incorrect, values -" +
                            "\n1 - $byteArray" +
                            "\n2 - $unencrypted"
                )
            }
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