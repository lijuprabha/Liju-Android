package com.trivand.sql

import java.io.Serializable

data class User(val email: String,val userName: String,val passWord: String , var lat: String, var lon: String,var radius:String, var wifi: String): Serializable {

}