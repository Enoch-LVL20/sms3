package com.enochlvl20.sms3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enochlvl20.sms3.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val SMS_CONSENT_REQUEST = 1462

    private val smsVerificationReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent?) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action){
                val extras = intent.extras
                val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

                when(smsRetrieverStatus.statusCode){
                    CommonStatusCodes.SUCCESS -> {
                        //getConsent intent
                        val consentIntent =
                            extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        try {
                            if (consentIntent != null) {
                                startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
                            }
                        }catch (e: Exception){
                            Log.e(TAG, "Intent error")
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        //handle timeout error
                    }
                    CommonStatusCodes.CANCELED ->{
                        //handle cancel tap
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        doSmsStuff()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun doSmsStuff(){
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        registerReceiver(
            smsVerificationReceiver,
            intentFilter,
            SmsRetriever.SEND_PERMISSION,
            null
        )
        initAutoFill()
    }

    private fun initAutoFill() {
        SmsRetriever.getClient(this)
            .startSmsUserConsent(null)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful){
                    Log.e(TAG, "Listening")
                }else{
                    Log.e(TAG, "failed")
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            SMS_CONSENT_REQUEST ->
                if (resultCode == Activity.RESULT_OK && data != null){
                    val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                    val otpCode = message?.filter { it.isDigit() } ?: ""
                    binding.tipOtp.setText(otpCode)
                    binding.tipOtp.setSelection(otpCode.length)
                }else{
                    Log.e(TAG, "Permission denied")
                }
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(smsVerificationReceiver)
        Log.e(TAG, "unregister")
    }

    companion object{
        private const val TAG = "MainActivity"
    }
}