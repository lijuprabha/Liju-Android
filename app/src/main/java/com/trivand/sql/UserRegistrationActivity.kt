package com.trivand.sql

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_register.*
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.parseOpt
import org.jetbrains.anko.db.select
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

class UserRegistrationActivity : AppCompatActivity() {
    val Context.database: MyDatabaseOpenHelper
        get() = MyDatabaseOpenHelper.getInstance(applicationContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        reg_button.setOnClickListener {
            if (validate()) {
                var s = emailCheck(reg_email.text.toString())
                if (s.isNullOrEmpty()) {
                    val values = ContentValues()
                    values.put("email", reg_email.text.toString())
                    values.put("name", name.text.toString())
                    values.put("passWord", reg_password.text.toString())
                    values.put("lat", "00")
                    values.put("lon", "00")
                    values.put("radius","60")
                    values.put("wifi","")
                    insertUser(values)
                } else
                    Toast.makeText(this, getString(R.string.error_already_exists), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    fun validate(): Boolean {
        if (name.text.isEmpty()) {
            name.error = getString(R.string.error_field_required)
            return false
        }
        if (reg_email.text.isEmpty()) {
            reg_email.error = getString(R.string.error_field_required)
            return false
        }
        if (reg_password.text.isEmpty()) {
            reg_password.error = getString(R.string.error_field_required)
            return false
        }

        return true

    }

    private fun insertUser(user: ContentValues) {
        doAsync {
            val result = database.use {
                insert("User", null, user)
            }

            uiThread {
                toast(resources.getString(R.string.rgistration_suceesfully))
                finish()

            }
        }

    }

    fun emailCheck(userEmail: String): String? {
        var s: String? = null
        database.use {
            select("User", "email").whereSimple("email = ?", userEmail).exec {
                s = parseOpt(classParser())
            }
        }

        return s
    }
}