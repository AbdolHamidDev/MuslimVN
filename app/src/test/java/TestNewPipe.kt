package com.example.muslimvn

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.ContentCountry

class TestNewPipe {
    fun test() {
        NewPipe.init(null, Localization.fromLocale(java.util.Locale("vi", "VN")), ContentCountry("VN"))
    }
}
