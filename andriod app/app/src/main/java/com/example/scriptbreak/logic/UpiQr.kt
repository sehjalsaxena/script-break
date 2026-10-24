package com.example.scriptbreak.logic

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.net.URLDecoder

/**
 * A UPI payment QR code: upi://pay?pa=<UPI ID>&pn=<name>&am=<amount>&tn=<note>.
 * A QR code is only another way to enter a payment: the payee, the AI check and the PIN are the same
 * as for a typed UPI ID. The name written in a QR is NOT verified, so the app never uses it as the payee's name.
 */
data class UpiQr(val upiId: String, val name: String?, val amount: Double?, val note: String?) {
    companion object {
        /** Reads the text inside a QR code. Null if it is not a UPI payment code with a payee UPI ID. */
        fun parse(text: String?): UpiQr? {
            val t = text?.trim() ?: return null
            if (!t.lowercase().startsWith("upi://pay")) return null
            val params = LinkedHashMap<String, String>()
            for (kv in t.substringAfter('?', "").split('&')) {
                if (!kv.contains('=')) continue
                val key = kv.substringBefore('=').trim().lowercase()
                if (key.isNotEmpty()) params.putIfAbsent(key, decode(kv.substringAfter('=')).trim())
            }
            val pa = params["pa"]?.lowercase()?.takeIf { it.length >= 3 && it.count { c -> c == '@' } == 1 } ?: return null
            val am = params["am"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
            return UpiQr(pa, params["pn"]?.takeIf { it.isNotEmpty() }?.take(40), am, params["tn"]?.takeIf { it.isNotEmpty() }?.take(40))
        }

        private fun decode(s: String): String = try { URLDecoder.decode(s, "UTF-8") } catch (e: Exception) { s }
    }
}

/** Finds and reads a QR code in an image, on the phone. Plain ZXing decoding: no AI model, nothing is uploaded. */
object QrDecoder {
    private val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
    )

    /** The text of the QR code in these ARGB pixels (a picked image), or null if there is none. */
    fun decode(pixels: IntArray, width: Int, height: Int): String? = decode(RGBLuminanceSource(width, height, pixels))

    /** The same for one camera frame: its brightness (Y) plane, one byte per pixel. */
    fun decodeLuminance(data: ByteArray, width: Int, height: Int): String? =
        decode(PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false))

    private fun decode(source: LuminanceSource): String? {
        // two ways of turning the picture into black and white, then once more for light-on-dark codes
        val attempts = listOf(
            BinaryBitmap(HybridBinarizer(source)),
            BinaryBitmap(GlobalHistogramBinarizer(source)),
            BinaryBitmap(HybridBinarizer(source.invert())),
        )
        for (bitmap in attempts) {
            try { return MultiFormatReader().decode(bitmap, hints).text } catch (e: Exception) { }
        }
        return null
    }

    /** Reads only the one image the person picked (Android photo picker). Null if it can't be read or has no QR. */
    fun decode(context: Context, uri: Uri): String? = try {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
        else {
            var sample = 1   // shrink very large photos so they fit in memory; a QR stays readable
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2000) sample *= 2
            val bmp = cr.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
            if (bmp == null) null
            else {
                val w = bmp.width; val h = bmp.height
                val pixels = IntArray(w * h)
                bmp.getPixels(pixels, 0, w, 0, 0, w, h)
                bmp.recycle()
                decode(pixels, w, h)
            }
        }
    } catch (e: Exception) {
        null
    }
}
