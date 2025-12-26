package com.iliass.iliass.model

import java.io.Serializable

data class Phone(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val imei: String,
    val shopName: String,
    val shopAddress: String,
    val dateRegistered: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val notes: String? = null
) : Serializable