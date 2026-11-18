package manoellribeiro.dev.martp.printerhardware.models.failures

import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.printerhardware.R

class SmartPosPrintingFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.smart_pos_printing_failure
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)
