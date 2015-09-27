package com.alexfacciorusso.kotlinbot.model

data class Bug(val summary: String,
               val description: String,
               val link: String,
               val reporterFullname: String,
               val reporterNickname: String)