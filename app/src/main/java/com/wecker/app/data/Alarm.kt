package com.wecker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Art der Aufgabe, die zum Beenden des Weckers gelöst werden muss. */
enum class ChallengeType { NONE, MATH, BARCODE, STEPS }

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Bitmask der Wochentage: Bit 0 = Montag … Bit 6 = Sonntag. 0 = einmalig. */
    val daysMask: Int = 0,
    /**
     * Epoch-Day eines konkreten Kalenderdatums (z. B. 09.10.2028). Ist er gesetzt,
     * klingelt der Wecker genau einmal an diesem Tag und [daysMask] wird ignoriert.
     */
    val dateEpochDay: Long? = null,
    val label: String = "",
    val enabled: Boolean = true,
    val challengeType: ChallengeType = ChallengeType.NONE,
    /** Anzahl der Rechenaufgaben (MATH). */
    val mathCount: Int = 3,
    /** Schwierigkeit der Rechenaufgaben: 0 = Leicht … 3 = Sehr schwer (MATH). */
    val mathDifficulty: Int = 1,
    /** Eingelernter Barcode-Wert (BARCODE). */
    val barcodeValue: String? = null,
    /** Zu laufende Schritte (STEPS). */
    val stepGoal: Int = 30,
    /** Aufwachkontrolle: Minuten bis zum erneuten Klingeln. 0 = aus. */
    val wakeCheckMinutes: Int = 0,
    /** Eigener Klingelton (URI als String); null = Standard-Alarmton. */
    val ringtoneUri: String? = null,
    /**
     * Epoch-Day eines Vorkommens, das einmalig ausfallen soll („Nächsten
     * überspringen"). Nur für wiederkehrende Wecker gedacht.
     */
    val skipEpochDay: Long? = null
)
