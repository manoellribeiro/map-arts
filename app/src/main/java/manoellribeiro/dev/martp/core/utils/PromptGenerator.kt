package manoellribeiro.dev.martp.core.utils

class PromptGenerator {
    fun generateArtDescriptionPrompt(
        countryName: String,
        cityName: String,
        streetName: String,
        languageToReturn: String
    ): String {
        return "You are being used to generate the description of interactive arts generated using" +
                " the user location. Your goal is to give the user a brief information about the location" +
                " he generated the art, it can be anything as saying the user is close to a touristic place," +
                " or the main goal of the place, or a curiosity about the name of the place." +
                " Try to be creative and remember you are being used in an art project." +
                "You should not used bullet points or any special characters" +
                "You should pay special attention with grammar and punctuation marks" +
                "Your text should not have more than 300 characters" +
                "Use the following location to generate:" +
                "country: $countryName" +
                "city: $cityName" +
                "street: $streetName" +
                "language to return: $languageToReturn"
    }

}