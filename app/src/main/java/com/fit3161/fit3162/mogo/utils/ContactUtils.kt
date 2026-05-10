package com.fit3161.fit3162.mogo.utils

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

fun readContactFromUri(contentResolver: ContentResolver, uri: Uri): Pair<String?, String?> {
    var name: String? = null
    var phone: String? = null

    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            name = it.getString(nameIndex)
            val contactId = it.getString(idIndex)

            val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )
            phoneCursor?.use { pc ->
                if (pc.moveToFirst()) {
                    val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    phone = pc.getString(phoneIndex)
                }
            }
        }
    }
    return Pair(name, phone)
}