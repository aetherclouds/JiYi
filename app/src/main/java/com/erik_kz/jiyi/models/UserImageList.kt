package com.erik_kz.jiyi.models

import com.google.firebase.firestore.PropertyName

class UserImageList (
    // firebase REQUIRES we have a default value so let's just slap a null on it
    @PropertyName("images") val  images: List<String>? = null
)
