package com.example.mycloset.util

import com.example.mycloset.data.model.Item
import com.example.mycloset.data.model.Closet
import com.example.mycloset.data.model.Visibility

fun Item.updateVisibility(newVisibility: Visibility): Item {
    return this.copy(visibility = newVisibility.name)
}

fun Closet.updateVisibility(newVisibility: Visibility): Closet {
    return this.copy(visibility = newVisibility.name)
}