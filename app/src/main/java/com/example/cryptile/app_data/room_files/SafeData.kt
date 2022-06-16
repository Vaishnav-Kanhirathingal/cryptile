package com.example.cryptile.app_data.room_files

import android.os.Environment
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.data_classes.SafeFiles.Companion.decrypt
import com.example.cryptile.data_classes.SafeFiles.Companion.encrypt
import com.example.cryptile.data_classes.SafeFiles.Companion.getSafeFileEnum
import com.example.cryptile.data_classes.SafeFiles.Companion.test
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
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
    @ColumnInfo(name = "safe_partial_key") var safePartialKey: String,
    @ColumnInfo(name = "personal_access_only") var personalAccessOnly: Boolean,
    @ColumnInfo(name = "encryption_algorithm") var encryptionAlgorithm: String,
    @ColumnInfo(name = "safe_created") var safeCreated: Long,
    @ColumnInfo(name = "safe_absolute_location") var safeAbsoluteLocation: String,
    @ColumnInfo(name = "safe_salt") var safeSalt: String,
) {
    companion object {
        const val rootDirectory = "/storage/emulated/0"
        const val metaDataFileName = "META_DATA.txt"
        const val safeDataDirectory = "DATA"
        const val logFileName = "Log.txt"
        const val testDirectory = "TEST"
        const val unencryptedTestFileName = "U_ETF_CRYPTILE.txt"
        const val encryptedTestFileName = "ETF_CRYPTILE.txt"
        const val cacheDirectory = ".CACHE"
        val ivSpec =
            IvParameterSpec(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))

        // TODO: check below this
        val decryptedFileName = UUID.randomUUID().toString()
        const val encryptedFileName = "ENC_FILE.TXT"
        const val testSizeLimit = 50


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
         * This takes path of a metadata file of a safe as a parameter. Then, using that file,
         * extracts the safe data values. It then checks if the safeData value for safe's path has
         * changed and records this changes into the meta-data file.
         */
        fun load(path: String): SafeData {
            val reader = BufferedReader(FileReader(File("$rootDirectory/$path")))
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
                        Environment.getExternalStorageDirectory(), this.safeAbsoluteLocation
                    ),
                    metaDataFileName
                )
            )
            writer.append(GsonBuilder().setPrettyPrinting().create().toJson(this))
            writer.flush()
            writer.close()
            saveChangesToLogFile("metadata file updated, new meta-data - $this")
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
    fun saveChangesToLogFile(string: String) {
        FileWriter(
            File(
                File(Environment.getExternalStorageDirectory(), safeAbsoluteLocation),
                logFileName
            ),
            true
        ).apply {
            append(
                SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(System.currentTimeMillis())
                        + "-[$safeName] | $string\n"
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
            Environment.getExternalStorageDirectory(),
            "$safeAbsolutePath/${testDirectory}"
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
            Environment.getExternalStorageDirectory(),
            "$safeAbsolutePath/${safeDataDirectory}"
        ).apply { if (!this.exists()) this.mkdirs() }
        File(
            Environment.getExternalStorageDirectory(),
            "$safeAbsolutePath/${cacheDirectory}"
        ).apply { if (!this.exists()) this.mkdirs() }
    }

    /**
     * decrypt the partial key using given password and return the result as key.
     */
    fun getKey(
        safeIsPersonal: Boolean,
        passwordOne: String,
    ): SecretKey {
        // TODO: implement personal
        val key = stringToKey(safePartialKey)
        val ecp = String(encrypt(passwordOne.toByteArray(StandardCharsets.UTF_8), key)!!)
        return generateKeyFromPassword(ecp)
    }

    /**
     * generates key one and key two using the method mentioned in get-key function. Once
     * generated, encrypt key-two using key one to get final key to be returned.
     */
    fun getKey(
        safeIsPersonal: Boolean,
        passwordOne: String,
        passwordTwo: String,
    ): SecretKey {
        // TODO: implement personal
        val key = stringToKey(safePartialKey)
        val ecp = String(
            encrypt(
                passwordOne.toByteArray(StandardCharsets.UTF_8),
                generateKeyFromPassword(passwordTwo)
            )!!
        )
        val returnable = generateKeyFromPassword(ecp)
        Log.d(TAG, "key - ${keyToString(returnable)}")
        return returnable
    }


    /**
     * this returns the name of every file and folder inside the '{safePath}/DATA' folder. This
     * list can be later used to display encrypted files as a list in the viewer fragment.
     */
    fun getDataFileList(): List<String> {
        // TODO: has to return files as list of SafeFiles
        val directory = File(
            Environment.getExternalStorageDirectory(),
            "${safeAbsoluteLocation}/${safeDataDirectory}"
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
     * takes a password string as parameter and generates a secret key from that key. the key
     * generated is always the same.
     */
    fun generateKeyFromPassword(password: String): SecretKey {
        // TODO: get salt from safe data
        val salt = safeSalt.toByteArray(StandardCharsets.ISO_8859_1)
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
     * takes master key as parameter. it then takes the test file content from safe, encrypts
     * it, then checks if the encrypted string is the same as the encrypted test file content.
     * if same, returns true else false
     */
    fun checkKeyGenerated(masterKey: SecretKey): Boolean {
        val cipherReader =
            BufferedReader(FileReader(File("${rootDirectory}/$safeAbsoluteLocation/${testDirectory}/${encryptedTestFileName}")))
        val plainReader =
            BufferedReader(FileReader(File("${rootDirectory}/$safeAbsoluteLocation/${testDirectory}/${unencryptedTestFileName}")))
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
                saveChangesToLogFile("Login attempted, attempt failed")
                return false
            }
        }
        saveChangesToLogFile("Login attempted, attempt succeeded")
        return true
    }

    /**
     * takes absolute file path of the selected file, safe master key for encryption, safe path
     * to store the encrypted file inside the safe.
     */
    fun importFileToSafe(
        fileAbsolutePath: String,
        safeMasterKey: String
    ): Boolean {
        val safeFile = getSafeFileEnum(fileAbsolutePath)
        val file = File("${rootDirectory}/$fileAbsolutePath")
        val originalFileByteArray = ByteArray(
            Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).size().toInt()
        )
        BufferedInputStream(FileInputStream(file)).apply {
            this.read(originalFileByteArray, 0, originalFileByteArray.size);this.close()
        }
        val destination = safeAbsoluteLocation + "/" +
                safeDataDirectory + "/" +
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
                return false
            }
            val encryptedArray =
                encrypt(originalFileByteArray, stringToKey(safeMasterKey))

            File(
                File(Environment.getExternalStorageDirectory(), destination),
                encryptedFileName
            ).writeBytes(encryptedArray!!)
        }
        //
        test(stringToKey(safeMasterKey))
        openFile(safeMasterKey, safeAbsoluteLocation, safeFile)
        saveChangesToLogFile("File imported - ${safeFile.toString()}")
        return true
    }

    fun openFile(
        safeMasterKey: String,
        safeAbsolutePath: String,
        safeFile: SafeFiles
    ) {
        val encryptedFile = File(
            rootDirectory + "/" +
                    safeAbsolutePath + "/" +
                    safeDataDirectory + "/" +
                    safeFile.fileNameUpperCase + "/" +
                    encryptedFileName
        )
        val encryptedByteArray = ByteArray(
            Files
                .readAttributes(encryptedFile.toPath(), BasicFileAttributes::class.java)
                .size()
                .toInt()
        )
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
        File(
            cache,
            "${decryptedFileName}.${safeFile.extension}"
        ).writeBytes(originalByteArray!!)
        saveChangesToLogFile("File opened - ${safeFile.toString()}")
    }

    fun deleteSafe() {
        // TODO: implement
    }

    fun clearCache() {
        saveChangesToLogFile("cache cleared from cache file.")
        // TODO: implement
    }
}