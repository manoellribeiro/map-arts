package manoellribeiro.dev.martp.core.models.failures


import manoellribeiro.dev.martp.R

class LocationDisabledFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.location_permission_not_granted_failure_error
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)