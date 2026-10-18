package manoellribeiro.dev.martp.core.models.failures

import manoellribeiro.dev.martp.R

class MapboxApiErrorFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.error_downloading_location_image
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)