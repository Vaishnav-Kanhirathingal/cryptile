package com.example.cryptile.data_classes

import android.os.Environment
import android.util.Log
import com.example.cryptile.app_data.room_files.SafeData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.text.SimpleDateFormat

private const val TAG = "SafeFiles"

data class SafeFiles(
    val fileName: String,
    val fileCreated: String,
    val fileSize: String
) {

    companion object {
        const val root = "/storage/emulated/0/"
        const val metaDataFileName = "META_DATA.txt"
        const val safeDataFolder = "DATA"
        const val logFileName = "Log.txt"

        /**
         * This takes path of a metadata file of a safe as a parameter.
         * Then, using that file, extracts the safe data values. It then
         * checks if the safeData value for safe's path has changed and
         * records this changes into the meta-data file.
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
         * this function updates the data held inside the metadata file
         * this function assumes the metadata file is at the location
         * specified in the safeData object it takes as parameter.
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
         * get the safe root path from the safe-data variable.
         * Append the value of variable safeDataFolder and
         * logFile to the rootPath provided. This gives the
         * location of the log file. take the string and add
         * it to the end of the log file.
         */
        fun saveChangesToLogFile(
            string: String,
            safeRootPath: String,
            safeName: String
        ) {
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

        fun importFileToSafe(
            absoluteFilePath: String,
            safeMasterKey: String,
            safePath: String
        ) {
            // TODO: generate folders an files from the given file path and master key.
        }

        /**
         * create a random string of some fixed length.
         * use this function twice if required.
         */
        fun createPartialKey(passwordOne: String): String {
            // TODO: implement using cryptographic libraries
            return "someKey"
        }

        /**
         * decrypt the partial key using given password and
         * return the result as key.
         */
        fun getKey(
            passwordOne: String,
            partialKeyOne: String
        ): String {
            // TODO: implement using cryptographic libraries
            return "generated key"
        }

        /**
         * generates key one and key two using the method mentioned
         * in get-key function. Once generated, encrpt key-two using
         * key one to get final key to be returned.
         */
        fun getKey(
            passwordOne: String,
            partialKeyOne: String,
            passwordTwo: String,
            partialKeyTwo: String,
        ): String {
            // TODO: implement using cryptographic libraries
            return "generated key"
        }
    }
}