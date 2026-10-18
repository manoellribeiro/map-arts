package manoellribeiro.dev.martp.core.logging

/**
 * Records exceptions that are handled by the app (e.g. transformed into a Failure or replaced
 * by a default value), so they are not lost even though the user never sees a crash.
 * Never put user data (emails, usernames, coordinates) in the operation name.
 */
interface ErrorLogger {
    fun logNonFatal(throwable: Throwable, operation: String)
}
