package com.example.cryptile.data_classes

data class SafeFiles(
    val fileName: String,
    val fileCreated: String,
    val fileSize: String
){
    companion object {
        val root = "/storage/emulated/0/"
    }

}