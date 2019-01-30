package com.trivand.sql

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.parseOpt
import org.jetbrains.anko.db.select
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

class LoginActivity : AppCompatActivity() {

    val Context.database: MyDatabaseOpenHelper
        get() = MyDatabaseOpenHelper.getInstance(applicationContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        sign_in_button.setOnClickListener {
            attemptLogin()

        }
        get.setOnClickListener { startActivity(Intent(this, UserRegistrationActivity::class.java)) }

    }


    private fun attemptLogin() {
        email.error = null
        password.error = null

        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            logIn(emailStr, passwordStr)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }


    private fun logIn(userEmail: String, passWord: String) {
        var user: User? = null
        doAsync {
            database.use {
                select("User", "email", "name", "passWord", "lat", "lon", "radius", "wifi").whereArgs("email = {userEmail} and passWord={userPassword}", "userEmail" to userEmail, "userPassword" to passWord).exec {
                    user = parseOpt(classParser())
                }
            }

            uiThread {
                if (user != null) {
                    var intent = Intent(this@LoginActivity, MapsActivity::class.java)
                    intent.putExtra("user", user!!)
                    startActivity(intent)
                    finish()
                } else {
                    toast(resources.getString(R.string.login_error_msg))
                }
            }
        }

    }

}

