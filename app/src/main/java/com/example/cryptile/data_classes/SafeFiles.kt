package com.example.cryptile.data_classes

import android.os.Environment
import android.util.Log
import com.example.cryptile.app_data.room_files.SafeData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import com.example.cryptile.data_classes.SafeFiles.Companion.safeDataFolder as safeDataFolder2


private const val TAG = "SafeFiles"

data class SafeFiles(
    val fileNameUpperCase: String,
    val fullFileName: String,
    val fileAdded: String,
    val fileSize: String,
    val fileType: FileType,
) {
    companion object {
        const val root = "/storage/emulated/0/"
        private const val metaDataFileName = "META_DATA.txt"
        private const val safeDataFolder = "DATA"
        private const val logFileName = "Log.txt"
        private const val testDirectory = "TEST"
        private const val unencryptedTestFileName = "UETF_CRYPTILE.txt"
        private const val encryptedTestFileName = "ETF_CRYPTILE.txt"
        private const val cacheDirectory = ".CACHE"

        // TODO: check this
        private const val encryptedFileName = "ENC_FILE.TXT"

        // TODO: check here
        private const val testSizeLimit = 50

        private val ivSpec =
            IvParameterSpec(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))

        /**
         * takes key converted to string as parameter and converts it back to the initial key.
         */
        fun stringToKey(encodedKey: String): SecretKey {
            val decodedKey = Base64.getDecoder().decode(encodedKey)
            return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
        }

        /**
         * takes key as parameter and converts it to string such that is can be extracted back from
         * the string outputted
         */
        fun keyToString(key: SecretKey) = Base64.getEncoder().encodeToString(key.encoded)!!

        /**
         * This takes path of a metadata file of a safe as a parameter. Then, using that file,
         * extracts the safe data values. It then checks if the safeData value for safe's path has
         * changed and records this changes into the meta-data file.
         */
        fun readMetaData(path: String): SafeData {
            val reader = BufferedReader(FileReader(File(root + path)))
            var nextLine = reader.readLine()
            var fileDataString = ""
            while (!nextLine.isNullOrEmpty()) {
                fileDataString += "\n$nextLine"
                nextLine = reader.readLine()
            }
            Log.d(TAG, "string received = $fileDataString")
            val finalData = Gson().fromJson(fileDataString, SafeData::class.java)
            if (finalData.safeAbsoluteLocation != path) {
                finalData.safeAbsoluteLocation = path.removeSuffix("/$metaDataFileName")
                saveChangesToMetadata(finalData)
            }
            Log.d(TAG, finalData.toString())
            return finalData
        }

        /**
         * this function updates the data held inside the metadata file this function assumes the
         * metadata file is at the location specified in the safeData object it takes as parameter.
         */
        fun saveChangesToMetadata(safeData: SafeData) {
            try {
                Log.d(TAG, "attempting changes")
                val writer = FileWriter(
                    File(
                        File(
                            Environment.getExternalStorageDirectory(), safeData.safeAbsoluteLocation
                        ),
                        metaDataFileName
                    )
                )
                writer.append(GsonBuilder().setPrettyPrinting().create().toJson(safeData))
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        /**
         * takes the safe root path from the safe-data variable. Appends the value of variable
         * safeDataFolder and logFile to the rootPath provided. This gives the location of the log
         * file. Then, it takes the string and add it to the end of the log file after appropriate
         * formatting.
         */
        fun saveChangesToLogFile(string: String, safeRootPath: String, safeName: String) {
            FileWriter(
                File(
                    File(Environment.getExternalStorageDirectory(), safeRootPath), logFileName
                ),
                true
            ).apply {
                append(
                    SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(System.currentTimeMillis())
                            + "-[$safeName]\t$string\n"
                )
                flush()
                close()
            }
        }

        /**
         * takes safe path to store test files at appropriate locations. Uses master key for
         * encryption. Generates a long string for encryption and stores it in the unencrypted file.
         * Then, encrypts the string and stores it into the encrypted file. This is also responsible
         * for generating data path to store encrypted files.
         */
        fun generateTestFilesAndStorageDirectory(safeAbsolutePath: String, masterKey: String) {
            File(
                Environment.getExternalStorageDirectory(), "$safeAbsolutePath/$testDirectory"
            ).apply {
                if (!this.exists()) {
                    this.mkdirs()
                }
                val plainWriter = FileWriter(File(this, unencryptedTestFileName))
                val cipherWriter = FileWriter(File(this, encryptedTestFileName))

                for (i in 0..testSizeLimit) {
                    val generatedString = UUID.randomUUID().toString()
                    val cipher = String(
                        encrypt(
                            generatedString.toByteArray(StandardCharsets.ISO_8859_1),
                            stringToKey(masterKey)
                        )!!
                    )
                    plainWriter.append("$generatedString\n")
                    cipherWriter.append("$cipher\n")

                }
                plainWriter.apply { flush();close() }
                cipherWriter.apply { flush();close() }
            }
            File(
                Environment.getExternalStorageDirectory(), "$safeAbsolutePath/$safeDataFolder2"
            ).apply { if (!this.exists()) this.mkdirs() }
        }

        /**
         * this returns the name of every file and folder inside the '{safePath}/DATA' folder. This
         * list can be later used to display encrypted files as a list in the viewer fragment.
         */
        fun getDataFileList(safeAbsolutePath: String): List<String> {
            // TODO: has to return files as list of SafeFiles
            val directory = File(
                Environment.getExternalStorageDirectory(), "${safeAbsolutePath}/$safeDataFolder"
            )
            val listOfFiles = directory.listFiles()
            val finalList = mutableListOf<String>()

            if (!listOfFiles.isNullOrEmpty()) {
                for (i in listOfFiles) {
                    finalList.add(i.name)
                }
            }
            return finalList
        }

        /**
         * encrypts byte array using key given. use Base64.getEncoder().encodeToString() to convert
         * to string. to convert string to byte array use string.toByteArray(StandardCharsets.ISO_8859_1)
         */
        private fun encrypt(byteArray: ByteArray, key: SecretKey): ByteArray? {
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
         * decrypts encrypted byte array using key given. use String() to convert to string. To
         * convert encrypted string to byte array use cipher.toByteArray(StandardCharsets.ISO_8859_1)
         */
        private fun decrypt(byteArray: ByteArray, key: SecretKey): ByteArray? {
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
         * creates a random string of some fixed length.This can be used to get a string of required
         * length to be a key Use this function twice if required.
         */
        fun createRandomPartialKey(): String {
            var originalKey: SecretKey
            KeyGenerator.getInstance("AES").apply {
                init(256)
                return Base64.getEncoder().encodeToString(generateKey().encoded)
            }
        }

        /**
         * takes a password string as parameter and generates a secret key from that key. the key
         * generated is always the same.
         */
        private fun generateKeyFromPassword(password: String, stringSalt: String): SecretKey {
            // TODO: get salt from safe data
            val salt = stringSalt.toByteArray(StandardCharsets.ISO_8859_1)
            val saltString = Base64.getEncoder().encodeToString(salt)
            return SecretKeySpec(
                SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(
                        PBEKeySpec(
                            password.toCharArray(), saltString.toByteArray(), 65536, 256
                        )
                    ).encoded, "AES"
            )
        }

        /**
         * decrypt the partial key using given password and return the result as key.
         */
        fun getKey(
            salt: String,
            safeIsPersonal: Boolean,
            partialKey: String,
            passwordOne: String,
        ): SecretKey {
            // TODO: implement personal
            val key = stringToKey(partialKey)
            val ecp = String(encrypt(passwordOne.toByteArray(StandardCharsets.UTF_8), key)!!)
            return generateKeyFromPassword(ecp, salt)
        }

        /**
         * generates key one and key two using the method mentioned in get-key function. Once
         * generated, encrypt key-two using key one to get final key to be returned.
         */
        fun getKey(
            salt: String,
            safeIsPersonal: Boolean,
            partialKey: String,
            passwordOne: String,
            passwordTwo: String,
        ): SecretKey {
            // TODO: implement personal
            val key = stringToKey(partialKey)
            val ecp = String(
                encrypt(
                    passwordOne.toByteArray(StandardCharsets.UTF_8),
                    generateKeyFromPassword(passwordTwo, salt)
                )!!
            )
            val returnable = generateKeyFromPassword(ecp, salt)
            Log.d(TAG, "key - ${keyToString(returnable)}")
            return returnable
        }

        /**
         * takes master key as parameter. it then takes the test file content from safe, encrypts
         * it, then checks if the encrypted string is the same as the encrypted test file content.
         * if same, returns true else false
         */
        fun checkKeyGenerated(masterKey: SecretKey, safeAbsolutePath: String): Boolean {
            val cipherReader =
                BufferedReader(FileReader(File("$root$safeAbsolutePath/$testDirectory/$encryptedTestFileName")))
            val plainReader =
                BufferedReader(FileReader(File("$root$safeAbsolutePath/$testDirectory/$unencryptedTestFileName")))
            for (i in 0..testSizeLimit) {
                val plain = plainReader.readLine()
                val cipher = cipherReader.readLine()
                val extractedText =
                    String(decrypt(cipher.toByteArray(StandardCharsets.ISO_8859_1), masterKey)!!)
                if (plain == extractedText) {
                    Log.d(TAG, "text matches for line $i")
                } else {
                    return false
                }
            }
            return true
        }

        /**
         * gets size of file as long and outputs human readable size formats.
         * example - 123456789 gives 123.45 MB
         */
        private fun getSize(size: Long): String {
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
         * gets extension string. eg - ".mp4", ".acc", etc. then categorizes it into one of multiple
         * enum file types.
         */
        private fun getFileType(extension: String): FileType {
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
         * takes file's absolute path and returns a SafeFiles object
         */
        private fun getFile(fileAbsolutePath: String): SafeFiles {
            val pointerIndex = fileAbsolutePath.lastIndexOf('.')
            val extensionType = fileAbsolutePath.substring(pointerIndex, fileAbsolutePath.length)
            val fileName =
                fileAbsolutePath.substring(fileAbsolutePath.lastIndexOf('/') + 1, pointerIndex)
            val file = File("$root/$fileAbsolutePath")
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
                fullFileName = fileName + extensionType
            )
        }

        //-----------------------------------------------------------------------------above-working
        //-----------------------------------------------------------------------------above-working
        //-----------------------------------------------------------------------------above-working

        /**
         * takes absolute file path of the selected file, safe master key for encryption, safe path
         * to store the encrypted file inside the safe.
         */
        fun importFileToSafe(
            fileAbsolutePath: String,
            safeMasterKey: String,
            safeAbsolutePath: String
        ): Boolean {
            // TODO: implement, use key
            val safeFile = getFile(fileAbsolutePath)
            val file = File("$root/$fileAbsolutePath")
            val originalFileByteArray = ByteArray(
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).size().toInt()
            )
            BufferedInputStream(FileInputStream(file)).apply {
                this.read(originalFileByteArray, 0, originalFileByteArray.size);this.close();
            }
            val destination = safeAbsolutePath + "/" +
                    safeDataFolder + "/" +
                    safeFile.fileNameUpperCase.uppercase(Locale.getDefault())
            val gsonValue = GsonBuilder().setPrettyPrinting().create().toJson(safeFile)
            Log.d(TAG, "gson created = \n$gsonValue\ndestination = $destination")
            Log.d(TAG, "size = ${originalFileByteArray.size}\nbyte array = ")
            File(
                Environment.getExternalStorageDirectory(), destination
            ).apply {
                if (!this.exists()) {
                    this.mkdirs()
                } else {
                    // TODO: return with warning
                }
                val encryptedArray = encrypt(originalFileByteArray, stringToKey(safeMasterKey))

                File(
                    File(Environment.getExternalStorageDirectory(), destination), encryptedFileName
                ).writeBytes(encryptedArray!!)
            }
            //
            test(stringToKey(safeMasterKey))
            openFile(safeMasterKey, safeAbsolutePath, safeFile)
            return true
        }

        fun openFile(
            safeMasterKey: String,
            safeAbsolutePath: String,
            safeFile: SafeFiles
        ) {
            // TODO: implement
            val encryptedFile = File(
                root + safeAbsolutePath + "/" +
                        safeDataFolder + "/" +
                        safeFile.fileNameUpperCase + "/" +
                        encryptedFileName
            )
            val encryptedByteArray = ByteArray(
                Files
                    .readAttributes(encryptedFile.toPath(), BasicFileAttributes::class.java)
                    .size()
                    .toInt()
            )
            // TODO: save bytes from file to encrypted byte array

            try {
                val buf = BufferedInputStream(FileInputStream(encryptedFile))
                buf.read(encryptedByteArray, 0, encryptedByteArray.size)
                buf.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }


            val originalByteArray = decrypt(encryptedByteArray, stringToKey(safeMasterKey))

            val cache = File(
                Environment.getExternalStorageDirectory(), "$safeAbsolutePath/$cacheDirectory"
            )
            if (!cache.exists()) {
                // TODO: generate cache at init
                cache.mkdirs()
            }
            File(cache, "decryptedFileName.txt").writeBytes(originalByteArray!!)
        }

        private fun test(masterKey: SecretKey) {
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

        fun deleteSafe() {
            // TODO: implement
        }
    }
}


enum class FileType {
    UNKNOWN, IMAGE, VIDEO, AUDIO, DOCUMENT, COMPRESSED, TEXT,
}