package com.example.mycloset.data.model

//-------------Item Category-------------------------------------

enum class Season{
    SPRING, SUMMER, FALL, WINTER, ALL_YEAR;
    override fun toString(): String {
        return name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}
enum class Color{
    BLACK, WHITE;
    override fun toString(): String {
        return name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

//Core Categories
enum class Category(val label: String, val subCategories: List<String>) {
    TOPS("Tops", listOf("T-Shirt", "Jacket", "Sweater", "Blouse")),
    BOTTOMS("Bottoms", listOf("Jeans", "Shorts", "Skirt", "Trousers")),
    SHOES("Shoes", listOf("Sneakers", "Boots", "Sandals", "Heels")),
    ACCESSORIES("Accessories", listOf("Hat", "Belt", "Scarf", "Sunglasses"));

    override fun toString(): String = label
}
