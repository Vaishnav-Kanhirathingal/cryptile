package com.example.cryptile.data_classes

import android.os.Environment
import android.util.Log
import com.example.cryptile.app_data.room_files.SafeData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
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
    val fileName: String,
    val fileCreated: String,
    val fileSize: String
) {
    companion object {
        const val root = "/storage/emulated/0/"
        private const val metaDataFileName = "META_DATA.txt"
        private const val safeDataFolder = "DATA"
        private const val logFileName = "Log.txt"
        private const val testDirectory = "TEST"
        private const val unencryptedTestFileName = "UETF_CRYPTILE.txt"
        private const val encryptedTestFileName = "ETF_CRYPTILE.txt"

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
                    val cipher = encrypt(
                        generatedString.toByteArray(StandardCharsets.ISO_8859_1),
                        stringToKey(masterKey)
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
         * encrypts byte array using key given
         */
        private fun encrypt(byteArray: ByteArray, key: SecretKey): String? {
            try {
                val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
                return Base64.getEncoder().encodeToString(cipher.doFinal(byteArray))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * decrypts encrypted byte array using key given
         */
        private fun decrypt(byteArray: ByteArray, key: SecretKey): String? {
            try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
                return String(cipher.doFinal(Base64.getDecoder().decode(byteArray)))
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
            //remove from here
            CoroutineScope(Dispatchers.IO).launch {
                val salt = ByteArray(32)
                SecureRandom().nextBytes(salt)
                test(salt)
            }
            //to here
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
            val ecp = encrypt(passwordOne.toByteArray(StandardCharsets.UTF_8), key) ?: passwordOne
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
            val ecp = encrypt(
                passwordOne.toByteArray(StandardCharsets.UTF_8),
                key
            ) + encrypt(passwordTwo.toByteArray(StandardCharsets.UTF_8), key)
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
                    decrypt(cipher.toByteArray(StandardCharsets.ISO_8859_1), masterKey)
                if (plain == extractedText) {
                    Log.d(TAG, "text matches for line $i")
                } else {
                    return false
                }
            }
            return true
        }

        //-----------------------------------------------------------------------------above-working
        //-----------------------------------------------------------------------------above-working
        //-----------------------------------------------------------------------------above-working

        /**
         * takes absolute file path of the selected file, safe master key for encryption, safe path
         * to store the encrypted file inside the safe.
         */
        fun importFileToSafe(
            absoluteFilePath: String,
            safeMasterKey: String,
            safeAbsolutePath: String
        ) {
            // TODO: implement
        }

        fun openFile(key: SecretKey, actualName: String, safeAbsolutePath: String) {
            val filePath = "$root$safeAbsolutePath/$safeDataFolder/$actualName"
        }

        private fun test(salt: ByteArray) {
            val regen =
                String(salt, StandardCharsets.ISO_8859_1).toByteArray(StandardCharsets.ISO_8859_1)
            Log.d(
                TAG, "salts are ${
                    if (regen.contentEquals(salt)) "" else "un"
                }equal"
            )
        }

        fun deleteSafe() {
            // TODO: implement
        }
    }
}