package com.iliass.iliass.util

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.Phone
import java.io.File
import java.io.FileOutputStream

class PhoneStorageManager(private val context: Context) {

    private val gson = Gson()
    private val prefsName = "phone_inventory"
    private val phoneListKey = "phones"

    private val imagesDir: File
        get() = File(context.filesDir, "phone_images").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Check if an IMEI already exists in the inventory
     * @param imei The IMEI to check
     * @param excludePhoneId Optional phone ID to exclude from the check (for updates)
     * @return true if IMEI exists, false otherwise
     */
    fun isImeiExists(imei: String, excludePhoneId: String? = null): Boolean {
        val phones = getAllPhones()
        return phones.any { phone ->
            phone.imei.equals(imei, ignoreCase = true) && phone.id != excludePhoneId
        }
    }

    fun savePhone(phone: Phone): Boolean {
        return try {
            // Check for duplicate IMEI before saving
            if (isImeiExists(phone.imei, phone.id)) {
                return false // IMEI already exists
            }

            val phones = getAllPhones().toMutableList()
            phones.add(phone)
            savePhoneList(phones)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun updatePhone(phone: Phone): Boolean {
        return try {
            // Check for duplicate IMEI (excluding the current phone being updated)
            if (isImeiExists(phone.imei, phone.id)) {
                return false // IMEI already exists on another phone
            }

            val phones = getAllPhones().toMutableList()
            val index = phones.indexOfFirst { it.id == phone.id }
            if (index != -1) {
                phones[index] = phone
                savePhoneList(phones)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deletePhone(phoneId: String): Boolean {
        return try {
            val phones = getAllPhones().toMutableList()
            val phone = phones.find { it.id == phoneId }

            // Delete associated image if exists
            phone?.imagePath?.let { path ->
                File(path).delete()
            }

            phones.removeAll { it.id == phoneId }
            savePhoneList(phones)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAllPhones(): List<Phone> {
        return try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val json = prefs.getString(phoneListKey, "[]") ?: "[]"
            val type = object : TypeToken<List<Phone>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun searchPhones(query: String): List<Phone> {
        val allPhones = getAllPhones()
        if (query.isBlank()) return allPhones

        return allPhones.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.imei.contains(query, ignoreCase = true) ||
                    it.shopName.contains(query, ignoreCase = true) ||
                    it.shopAddress.contains(query, ignoreCase = true)
        }
    }

    fun saveImage(bitmap: Bitmap, phoneId: String): String? {
        return try {
            val imageFile = File(imagesDir, "phone_$phoneId.jpg")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun savePhoneList(phones: List<Phone>) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val json = gson.toJson(phones)
        prefs.edit().putString(phoneListKey, json).apply()
    }
}