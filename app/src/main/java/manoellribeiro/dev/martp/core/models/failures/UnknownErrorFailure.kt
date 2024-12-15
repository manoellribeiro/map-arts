package manoellribeiro.dev.martp.core.models.failures



import manoellribeiro.dev.martp.R

class UnknownErrorFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.unknown_error_failure
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)