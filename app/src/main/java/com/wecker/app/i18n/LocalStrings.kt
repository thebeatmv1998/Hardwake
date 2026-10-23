package com.wecker.app.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/** Aktuelle UI-Texte. Wird in MainActivity und RingActivity bereitgestellt. */
val LocalStrings = staticCompositionLocalOf<Strings> { stringsFor(AppLanguage.SYSTEM) }
