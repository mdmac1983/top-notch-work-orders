package com.topnotchlock.workorder.data

import org.json.JSONObject

/** One business this app can generate work orders for (name + header phone number). */
data class Company(
    val id: String,
    val name: String,
    val phone: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("phone", phone)
    }

    companion object {
        fun fromJson(json: JSONObject): Company = Company(
            id = json.optString("id"),
            name = json.optString("name"),
            phone = json.optString("phone")
        )
    }
}
