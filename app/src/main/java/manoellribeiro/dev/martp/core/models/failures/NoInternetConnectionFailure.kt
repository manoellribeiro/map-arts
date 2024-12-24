package manoellribeiro.dev.martp.core.models.failures

import manoellribeiro.dev.martp.R

class NoInternetConnectionFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.no_internet_connection_failure_message
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)