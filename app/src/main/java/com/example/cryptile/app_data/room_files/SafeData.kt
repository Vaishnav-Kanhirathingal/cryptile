package com.example.cryptile.app_data.room_files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cryptile.data_classes.SafeFiles
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


private const val TAG = "SafeData"

@Entity(tableName = "safe_database")
class SafeData(
    // TODO: randomize id to avoid safe conflicts
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "safe_name") var safeName: String,
    @ColumnInfo(name = "safe_owner") var safeOwner: String,
    @ColumnInfo(name = "safe_uses_multiple_password") var safeUsesMultiplePassword: Boolean,
    @ColumnInfo(name = "personal_access_only") var personalAccessOnly: Boolean,
    @ColumnInfo(name = "encryption_algorithm") var encryptionAlgorithm: String,
    @ColumnInfo(name = "safe_created") var safeCreated: Long,
    @ColumnInfo(name = "hide_safe_path") var hideSafePath: Boolean,
    @ColumnInfo(name = "safe_absolute_location") var safeAbsoluteLocation: String,
    @ColumnInfo(name = "safe_salt") var safeSalt: String,
) {
    companion object {
        const val DEFAULT_PASSWORD = "DEFAULT"// TODO: remove since password wont be empty

        const val metaDataFileName = "META_DATA.txt"
        const val logFileName = "Log.txt"
        const val unencryptedTestFileName = "U_ETF_CRYPTILE.txt"
        const val encryptedTestFileName = "ETF_CRYPTILE.txt"

        const val safeDataFileName = ".SAFE_FILES_META.txt"
        const val encryptedFileName = "ENC_FILE.CRYPTILE"

        const val safeDataDirectory = "DATA"
        const val testDirectory = "TEST"
        const val cacheDirectory = ".CACHE"
        const val exportDirectoryName = "EXPORTED_FILES"

        val decryptedFileName = UUID.randomUUID().toString()

        const val testSizeLimit = 50

        private val ivSpec =
            IvParameterSpec(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))

        /**
         * takes key converted to string as parameter and converts it back to the initial key.
         */
        fun stringToKey(string: String): SecretKey {
            val decodedKey = Base64.getDecoder().decode(string)
            return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
        }

        /**
         * takes key as parameter and converts it to string such that is can be extracted back from
         * the string outputted
         */
        fun keyToString(key: SecretKey) = Base64.getEncoder().encodeToString(key.encoded)!!

        /**
         * creates a random string of some fixed length.This can be used to get a string of required
         * length to be a key Use this function twice if required.
         */
        fun createRandomKey(): String {
            KeyGenerator.getInstance("AES").apply {
                init(256)
                return Base64.getEncoder().encodeToString(generateKey().encoded)
            }
        }

        /**
         * encrypts byte array using key list given.
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
         * decrypts encrypted byte array using key list given. the order of the keys are reversed.
         * So, the keys should be in the same order as when provided for encryption
         */
        fun decrypt(byteArray: ByteArray, key: List<SecretKey>): ByteArray? {
            try {
                var returnable = byteArray
                for (i in key.reversed()) {
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                    cipher.init(Cipher.DECRYPT_MODE, i, ivSpec)
                    returnable = cipher.doFinal(Base64.getDecoder().decode(returnable))
                }
                return returnable
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }


        /**
         * This takes path of a metadata file of a safe as a parameter. Then, using that file,
         * extracts the safe data values. It then checks if the safeData value for safe's path has
         * changed and records this changes into the meta-data file.
         */
        fun load(path: String): SafeData {
            val reader =
                BufferedReader(FileReader(File(Environment.getExternalStorageDirectory(), path)))
            var nextLine = reader.readLine()
            var fileDataString = ""
            while (!nextLine.isNullOrEmpty()) {
                fileDataString += "\n$nextLine"
                nextLine = reader.readLine()
            }
            Log.d(TAG, "string received = $fileDataString")
            Gson().fromJson(fileDataString, SafeData::class.java).apply {
                if (this.safeAbsoluteLocation != path) {
                    this.safeAbsoluteLocation = path.removeSuffix("/$metaDataFileName")
                    this.saveChangesToMetadata()
                }
                Log.d(TAG, "safe data file generated = \n${this.toString()}")
                return this
            }
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    override fun equals(other: Any?): Boolean {
        return try {
            val x = other as SafeData
            return id == x.id &&
                    safeName == x.safeName &&
                    safeOwner == x.safeOwner &&
                    safeUsesMultiplePassword == x.safeUsesMultiplePassword &&
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

    //--------------------------------------------------------------------------------safe-functions

    /**
     * this function generates necessary directories for all safe files. these directories include
     * the cache, export and the data directories. an additional test directory with one plain test
     * and it's corresponding cipher text is also generated to check if the key received is correct.
     * these are generated using the master-key list provided
     */
    fun generateDirectories(
        masterKey: List<SecretKey>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val fileDirectory = File(Environment.getExternalStorageDirectory(), safeAbsoluteLocation)
        if (fileDirectory.exists()) {
            onFailure()
            return
        } else {
            fileDirectory.mkdirs()
        }
        File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/${exportDirectoryName}"
        ).mkdirs()
        File(
            Environment.getExternalStorageDirectory(), "$safeAbsoluteLocation/${testDirectory}"
        ).apply {
            this.mkdirs()
            val plainWriter = FileWriter(File(this, unencryptedTestFileName))
            val cipherWriter = FileWriter(File(this, encryptedTestFileName))

            for (i in 0..testSizeLimit) {
                val generatedString = UUID.randomUUID().toString()
                val cipher = String(
                    encrypt(
                        generatedString.toByteArray(StandardCharsets.ISO_8859_1),
                        masterKey
                    )!!
                )
                plainWriter.append("$generatedString\n")
                cipherWriter.append("$cipher\n")

            }
            plainWriter.apply { flush();close() }
            cipherWriter.apply { flush();close() }
        }
        File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/${safeDataDirectory}"
        ).apply { if (!this.exists()) this.mkdirs() }
        File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/${cacheDirectory}"
        ).apply { if (!this.exists()) this.mkdirs() }

        saveChangesToLogFile(
            action = "creation",
            string = "\t\t\t\t-------------safe-created-------------\n"
        )
        onSuccess()
    }

    /**
     * this function updates the data held inside the metadata file this function assumes the
     * metadata file is at the location specified in the safeData object it takes as parameter.
     */
    fun saveChangesToMetadata() {
        try {
            Log.d(TAG, "attempting changes")
            val writer = FileWriter(
                File(
                    File(
                        Environment.getExternalStorageDirectory(), safeAbsoluteLocation
                    ),
                    metaDataFileName
                )
            )
            writer.append(GsonBuilder().setPrettyPrinting().create().toJson(this))
            writer.flush()
            writer.close()
            saveChangesToLogFile(
                "Updating safeData",
                "metadata file updated, new meta-data - $this"
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * this function is a logging function which takes a string as parameter then, appends it to the
     * end of a string with the current time and safeName and writes it to the log text file.
     */
    fun saveChangesToLogFile(
        action: String,
        string: String
    ) {
        FileWriter(
            File(
                File(Environment.getExternalStorageDirectory(), safeAbsoluteLocation),
                logFileName
            ),
            true
        ).apply {
            append(
                SimpleDateFormat("[yyyy|MM|dd | HH:mm:ss:SSS]").format(System.currentTimeMillis())
                        + " - [$safeName] |\t[$action] |\t$string\n"
            );flush();close()
        }
    }

    /**
     * deletes safe directories entirely using recursive deletion
     */
    fun deleteSafe() {
        File(Environment.getExternalStorageDirectory(), safeAbsoluteLocation).deleteRecursively()
    }

    /**
     * Clears content of '.CACHE' directory within the safe. Does not delete the directory.
     */
    fun clearCache() {
        for (child in File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/$cacheDirectory"
        ).listFiles()!!) {
            child.delete()
        }
        saveChangesToLogFile(
            action = "clear cache",
            string = "cache cleared from cache directory."
        )
    }

    //---------------------------------------------------------------------------safe-data-functions

    /**
     * this function is used to change the encryption of the safe after it undergoes a password
     * change. The function takes a file at a time, uses the old key list to decrypt it, then,
     * uses the new key list to encrypt it.
     */
    fun changeEncryption(
        oldKey: List<SecretKey>,
        newKey: List<SecretKey>,
    ) {
        val listOfFiles = File(
            Environment.getExternalStorageDirectory(),
            "${safeAbsoluteLocation}/${safeDataDirectory}"
        ).listFiles()
        // TODO: use size to display loading
        val size = listOfFiles!!.size.toFloat()
        if (!listOfFiles.isNullOrEmpty()) {
            for (i in listOfFiles) {
                // TODO: attempt to use get list of safe files
                val originalFile = File(i, encryptedFileName)
                val originalByte = ByteArray(
                    Files.readAttributes(originalFile.toPath(), BasicFileAttributes::class.java)
                        .size().toInt()
                )
                BufferedInputStream(FileInputStream(originalFile)).apply {
                    read(originalByte, 0, originalByte.size);close()
                }
                val newByteArray = encrypt(decrypt(originalByte, oldKey)!!, newKey)!!
                originalFile.writeBytes(newByteArray)
            }
        }
        File(
            Environment.getExternalStorageDirectory(), "$safeAbsoluteLocation/${testDirectory}"
        ).apply {
            val plainTextReader = BufferedReader(FileReader(File(this, unencryptedTestFileName)))
            val cipherWriter = FileWriter(File(this, encryptedTestFileName))
            cipherWriter.write("")
            for (i in 0..testSizeLimit) {
                val pt = plainTextReader.readLine()
                val ct = String(encrypt(pt.toByteArray(StandardCharsets.ISO_8859_1), newKey)!!)
                cipherWriter.append("$ct\n")
                Log.d(TAG, "pt - $pt, ct - $ct")
            }
            plainTextReader.apply { close() }
            cipherWriter.apply { flush();close() }
        }
        saveChangesToLogFile(
            action = "password change",
            string = "encryption changed for all files"
        )
    }

    /**
     * takes key list and saves decrypted version of each file in the export directory
     */
    fun exportAll(keyList: List<SecretKey>) {
        for (i in getDataFileList()) {
            export(i, keyList)
        }
    }

    /**
     * takes safe file as a parameter along with the key list. It gets the file as byte array and
     * decrypts it using the key list. Once decryption is complete, it creates a new file inside
     * the export directory with the same name as the file's name
     */
    fun export(safeFile: SafeFiles, keyList: List<SecretKey>) {
        val dc = decrypt(getEncryptedByteArrayFromSafeFile(safeFile), keyList)!!
        val file = File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/" +
                    "$exportDirectoryName/" +
                    "${safeFile.fileNameUpperCase}.${safeFile.extension}"
        )
        file.writeBytes(dc)
        Log.d(TAG, "exported file in directory: ${safeFile.fileDirectory}")
        saveChangesToLogFile(
            action = "export",
            string = "exported file from directory ${safeFile.fileDirectory} " +
                    "of extension ${safeFile.extension} " +
                    "of size - ${SafeFiles.getSize(safeFile.fileSize)} " +
                    "to $exportDirectoryName folder in safe root"
        )
    }

    /**
     * takes a safeFile parameter and uses its properties to delete it from the safe
     */
    fun deleteFile(safeFile: SafeFiles) {
        File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/$safeDataDirectory/${safeFile.fileDirectory}"
        ).deleteRecursively()
        saveChangesToLogFile(
            action = "deletion",
            string = "deleted file from directory ${safeFile.fileDirectory} " +
                    "of extension ${safeFile.extension} " +
                    "of size - ${SafeFiles.getSize(safeFile.fileSize)} "
        )
    }

    /**
     * takes list of keys and the file to open. decrypts the file and opens it using context it
     * received as a parameter
     */
    fun openFile(safeMasterKey: List<SecretKey>, safeFile: SafeFiles, context: Context) {
        // TODO: implement properly, decrypted file shouldn't be stored on disc cache
        val encryptedByteArray = getEncryptedByteArrayFromSafeFile(safeFile)
        val decryptedFile = File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/$cacheDirectory/${decryptedFileName}.${safeFile.extension}"
        )
        decryptedFile.writeBytes(decrypt(encryptedByteArray, safeMasterKey)!!)
        saveChangesToLogFile(
            action = "open file",
            string = "File directory opened - ${safeFile.fileDirectory}, " +
                    "file extension ${safeFile.fileDirectory}, " +
                    "file size - ${safeFile.fileSize}"
        )

        val uri: Uri = Uri.fromFile(decryptedFile).normalizeScheme()
        val mime: String = getMimeType(uri.toString())!!
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = uri
        intent.type = mime
        context.startActivity(Intent.createChooser(intent, "Open file with"))
        // TODO: fix this
    }

    private fun getMimeType(url: String?): String? {
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)
        return if (ext != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        } else {
            null
        }

    }

    private fun getEncryptedByteArrayFromSafeFile(safeFile: SafeFiles): ByteArray {
        // TODO: implement properly, decrypted file shouldn't be stored on disc cache
        val encryptedFile = File(
            Environment.getExternalStorageDirectory(),
            "${safeAbsoluteLocation}/$safeDataDirectory/${safeFile.fileDirectory}/$encryptedFileName"
        )
        val encryptedByteArray = ByteArray(
            Files.readAttributes(encryptedFile.toPath(), BasicFileAttributes::class.java)
                .size().toInt()
        )
        try {
            BufferedInputStream(FileInputStream(encryptedFile)).apply {
                read(encryptedByteArray, 0, encryptedByteArray.size);close()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return encryptedByteArray
    }

    /**
     * this returns the name of every file and folder inside the '{safePath}/DATA' folder. This
     * list can be later used to display encrypted files as a list in the viewer fragment.
     */
    fun getDataFileList(): List<SafeFiles> {
        val listOfFiles = File(
            Environment.getExternalStorageDirectory(),
            "${safeAbsoluteLocation}/${safeDataDirectory}"
        ).listFiles()
        val finalList = mutableListOf<SafeFiles>()
        if (!listOfFiles.isNullOrEmpty()) {
            for (i in listOfFiles) {
                finalList.add(
                    Gson().fromJson(
                        FileReader(File(i, safeDataFileName)).readText(),
                        SafeFiles::class.java
                    )
                )
            }
        }
        return finalList
    }

    /**
     * takes absolute file path of the selected file, safe master key for encryption, safe path
     * to store the encrypted file inside the safe.
     */
    fun importFileToSafe(fileAbsolutePath: String, safeMasterKey: List<SecretKey>) {
        val safeFile = getSafeFileEnum(fileAbsolutePath)
        val file = File(Environment.getExternalStorageDirectory(), fileAbsolutePath)
        val originalFileByteArray = ByteArray(
            Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).size().toInt()
        )
        BufferedInputStream(FileInputStream(file)).apply {
            this.read(originalFileByteArray, 0, originalFileByteArray.size);this.close()
        }
        val destination = "$safeAbsoluteLocation/$safeDataDirectory/${safeFile.fileDirectory}"
        val destinationDirectory = File(Environment.getExternalStorageDirectory(), destination)
        destinationDirectory.mkdirs()

        File(
            File(Environment.getExternalStorageDirectory(), destination), encryptedFileName
        ).writeBytes(encrypt(originalFileByteArray, safeMasterKey)!!)
        FileWriter(File(destinationDirectory, safeDataFileName)).apply {
            append(GsonBuilder().setPrettyPrinting().create().toJson(safeFile));flush();close()
        }
        saveChangesToLogFile(
            action = "import",
            string = "File imported to " +
                    "directory - ${safeFile.fileDirectory}, " +
                    "extension - ${safeFile.extension}, " +
                    "file size - ${safeFile.fileSize}"
        )
    }

    /**
     * takes file's absolute path and returns a SafeFiles object
     */
    private fun getSafeFileEnum(fileAbsolutePath: String): SafeFiles {
        val pointerIndex = fileAbsolutePath.lastIndexOf('.')
        val extensionType = fileAbsolutePath.substring(pointerIndex, fileAbsolutePath.length)
        val fileName =
            fileAbsolutePath.substring(fileAbsolutePath.lastIndexOf('/') + 1, pointerIndex)
        val file = File(Environment.getExternalStorageDirectory(), fileAbsolutePath)
        val size = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).size()
        val bytes = ByteArray(size.toInt())
        BufferedInputStream(FileInputStream(file)).apply {
            this.read(bytes, 0, bytes.size)
            this.close()
        }
        var randomDirectoryName: String
        do randomDirectoryName = UUID.randomUUID().toString() while (
            File(
                Environment.getExternalStorageDirectory(),
                "$safeAbsoluteLocation/$safeDataDirectory/$randomDirectoryName"
            ).exists()
        )
        return SafeFiles(
            fileNameUpperCase = fileName.uppercase(Locale.getDefault()),
            fileAdded = SimpleDateFormat("yyyy/MM/dd").format(System.currentTimeMillis()),
            fileSize = size,
            fileType = SafeFiles.getFileType(extensionType),
            extension = extensionType,
            fileDirectory = randomDirectoryName
        )
    }

    //----------------------------------------------------------------------------safe-key-functions

    /**
     * decrypt the partial key using given password and return the result as key.
     */
    fun getKey(
        passwordOne: String
    ): MutableList<SecretKey> {
        return mutableListOf(generateKeyFromPassword(passwordOne.ifEmpty { DEFAULT_PASSWORD }))
    }

    /**
     * generates key one and key two using the method mentioned in get-key function. Once
     * generated, encrypt key-two using key one to get final key to be returned.
     */
    fun getKey(
        passwordOne: String,
        passwordTwo: String
    ): MutableList<SecretKey> {
        return mutableListOf(
            generateKeyFromPassword(passwordOne.ifEmpty { DEFAULT_PASSWORD }),
            generateKeyFromPassword(passwordTwo.ifEmpty { DEFAULT_PASSWORD })
        )
    }

    /**
     * takes a password string as parameter and generates a secret key from that key. the key
     * generated is always the same.
     */
    private fun generateKeyFromPassword(
        password: String
    ): SecretKey {
        val salt = safeSalt.toByteArray(StandardCharsets.ISO_8859_1)
        val saltString = Base64.getEncoder().encodeToString(salt)
        return SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(
                PBEKeySpec(
                    password.toCharArray(), saltString.toByteArray(), 65536, 256
                )
            ).encoded, "AES"
        )
    }

    /**
     * takes list of master key as parameter then takes the test file content from safe, encrypts
     * it, then checks if the encrypted string is the same as the encrypted test file content.
     * if same for every line in the file, returns true else false.
     */
    fun checkKeyGenerated(masterKey: List<SecretKey>): Boolean {
        val cipherReader =
            BufferedReader(
                FileReader(
                    File(
                        Environment.getExternalStorageDirectory(),
                        "$safeAbsoluteLocation/${testDirectory}/${encryptedTestFileName}"
                    )
                )
            )
        val plainReader =
            BufferedReader(
                FileReader(
                    File(
                        Environment.getExternalStorageDirectory(),
                        "$safeAbsoluteLocation/${testDirectory}/${unencryptedTestFileName}"
                    )
                )
            )
        for (i in 0..testSizeLimit) {
            val plain = plainReader.readLine()
            val cipher = cipherReader.readLine()
            val extractedText =
                String(
                    decrypt(
                        cipher.toByteArray(StandardCharsets.ISO_8859_1),
                        masterKey
                    )!!
                )
            if (plain == extractedText) {
                Log.d(TAG, "text matches for line $i")
            } else {
                saveChangesToLogFile(
                    action = "login",
                    string = "Login attempt failed"
                )
                return false
            }
        }
        saveChangesToLogFile(
            action = "login",
            string = "Login attempt succeeded"
        )
        return true
    }
}