package com.example.cryptile.data_classes

import android.os.Environment
import android.util.Log
import com.example.cryptile.app_data.room_files.SafeData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.text.SimpleDateFormat
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
            // TODO: implement
            File(
                Environment.getExternalStorageDirectory(), "$safeAbsolutePath/$testDirectory"
            ).apply {
                if (!this.exists()) {
                    this.mkdirs()
                }
                FileWriter(File(this, unencryptedTestFileName)).apply {
                    append("UETF\nmaster key - $masterKey");flush();close()
                }
                FileWriter(File(this, encryptedTestFileName)).apply {
                    append("ETF");flush();close()
                }
            }
            File(
                Environment.getExternalStorageDirectory(), "$safeAbsolutePath/$safeDataFolder2"
            ).apply {
                if (!this.exists()) {
                    this.mkdirs()
                }
            }
        }

        /**
         * takes master key as parameter. it then takes the test file content from safe, encrypts
         * it, then checks if the encrypted string is the same as the encrypted test file content.
         * if same, returns true else false
         */
        fun checkKeyGenerated(masterKey: String): Boolean {
            // TODO: implement
            return true
        }

        /**
         * takes absolute file path of the selected file, safe master key for encryption, safe path
         * to store the encrypted file inside the safe.
         */
        fun importFileToSafe(
            absoluteFilePath: String,
            safeMasterKey: String,
            safePath: String
        ) {
            // TODO: implement
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
            for (i in listOfFiles) {
                finalList.add(i.name)
            }
            Log.d(TAG, "final list of directory items = ${finalList.toString()}")
            return finalList
        }

        /**
         * creates a random string of some fixed length.This can be used to get a string of required
         * length to be a key Use this function twice if required.
         */
        fun createPartialKey(): String {
            // TODO: implement
            return "someKey"
        }

        /**
         * decrypt the partial key using given password and return the result as key.
         */
        fun getKey(
            passwordOne: String,
            partialKeyOne: String
        ): String {
            // TODO: implement
            return "generated key"
        }

        /**
         * generates key one and key two using the method mentioned in get-key function. Once
         * generated, encrpt key-two using key one to get final key to be returned.
         */
        fun getKey(
            passwordOne: String,
            partialKeyOne: String,
            passwordTwo: String,
            partialKeyTwo: String,
        ): String {
            // TODO: implement
            return "generated key"
        }
    }
}