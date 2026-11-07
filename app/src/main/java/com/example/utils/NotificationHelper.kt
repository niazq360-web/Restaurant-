package com.example.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

object NotificationHelper {

    /**
     * Sends an automatic SMS to a phone number.
     * If SEND_SMS permission is granted, dispatches directly via SmsManager.
     * Otherwise opens the system SMS app with pre-filled content.
     */
    fun sendAutoSms(context: Context, phone: String, message: String): Boolean {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) return false

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasPermission) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanPhone, null, message, null, null)
                }
                Toast.makeText(context, "Auto-SMS sent to $cleanPhone", Toast.LENGTH_SHORT).show()
                true
            } catch (e: Exception) {
                fallbackSmsIntent(context, cleanPhone, message)
                false
            }
        } else {
            fallbackSmsIntent(context, cleanPhone, message)
            false
        }
    }

    private fun fallbackSmsIntent(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(phone)}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open SMS app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens WhatsApp to send a message to a specific number.
     * Formats Pakistani local numbers (03xx) to 923xx for WhatsApp API.
     */
    fun sendWhatsAppMessage(context: Context, rawPhone: String, message: String) {
        val cleanDigits = rawPhone.replace(Regex("[^0-9+]"), "")
        val formattedNumber = when {
            cleanDigits.startsWith("+") -> cleanDigits.removePrefix("+")
            cleanDigits.startsWith("00") -> cleanDigits.removePrefix("00")
            cleanDigits.startsWith("0") && cleanDigits.length >= 10 -> "92" + cleanDigits.substring(1)
            else -> cleanDigits
        }

        try {
            val url = if (formattedNumber.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // If official WhatsApp isn't matching, open standard chooser or browser
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}"))
                genericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(genericIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens Google Maps turn-by-turn navigation or search for a customer delivery address.
     */
    fun openGoogleMapsNavigation(context: Context, address: String) {
        val cleanAddress = address.trim()
        if (cleanAddress.isBlank()) {
            Toast.makeText(context, "Delivery address is empty.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Try Google Navigation first
            val navUri = Uri.parse("google.navigation:q=${Uri.encode(cleanAddress)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to geo URI or browser
                val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(cleanAddress)}")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            // Web fallback
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(cleanAddress)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Direct Phone Call via phone dialer
     */
    fun dialPhoneNumber(context: Context, phone: String) {
        if (phone.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(phone.trim())}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
