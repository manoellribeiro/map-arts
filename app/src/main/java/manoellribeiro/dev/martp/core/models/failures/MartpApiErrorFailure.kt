package manoellribeiro.dev.martp.core.models.failures

import manoellribeiro.dev.martp.R

class MartpApiErrorFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.martp_api_failure_message
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)