package manoellribeiro.dev.martp.core.models.failures

/**
 * Failure is a exception with meaningful name and user friendly message.
 * It should be used to prevent showing bad errors messages to users, so Interactors and ViewModels
 * should only show errors messages on Views from this kind of exception.
 */
abstract class Failure(
    originalExceptionMessage: String?,
    val messageToBeDisplayedToUserId: Int
): Exception(originalExceptionMessage)