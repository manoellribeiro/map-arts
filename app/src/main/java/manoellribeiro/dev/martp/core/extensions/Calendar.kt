package manoellribeiro.dev.martp.core.extensions

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Calendar.formatDateHour(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(this.time)