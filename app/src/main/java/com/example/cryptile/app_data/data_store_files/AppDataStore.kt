package com.example.cryptile.app_data.data_store_files

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_data_store"
)

class AppDataStore(val context: Context) {
    private val default = "Default"

    //------------------------------------------------------------------------------------------keys
    private val userNameKey = stringPreferencesKey("user_name_key")
    private val userEmailKey = stringPreferencesKey("user_email_key")
    private val userPhoneKey = stringPreferencesKey("user_phone_key")

    private val userUsesFingerprintKey = booleanPreferencesKey("user_uses_fingerprint_key")
    private val userLoggedInKey = booleanPreferencesKey("user_logged_in_key")

    //------------------------------------------------------------------------------------value-flow
    val userNameFlow: Flow<String> = stringMapper(userNameKey)
    val userEmailFlow: Flow<String> = stringMapper(userEmailKey)
    val userPhoneFlow: Flow<String> = stringMapper(userPhoneKey)

    val userUsesFingerprintFlow: Flow<Boolean> = booleanMapper(userUsesFingerprintKey)
    val userLoggedInFlow: Flow<Boolean> = booleanMapper(userLoggedInKey)

    //----------------------------------------------------------------------------------------mapper
    /**
     * string mapper is used to access every string stored in the data store.
     */
    private fun stringMapper(key: Preferences.Key<String>): Flow<String> {
        return context.dataStore.data.catch { it.printStackTrace() }.map { it[key] ?: default }
    }

    /**
     * boolean mapper is used to access every boolean stored in the data store.
     */
    private fun booleanMapper(key: Preferences.Key<Boolean>): Flow<Boolean> {
        return context.dataStore.data.catch { it.printStackTrace() }.map { it[key] ?: false }
    }

    //---------------------------------------------------------------------------------------setters
    /**
     * savers are used to save the values they get as parameters to the datastore. String saver is
     * used to store string values. StoreString is an enum which is used to decide, under what key
     * name the value is to be stored.
     */
    suspend fun stringSaver(string: String, storeString: StoreString) {
        context.dataStore.edit {
            it[when (storeString) {
                StoreString.USER_NAME -> userNameKey
                StoreString.USER_EMAIL -> userEmailKey
                StoreString.USER_PHONE -> userPhoneKey
                else -> throw IllegalArgumentException("string type not specified in data store")
            }] = string
        }
    }

    /**
     * savers are used to save the values they get as parameters to the datastore. boolean saver is
     * used to store boolean values. storeBoolean is an enum which is used to decide, under what
     * key name the value is to be stored.
     */
    suspend fun booleanSaver(boolean: Boolean, storeBoolean: StoreBoolean) {
        context.dataStore.edit {
            it[when (storeBoolean) {
                StoreBoolean.USER_USES_FINGERPRINT -> userUsesFingerprintKey
                StoreBoolean.USER_LOGGED_IN -> userLoggedInKey
                else -> {
                    throw IllegalArgumentException("boolean type not specified in data store")
                }
            }] = boolean
        }
    }
}

//---------------------------------------------------------------------------------------------enums
/**
 * these enums are used by saver functions to decide where the provided value is to be stored
 */
enum class StoreString {
    USER_NAME,
    USER_EMAIL,
    USER_PHONE
}

/**
 * these enums are used by saver functions to decide where the provided value is to be stored
 */
enum class StoreBoolean {
    USER_USES_FINGERPRINT,
    USER_LOGGED_IN
}