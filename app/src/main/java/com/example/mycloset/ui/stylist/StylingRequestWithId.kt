package com.example.mycloset.ui.stylist

data class StylingRequestWithId(
    val docId: String = "",
    val req: StylingRequest = StylingRequest()
)
