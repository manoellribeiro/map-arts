package manoellribeiro.dev.martp.printerhardware.models.failures

import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.printerhardware.R

class SmartPosPrinterNotConnectedFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.smart_pos_printer_not_connected_failure
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)
