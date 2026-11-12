package com.nangmanroad.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun Context.openWebLink(url: String) {
    val message = try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        return
    } catch (_: ActivityNotFoundException) {
        "이 기기에서 링크를 열 수 있는 브라우저가 없어요."
    } catch (_: SecurityException) {
        "이 화면에서는 외부 링크를 열 수 없어요."
    }
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
