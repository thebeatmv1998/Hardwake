package com.wecker.app.i18n

import java.util.Locale

/** Sprache der Oberfläche. [SYSTEM] folgt der Gerätesprache. */
enum class AppLanguage { SYSTEM, GERMAN, ENGLISH }

/**
 * Alle Texte der Oberfläche an einer Stelle. Jede Sprache implementiert dieses
 * Interface vollständig, damit beim Übersetzen nichts vergessen wird.
 */
interface Strings {
    val locale: Locale
    val datePattern: String
    val fullDatePattern: String
    val dayShort: List<String>
    val listSeparator: String

    // Allgemein
    val back: String
    val save: String
    val cancel: String
    val delete: String
    val close: String
    val ok: String
    val allow: String
    val off: String
    val today: String
    val tomorrow: String

    // Weckerliste
    val listTitle: String
    val tagline: String
    val deleteAlarmTitle: String
    fun deleteAlarmText(time: String, repeat: String): String
    val batteryHint: String
    val batteryAction: String
    val fullScreenHint: String
    val fullScreenAction: String
    val exactAlarmHint: String
    val emptyTitle: String
    val emptyText: String
    val newAlarmFab: String
    fun nextAlarmLabel(time: String, day: String): String
    fun countdownDays(days: Long, h: Long, m: Long, s: Long): String
    fun countdown(h: Long, m: Long, s: Long): String
    fun ringsIn(relative: String): String
    val expired: String
    val disabled: String
    val challengeNone: String
    fun challengeMath(level: String): String
    val challengeBarcode: String
    fun challengeSteps(goal: Int): String
    fun wakeCheckShort(minutes: Int): String
    val difficultyLevels: List<String>
    val settingsDescription: String

    // Einstellungen
    val settingsTitle: String
    val languageSection: String
    val languageSystem: String
    val languageGerman: String
    val languageEnglish: String
    val appearanceSection: String
    val themeLabel: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val paletteLabel: String
    val paletteCustom: String
    val paletteNames: List<String>
    val chooseColor: String
    val colorDialogTitle: String
    val hueLabel: String
    val saturationLabel: String
    val brightnessLabel: String
    val hexLabel: String
    val apply: String
    val alarmsSection: String
    val snoozeLabel: String
    fun minutesShort(n: Int): String
    val vibrationLabel: String
    val vibrationHint: String
    val aboutSection: String
    val versionLabel: String
    val privacyNote: String
    val sourceCode: String

    // Wecker bearbeiten
    val newAlarm: String
    val editAlarmTitle: String
    val repeatLabel: String
    val repeatOnce: String
    val repeatDate: String
    val repeatWeekly: String
    fun onceText(day: String): String
    val alarmDate: String
    val chooseDate: String
    val noDaySelected: String
    fun ringsOn(summary: String): String
    val labelField: String
    val ringtoneLabel: String
    val defaultRingtone: String
    val chooseRingtone: String
    val challengeToDismiss: String
    val challengeNoneShort: String
    val challengeMathShort: String
    val challengeBarcodeShort: String
    val challengeStepsShort: String
    val difficultyLabel: String
    fun problemCount(n: Int): String
    val learnBarcode: String
    val relearnBarcode: String
    val barcodeSaved: String
    val barcodeHelp: String
    fun stepsValue(n: Int): String
    val wakeCheckTitle: String
    fun wakeCheckText(minutes: Int): String
    val thirtyMinutes: String
    val dateInPast: String
    fun ringsAt(day: String, time: String, relative: String): String
    val soon: String

    // Klingelbildschirm
    val alarmWord: String
    val dismissAlarm: String
    fun snoozeButton(minutes: Int): String

    // Aufgaben
    val mathTitle: String
    fun mathProgress(current: Int, total: Int): String
    val answerLabel: String
    val wrongAnswer: String
    val check: String
    val scanCorrectBarcode: String
    val wrongBarcode: String
    val holdBarcode: String
    fun walkSteps(goal: Int): String
    val noStepSensor: String
    val activityPermission: String
    fun stepsProgress(current: Int, goal: Int): String
    val cameraPermission: String
    val learnBarcodeTitle: String
    val learnBarcodeHelp: String

    // Benachrichtigungen
    val channelAlarms: String
    val channelAlarmsDesc: String
    val channelWakeGuard: String
    val channelWakeGuardDesc: String
    fun ringTitle(time: String): String
    val ringFallbackText: String
    val solveAction: String
    val wakeGuardTitle: String
    val wakeGuardText: String
    val finalDismiss: String

    // Datum und relative Zeit
    val oneTime: String
    val everyDay: String
    val weekdaysRange: String
    fun relativeTime(totalSeconds: Long): String
}

fun resolveLanguage(language: AppLanguage): AppLanguage = when (language) {
    AppLanguage.SYSTEM ->
        if (Locale.getDefault().language == "de") AppLanguage.GERMAN else AppLanguage.ENGLISH
    else -> language
}

fun stringsFor(language: AppLanguage): Strings = when (resolveLanguage(language)) {
    AppLanguage.ENGLISH -> StringsEn
    else -> StringsDe
}

object StringsDe : Strings {
    override val locale = Locale.GERMAN
    override val datePattern = "EEE, dd.MM.yyyy"
    override val fullDatePattern = "EEEE, dd.MM.yyyy"
    override val dayShort = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    override val listSeparator = ", "

    override val back = "Zurück"
    override val save = "Speichern"
    override val cancel = "Abbrechen"
    override val delete = "Löschen"
    override val close = "Schließen"
    override val ok = "OK"
    override val allow = "Erlauben"
    override val off = "Aus"
    override val today = "heute"
    override val tomorrow = "morgen"

    override val listTitle = "Wecker"
    override val tagline = "Zuverlässig aufwachen"
    override val deleteAlarmTitle = "Wecker löschen?"
    override fun deleteAlarmText(time: String, repeat: String) =
        "Der Wecker $time ($repeat) wird endgültig entfernt."
    override val batteryHint =
        "Akku-Optimierung ist aktiv – das System legt die App sonst schlafen und der Wecker kann ausfallen."
    override val batteryAction = "Ausnahme"
    override val fullScreenHint =
        "Vollbild-Alarme sind deaktiviert – der Wecker öffnet sich dann nicht automatisch auf dem Sperrbildschirm."
    override val fullScreenAction = "Erlauben"
    override val exactAlarmHint = "Exakte Alarme sind deaktiviert – der Wecker könnte verspätet klingeln."
    override val emptyTitle = "Noch keine Wecker"
    override val emptyText = "Tippe auf „Wecker“, um deinen ersten zu erstellen."
    override val newAlarmFab = "Wecker"
    override fun nextAlarmLabel(time: String, day: String) = "Nächster Wecker · $time · $day"
    override fun countdownDays(days: Long, h: Long, m: Long, s: Long) =
        "%d T  %02d:%02d:%02d".format(days, h, m, s)
    override fun countdown(h: Long, m: Long, s: Long) = "%02d:%02d:%02d".format(h, m, s)
    override fun ringsIn(relative: String) = "Klingelt $relative"
    override val expired = "Abgelaufen"
    override val disabled = "Deaktiviert"
    override val challengeNone = "Keine Aufgabe"
    override fun challengeMath(level: String) = "Rechenaufgabe ($level)"
    override val challengeBarcode = "Barcode"
    override fun challengeSteps(goal: Int) = "$goal Schritte"
    override fun wakeCheckShort(minutes: Int) = "Aufwachkontrolle $minutes Min"
    override val difficultyLevels = listOf("Leicht", "Mittel", "Schwer", "Sehr schwer")
    override val settingsDescription = "Einstellungen"

    override val settingsTitle = "Einstellungen"
    override val languageSection = "Sprache"
    override val languageSystem = "System"
    override val languageGerman = "Deutsch"
    override val languageEnglish = "English"
    override val appearanceSection = "Darstellung"
    override val themeLabel = "Design"
    override val themeSystem = "System"
    override val themeLight = "Hell"
    override val themeDark = "Dunkel"
    override val paletteLabel = "Farbschema"
    override val paletteCustom = "Eigene Farbe"
    override val paletteNames = listOf("Petrol", "Ozean", "Wald", "Sonnenuntergang", "Violett")
    override val chooseColor = "Farbe wählen"
    override val colorDialogTitle = "Farbe wählen"
    override val hueLabel = "Farbton"
    override val saturationLabel = "Sättigung"
    override val brightnessLabel = "Helligkeit"
    override val hexLabel = "Hex"
    override val apply = "Übernehmen"
    override val alarmsSection = "Wecker"
    override val snoozeLabel = "Schlummerdauer"
    override fun minutesShort(n: Int) = "$n Min"
    override val vibrationLabel = "Vibration"
    override val vibrationHint = "Vibriert, während der Wecker klingelt."
    override val aboutSection = "Über"
    override val versionLabel = "Version"
    override val privacyNote = "Alle Daten bleiben auf diesem Gerät. Die App hat keinen Internetzugriff."
    override val sourceCode = "Quellcode auf GitHub"

    override val newAlarm = "Neuer Wecker"
    override val editAlarmTitle = "Wecker bearbeiten"
    override val repeatLabel = "Wiederholung"
    override val repeatOnce = "Einmalig"
    override val repeatDate = "Datum"
    override val repeatWeekly = "Wochentage"
    override fun onceText(day: String) =
        "Klingelt nur einmal: $day – danach wird der Wecker automatisch deaktiviert."
    override val alarmDate = "Weckdatum"
    override val chooseDate = "Datum wählen"
    override val noDaySelected = "Kein Tag gewählt → klingelt einmalig"
    override fun ringsOn(summary: String) = "Klingelt: $summary"
    override val labelField = "Bezeichnung (optional)"
    override val ringtoneLabel = "Klingelton"
    override val defaultRingtone = "Standard-Alarmton"
    override val chooseRingtone = "Klingelton wählen"
    override val challengeToDismiss = "Aufgabe zum Beenden"
    override val challengeNoneShort = "Keine"
    override val challengeMathShort = "Mathe"
    override val challengeBarcodeShort = "Barcode"
    override val challengeStepsShort = "Schritte"
    override val difficultyLabel = "Schwierigkeit:"
    override fun problemCount(n: Int) = "Anzahl Aufgaben: $n"
    override val learnBarcode = "Barcode einlernen"
    override val relearnBarcode = "Barcode neu einlernen"
    override val barcodeSaved = "Barcode gespeichert ✓"
    override val barcodeHelp =
        "Scanne den Barcode, den du morgens suchen musst (z. B. auf der Duschgel-Flasche)."
    override fun stepsValue(n: Int) = "Schritte: $n"
    override val wakeCheckTitle = "Aufwachkontrolle"
    override fun wakeCheckText(minutes: Int) =
        "Klingelt $minutes Min nach dem Beenden erneut, bis du in der Benachrichtigung endgültig deaktivierst"
    override val thirtyMinutes = "30 Min"
    override val dateInPast = "Dieses Datum liegt in der Vergangenheit"
    override fun ringsAt(day: String, time: String, relative: String) =
        "Klingelt $day um $time · $relative"
    override val soon = "bald"

    override val alarmWord = "Wecker"
    override val dismissAlarm = "Wecker beenden"
    override fun snoozeButton(minutes: Int) = "In $minutes Min erneut erinnern"

    override val mathTitle = "Rechenaufgabe"
    override fun mathProgress(current: Int, total: Int) = "Aufgabe $current von $total"
    override val answerLabel = "Antwort"
    override val wrongAnswer = "Falsch – versuch es nochmal"
    override val check = "Prüfen"
    override val scanCorrectBarcode = "Scanne den richtigen Barcode"
    override val wrongBarcode = "Falscher Barcode – versuch es nochmal"
    override val holdBarcode = "Halte den Barcode vor die Kamera"
    override fun walkSteps(goal: Int) = "Geh $goal Schritte"
    override val noStepSensor = "Dieses Gerät hat keinen Schrittzähler-Sensor."
    override val activityPermission = "Berechtigung für körperliche Aktivität erforderlich"
    override fun stepsProgress(current: Int, goal: Int) = "$current / $goal Schritte"
    override val cameraPermission = "Kamera-Berechtigung erforderlich"
    override val learnBarcodeTitle = "Barcode einlernen"
    override val learnBarcodeHelp =
        "Scanne den Barcode, den du zum Beenden des Weckers brauchst – z. B. auf einem Produkt im Bad oder in der Küche."

    override val channelAlarms = "Wecker"
    override val channelAlarmsDesc = "Klingelnde Wecker"
    override val channelWakeGuard = "Aufwachkontrolle"
    override val channelWakeGuardDesc = "Erinnerung zum endgültigen Deaktivieren"
    override fun ringTitle(time: String) = "Wecker $time"
    override val ringFallbackText = "Aufstehen!"
    override val solveAction = "Aufgabe lösen"
    override val wakeGuardTitle = "Aufwachkontrolle"
    override val wakeGuardText = "Der Wecker klingelt erneut – oder jetzt endgültig deaktivieren."
    override val finalDismiss = "Endgültig deaktivieren"

    override val oneTime = "Einmalig"
    override val everyDay = "Täglich"
    override val weekdaysRange = "Mo–Fr"

    override fun relativeTime(totalSeconds: Long): String {
        val days = totalSeconds / 86_400
        return when {
            days >= 365 -> {
                val years = days / 365
                val rest = days % 365
                val yearsText = if (years == 1L) "1 Jahr" else "$years Jahren"
                if (rest == 0L) "in $yearsText"
                else "in $yearsText und " + if (rest == 1L) "1 Tag" else "$rest Tagen"
            }
            days > 0 -> "in " + if (days == 1L) "1 Tag" else "$days Tagen"
            else -> {
                val hours = totalSeconds / 3_600
                if (hours > 0) "in " + if (hours == 1L) "1 Stunde" else "$hours Stunden"
                else {
                    val minutes = totalSeconds / 60
                    "in " + if (minutes == 1L) "1 Minute" else "$minutes Minuten"
                }
            }
        }
    }
}

object StringsEn : Strings {
    override val locale = Locale.ENGLISH
    override val datePattern = "EEE, MMM d, yyyy"
    override val fullDatePattern = "EEEE, MMMM d, yyyy"
    override val dayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    override val listSeparator = ", "

    override val back = "Back"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val close = "Close"
    override val ok = "OK"
    override val allow = "Allow"
    override val off = "Off"
    override val today = "today"
    override val tomorrow = "tomorrow"

    override val listTitle = "Alarms"
    override val tagline = "Wake up on time"
    override val deleteAlarmTitle = "Delete alarm?"
    override fun deleteAlarmText(time: String, repeat: String) =
        "The alarm $time ($repeat) will be removed permanently."
    override val batteryHint =
        "Battery optimization is on. The system may put the app to sleep and the alarm could fail."
    override val batteryAction = "Exempt"
    override val fullScreenHint =
        "Full-screen alarms are off, so the alarm will not open on the lock screen by itself."
    override val fullScreenAction = "Allow"
    override val exactAlarmHint = "Exact alarms are off. The alarm may ring late."
    override val emptyTitle = "No alarms yet"
    override val emptyText = "Tap “Alarm” to create your first one."
    override val newAlarmFab = "Alarm"
    override fun nextAlarmLabel(time: String, day: String) = "Next alarm · $time · $day"
    override fun countdownDays(days: Long, h: Long, m: Long, s: Long) =
        "%dd  %02d:%02d:%02d".format(days, h, m, s)
    override fun countdown(h: Long, m: Long, s: Long) = "%02d:%02d:%02d".format(h, m, s)
    override fun ringsIn(relative: String) = "Rings $relative"
    override val expired = "Expired"
    override val disabled = "Disabled"
    override val challengeNone = "No challenge"
    override fun challengeMath(level: String) = "Math ($level)"
    override val challengeBarcode = "Barcode"
    override fun challengeSteps(goal: Int) = "$goal steps"
    override fun wakeCheckShort(minutes: Int) = "Wake check $minutes min"
    override val difficultyLevels = listOf("Easy", "Medium", "Hard", "Very hard")
    override val settingsDescription = "Settings"

    override val settingsTitle = "Settings"
    override val languageSection = "Language"
    override val languageSystem = "System"
    override val languageGerman = "Deutsch"
    override val languageEnglish = "English"
    override val appearanceSection = "Appearance"
    override val themeLabel = "Theme"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val paletteLabel = "Color scheme"
    override val paletteCustom = "Custom color"
    override val paletteNames = listOf("Teal", "Ocean", "Forest", "Sunset", "Violet")
    override val chooseColor = "Choose color"
    override val colorDialogTitle = "Choose a color"
    override val hueLabel = "Hue"
    override val saturationLabel = "Saturation"
    override val brightnessLabel = "Brightness"
    override val hexLabel = "Hex"
    override val apply = "Apply"
    override val alarmsSection = "Alarms"
    override val snoozeLabel = "Snooze duration"
    override fun minutesShort(n: Int) = "$n min"
    override val vibrationLabel = "Vibration"
    override val vibrationHint = "Vibrates while the alarm rings."
    override val aboutSection = "About"
    override val versionLabel = "Version"
    override val privacyNote = "All data stays on this device. The app has no internet access."
    override val sourceCode = "Source code on GitHub"

    override val newAlarm = "New alarm"
    override val editAlarmTitle = "Edit alarm"
    override val repeatLabel = "Repeat"
    override val repeatOnce = "Once"
    override val repeatDate = "Date"
    override val repeatWeekly = "Weekdays"
    override fun onceText(day: String) =
        "Rings once: $day. The alarm turns itself off afterwards."
    override val alarmDate = "Alarm date"
    override val chooseDate = "Choose date"
    override val noDaySelected = "No day selected → rings once"
    override fun ringsOn(summary: String) = "Rings: $summary"
    override val labelField = "Label (optional)"
    override val ringtoneLabel = "Ringtone"
    override val defaultRingtone = "Default alarm sound"
    override val chooseRingtone = "Choose ringtone"
    override val challengeToDismiss = "Challenge to dismiss"
    override val challengeNoneShort = "None"
    override val challengeMathShort = "Math"
    override val challengeBarcodeShort = "Barcode"
    override val challengeStepsShort = "Steps"
    override val difficultyLabel = "Difficulty:"
    override fun problemCount(n: Int) = "Problems: $n"
    override val learnBarcode = "Learn barcode"
    override val relearnBarcode = "Learn a new barcode"
    override val barcodeSaved = "Barcode saved ✓"
    override val barcodeHelp =
        "Scan the barcode you will have to find in the morning (for example on a shampoo bottle)."
    override fun stepsValue(n: Int) = "Steps: $n"
    override val wakeCheckTitle = "Wake check"
    override fun wakeCheckText(minutes: Int) =
        "Rings again $minutes min after you dismiss, until you turn it off from the notification"
    override val thirtyMinutes = "30 min"
    override val dateInPast = "This date is in the past"
    override fun ringsAt(day: String, time: String, relative: String) =
        "Rings $day at $time · $relative"
    override val soon = "soon"

    override val alarmWord = "Alarm"
    override val dismissAlarm = "Stop alarm"
    override fun snoozeButton(minutes: Int) = "Remind me in $minutes min"

    override val mathTitle = "Math challenge"
    override fun mathProgress(current: Int, total: Int) = "Problem $current of $total"
    override val answerLabel = "Answer"
    override val wrongAnswer = "Wrong — try again"
    override val check = "Check"
    override val scanCorrectBarcode = "Scan the correct barcode"
    override val wrongBarcode = "Wrong barcode — try again"
    override val holdBarcode = "Hold the barcode in front of the camera"
    override fun walkSteps(goal: Int) = "Walk $goal steps"
    override val noStepSensor = "This device has no step counter."
    override val activityPermission = "Permission for physical activity required"
    override fun stepsProgress(current: Int, goal: Int) = "$current / $goal steps"
    override val cameraPermission = "Camera permission required"
    override val learnBarcodeTitle = "Learn barcode"
    override val learnBarcodeHelp =
        "Scan the barcode you need to stop the alarm, for example on a product in the bathroom or kitchen."

    override val channelAlarms = "Alarms"
    override val channelAlarmsDesc = "Ringing alarms"
    override val channelWakeGuard = "Wake check"
    override val channelWakeGuardDesc = "Reminder to turn the alarm off for good"
    override fun ringTitle(time: String) = "Alarm $time"
    override val ringFallbackText = "Get up!"
    override val solveAction = "Solve challenge"
    override val wakeGuardTitle = "Wake check"
    override val wakeGuardText = "The alarm rings again, or turn it off for good now."
    override val finalDismiss = "Turn off for good"

    override val oneTime = "Once"
    override val everyDay = "Every day"
    override val weekdaysRange = "Mon–Fri"

    override fun relativeTime(totalSeconds: Long): String {
        val days = totalSeconds / 86_400
        return when {
            days >= 365 -> {
                val years = days / 365
                val rest = days % 365
                val yearsText = if (years == 1L) "1 year" else "$years years"
                if (rest == 0L) "in $yearsText"
                else "in $yearsText and " + if (rest == 1L) "1 day" else "$rest days"
            }
            days > 0 -> "in " + if (days == 1L) "1 day" else "$days days"
            else -> {
                val hours = totalSeconds / 3_600
                if (hours > 0) "in " + if (hours == 1L) "1 hour" else "$hours hours"
                else {
                    val minutes = totalSeconds / 60
                    "in " + if (minutes == 1L) "1 minute" else "$minutes minutes"
                }
            }
        }
    }
}
