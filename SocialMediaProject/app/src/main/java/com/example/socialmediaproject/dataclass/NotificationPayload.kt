package com.example.socialmediaproject.dataclass

import com.google.gson.annotations.SerializedName

data class NotificationPayload(@SerializedName("app_id")
                               val appId: String,
                               @SerializedName("include_external_user_ids")
                               val includedExternalUserIds: List<String>,
                               val contents: NotificationContent,
                               val data: Map<String, String>? = null)
