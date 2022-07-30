package com.example.cryptile.app_data.room_files

import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
import com.google.firebase.auth.FirebaseAuth
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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "safe_name") var safeName: String,
    @ColumnInfo(name = "safe_owner") var safeOwner: String,
    @ColumnInfo(name = "safe_uses_multiple_password") var safeUsesMultiplePassword: Boolean,
    @ColumnInfo(name = "personal_access_only") var personalAccessOnly: Boolean,
    @ColumnInfo(name = "encryption_algorithm") var encryptionLevel: Int,
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

        const val safeDataDirectoryName = "DATA"
        const val testDirectoryName = "TEST"
        const val cacheDirectoryName = ".CACHE"
        const val exportDirectoryName = "EXPORTED_FILES"
        const val encStorageDirectoryName = "CRYPT_DATA"

        val randomFileName get() = UUID.randomUUID().toString()

        const val testSizeLimit = 50
        const val encFileLimit = 1_000_000

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
         * @param [keyList] order should start with key from password one, password two if present then
         * private key if required.
         */
        fun encrypt(byteArray: ByteArray, keyList: List<SecretKey>): ByteArray? {
            try {
                var returnable = byteArray
                for (i in keyList) {
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
         * So, the keys should be in the same order as when provided for encryption.
         * @param [keyList] order should start with key from password one, password two if present then
         * private key if required.
         */
        fun decrypt(byteArray: ByteArray, keyList: List<SecretKey>): ByteArray? {
            try {
                var returnable = byteArray
                for (i in keyList.reversed()) {
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
         * @param [path] is the storage path, shouldn't start with emulated.
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
                Log.d(TAG, "safe data file generated = \n$this")
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
                    encryptionLevel == x.encryptionLevel &&
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
     * these are generated using the master-key list provided.
     * @param [onSuccess] action to be performed on Success.
     * @param [onFailure] action to be performed on Failure.
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
            Environment.getExternalStorageDirectory(), "$safeAbsoluteLocation/${testDirectoryName}"
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
            "$safeAbsoluteLocation/${safeDataDirectoryName}"
        ).apply { if (!this.exists()) this.mkdirs() }
        File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/${cacheDirectoryName}"
        ).apply { if (!this.exists()) this.mkdirs() }

        saveChangesToLogFile(
            action = "creation",
            string = "\t\t\t\t-------------SAFE-CREATED-------------\t\t\t\t"
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
     * formats and saves the given strings to the log file. Takes the time and user into account for
     * logging.
     * @param [action] can be up to 20 character long.
     * @param [string] this string specifies in detail what the action did to the safe.
     */
    private fun saveChangesToLogFile(
        action: String,
        string: String,
    ) {
        val fixSize: (String, Int) -> String = { shortString: String, length: Int ->
            if (shortString.length > length - 1) {
                action.substring(0, length - 1)
            } else {
                action.padEnd(length, '-')
            }
        }
        val user = FirebaseAuth.getInstance().currentUser!!.uid
        FileWriter(
            File(Environment.getExternalStorageDirectory(), "$safeAbsoluteLocation/$logFileName"),
            true
        ).apply {
            append(
                SimpleDateFormat("[yyyy-MM-dd]-[HH:mm:ss.SSS] | ").format(System.currentTimeMillis()) +
                        "[$safeName] | " +
                        "[$user] | " +
                        "[${fixSize(action, 20)}] | " +
                        "[$string]\n"
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
            "$safeAbsoluteLocation/$cacheDirectoryName"
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
     * @param oldKey this is the list of keys used to encrypt the safe previously.
     * @param newKey this is the new list of keys to be used to encrypt the safe.
     */
    fun changeEncryption(
        context: Context,
        layoutInflater: LayoutInflater,
        oldKey: List<SecretKey>,
        newKey: List<SecretKey>,
    ) {
        val fileList = getDataFileList()
        if (fileList.isNotEmpty()) {
            for (safeFile in fileList) {
                val curDir = File(
                    Environment.getExternalStorageDirectory(),
                    "$safeAbsoluteLocation/$safeDataDirectoryName/${safeFile.fileDirectory}/$encStorageDirectoryName"
                )
                AdditionalPrompts.initializeLoading(
                    layoutInflater = layoutInflater,
                    context = context,
                    title = "Changing encryption for ${safeFile.fileNameUpperCase}"
                )
                val size = curDir.listFiles()!!.size
                var iterator = 0
                for (file in curDir.listFiles()!!) {
                    Thread.sleep(100)
                    AdditionalPrompts.addProgress(
                        progress = (iterator * 100) / size,
                        dismiss = false
                    )
                    val originalByteArray = file.readBytes()
                    file.writeBytes(encrypt(decrypt(originalByteArray, oldKey)!!, newKey)!!)
                    iterator++
                }
                AdditionalPrompts.addProgress(
                    progress = (iterator * 100) / size,
                    dismiss = true
                )
                saveChangesToLogFile(
                    action = "encryption change",
                    string = "for ${safeFile.fileDirectory} " +
                            "of extension ${safeFile.extension} " +
                            "and size ${safeFile.fileSize}"
                )
            }
        }
        File(
            Environment.getExternalStorageDirectory(), "$safeAbsoluteLocation/${testDirectoryName}"
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
     * @param keyList takes list of keys used for encryption and decryption
     */
    fun exportAll(
        keyList: List<SecretKey>,
        context: Context,
        layoutInflater: LayoutInflater
    ) {
        for (i in getDataFileList()) {
            export(i, keyList, context, layoutInflater)
        }
    }

    /**
     * takes safe file as a parameter along with the key list. It gets the file as byte array and
     * decrypts it using the key list. Once decryption is complete, it creates a new file inside
     * the export directory with the same name as the file's name.
     * @param safeFile the safe file to export.
     * @param keyList takes list of keys used for encryption and decryption.
     */
    fun export(
        safeFile: SafeFiles,
        keyList: List<SecretKey>,
        context: Context,
        layoutInflater: LayoutInflater
    ) {
        val file = File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/" +
                    "$exportDirectoryName/" +
                    "${safeFile.fileNameUpperCase}${safeFile.extension}"
        )

        readFromWriteTo(
            from = safeFile,
            destinationFile = file,
            keyList = keyList,
            context = context,
            layoutInflater = layoutInflater
        )
        Log.d(TAG, "exported file in directory: ${safeFile.fileDirectory}")
        saveChangesToLogFile(
            action = "export",
            string = "from directory ${safeFile.fileDirectory} " +
                    "of extension ${safeFile.extension} " +
                    "of size - ${safeFile.fileSize} " +
                    "to $exportDirectoryName folder in safe root"
        )
    }


    /**
     * takes a safe file, decrypts it and stores it into the destination file.
     * @param from safe file to decrypt.
     * @param destinationFile file to store decrypted data. If already exists would be deleted and
     * recreated
     * @param keyList takes list of keys used for encryption and decryption.
     */
    private fun readFromWriteTo(
        from: SafeFiles,
        destinationFile: File,
        keyList: List<SecretKey>,
        context: Context,
        layoutInflater: LayoutInflater
    ) {
        val dir = File(
            Environment.getExternalStorageDirectory(),
            "${safeAbsoluteLocation}/$safeDataDirectoryName/${from.fileDirectory}/$encStorageDirectoryName"
        )

        // TODO: compensate in other functions if destination already exists
        val size = dir.list()!!.size

        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        AdditionalPrompts.initializeLoading(
            layoutInflater,
            context,
            "Decrypting file - ${from.fileNameUpperCase}"
        )
        for (i in 0 until size) {
            Thread.sleep(100)
            AdditionalPrompts.addProgress(
                progress = (i * 100) / size,
                dismiss = false
            )
            val fileBytes = File(dir, "${encryptedFileName}_$i").readBytes()
            destinationFile.appendBytes(decrypt(fileBytes, keyList)!!)
        }
        AdditionalPrompts.addProgress(
            progress = 100,
            dismiss = true
        )
    }

    /**
     * takes a safeFile parameter and uses its properties to delete it from the safe.
     * @param safeFile the file to delete from safe
     */
    fun deleteFile(safeFile: SafeFiles) {
        File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/$safeDataDirectoryName/${safeFile.fileDirectory}"
        ).deleteRecursively()
        saveChangesToLogFile(
            action = "deletion",
            string = "deleted file from directory ${safeFile.fileDirectory} " +
                    "of extension ${safeFile.extension} " +
                    "of size - ${safeFile.fileSize} "
        )
    }

    /**
     * takes list of keys and the file to open. decrypts the file and opens it using context it
     * received as a parameter
     * @param keyList takes list of keys used for encryption and decryption.
     * @param safeFile the file to open from safe
     */
    fun openFile(
        keyList: List<SecretKey>,
        safeFile: SafeFiles,
        context: Context,
        layoutInflater: LayoutInflater,
        fileOpener: (File) -> Unit
    ) {
        val decryptedFile = File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/$cacheDirectoryName/${randomFileName}${safeFile.extension}"
        )
        readFromWriteTo(
            from = safeFile,
            destinationFile = decryptedFile,
            keyList = keyList,
            context = context,
            layoutInflater = layoutInflater
        )
        saveChangesToLogFile(
            action = "open file",
            string = "File directory opened - ${safeFile.fileDirectory}, " +
                    "file extension ${safeFile.fileDirectory}, " +
                    "file size - ${safeFile.fileSize}"
        )
        fileOpener(decryptedFile)
    }

    /**
     * this returns the name of every file and folder inside the '{safePath}/DATA' folder. This
     * list can be later used to display encrypted files as a list in the viewer fragment.
     */
    fun getDataFileList(): List<SafeFiles> {
        val listOfFiles = File(
            Environment.getExternalStorageDirectory(),
            "${safeAbsoluteLocation}/${safeDataDirectoryName}"
        ).listFiles()
        val finalList = mutableListOf<SafeFiles>()
        if (!listOfFiles.isNullOrEmpty()) {
            for (i in listOfFiles) {
                try {
                    finalList.add(
                        Gson().fromJson(
                            FileReader(File(i, safeDataFileName)).readText(),
                            SafeFiles::class.java
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
        return finalList
    }

    /**
     * takes absolute file path of the selected file, safe master key for encryption, safe path
     * to store the encrypted file inside the safe.
     * @param [keyList] safe's key list
     */
    fun importFileToSafe(
        fileAbsolutePath: String,
        keyList: List<SecretKey>,
        context: Context,
        layoutInflater: LayoutInflater
    ) {
        val safeFile = getSafeFileEnum(fileAbsolutePath)
        val file = File(Environment.getExternalStorageDirectory(), fileAbsolutePath)

        val inputFileSize = Files
            .readAttributes(file.toPath(), BasicFileAttributes::class.java).size()
        Log.d(TAG, "file size = $inputFileSize")

        val destinationDirectory = File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsoluteLocation/$safeDataDirectoryName/${safeFile.fileDirectory}"
        )
        destinationDirectory.mkdirs()


        val newDestinationDirectory = File(destinationDirectory, encStorageDirectoryName)
        newDestinationDirectory.mkdirs()

        var i: Long = 0
        var iterationCount = 0

        AdditionalPrompts.initializeLoading(layoutInflater, context, "Importing file")
        val parts = (inputFileSize / encFileLimit) + 1

        val inputStream = BufferedInputStream(FileInputStream(file))
        while (i < inputFileSize) {
            Thread.sleep(100)
            AdditionalPrompts.addProgress(((iterationCount * 100) / parts).toInt(), false)
            val currentLimit =
                if (inputFileSize - i > encFileLimit) {
                    encFileLimit
                } else {
                    (inputFileSize - i).toInt()
                }
            Log.d(TAG, "currentLimit = $currentLimit, iteration i = $i")
            val cacheArray = ByteArray(currentLimit)
            inputStream.read(cacheArray, 0, currentLimit)

            File(newDestinationDirectory, "${encryptedFileName}_$iterationCount")
                .writeBytes(encrypt(cacheArray, keyList)!!)

            iterationCount += 1
            i += currentLimit
        }
        AdditionalPrompts.addProgress(100, true)
        inputStream.close()



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
        Log.d(TAG, "fileAbsolutePath = $fileAbsolutePath")
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
                "$safeAbsoluteLocation/$safeDataDirectoryName/$randomDirectoryName"
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
     * @param passwordOne generates a key using the given string.
     * @return returns the generated key as a list of secret keys
     */
    fun getKey(
        passwordOne: String
    ): MutableList<SecretKey> {
        return mutableListOf(generateKeyFromPassword(passwordOne.ifEmpty { DEFAULT_PASSWORD }))
    }

    /**
     * @param passwordOne generates first key using the given string.
     * @param passwordTwo generates second key using the given string.
     * @return returns the generated keys from both strings as a list of secret keys.
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
                    password.toCharArray(),
                    saltString.toByteArray(),
                    when (encryptionLevel) {
                        1 -> 16_384
                        2 -> 65_536
                        else -> 262_144
                    },
                    256
                )
            ).encoded, "AES"
        )
    }

    /**
     * takes list of master key as parameter then takes the test file content from safe, encrypts
     * it, then checks if the encrypted string is the same as the encrypted test file content.
     * if same for every line in the file, returns true else false.
     */
    fun checkKeyGenerated(keyList: List<SecretKey>): Boolean {
        val cipherReader =
            BufferedReader(
                FileReader(
                    File(
                        Environment.getExternalStorageDirectory(),
                        "$safeAbsoluteLocation/${testDirectoryName}/${encryptedTestFileName}"
                    )
                )
            )
        val plainReader =
            BufferedReader(
                FileReader(
                    File(
                        Environment.getExternalStorageDirectory(),
                        "$safeAbsoluteLocation/${testDirectoryName}/${unencryptedTestFileName}"
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
                        keyList
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