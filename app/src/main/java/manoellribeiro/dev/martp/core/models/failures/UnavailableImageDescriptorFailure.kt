package manoellribeiro.dev.martp.core.models.failures

import manoellribeiro.dev.martp.R

class UnavailableImageDescriptorFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.error_executing_operation_in_the_local_database //TODO: create the right message
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)