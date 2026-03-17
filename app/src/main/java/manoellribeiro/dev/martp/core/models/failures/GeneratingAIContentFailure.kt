package manoellribeiro.dev.martp.core.models.failures


import manoellribeiro.dev.martp.R

class GeneratingAIContentFailure(
    originalExceptionMessage: String?,
    messageToBeDisplayedToUserId: Int = R.string.generate_ai_content_failure_message
): Failure(
    originalExceptionMessage,
    messageToBeDisplayedToUserId
)