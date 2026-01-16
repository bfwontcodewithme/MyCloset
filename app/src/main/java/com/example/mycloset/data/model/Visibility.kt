package com.example.mycloset.data.model

enum class Visibility {
    PUBLIC,
    FRIENDS_ONLY,
    PRIVATE
}

// Firebase --> App
fun String.toVisibility(): Visibility {
    return try {
        Visibility.valueOf(this.uppercase())
    } catch (e: Exception) {
        Visibility.PRIVATE
    }
}

// App --> Firebase
fun Item.updateVisibility(newVisibility: Visibility): Item {
    return this.copy(visibility = newVisibility.name)
}
