package com.example.pawprints

data class User(
    var correo: String = "",
    var password: String = "",
    var nombre: String = "",
    var raza: String = "",
    var owner: String = "",
    var foto: String? = null
)
