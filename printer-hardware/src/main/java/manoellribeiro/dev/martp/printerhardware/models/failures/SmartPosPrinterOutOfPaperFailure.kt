package manoellribeiro.dev.martp.printerhardware.models.failures

import manoellribeiro.dev.martp.core.models.failures.Failure
import manoellribeiro.dev.martp.printerhardware.R

class SmartPosPrinterOutOfPaperFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.smart_pos_printer_out_of_paper_failure
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)
