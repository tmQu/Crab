package tkpm.com.crab.objects

class Message(message: String, sentBy: String, listSuggestions: List<Suggestion> = emptyList()) {
    var message: String
    var sentBy: String
    var listSuggestions: List<Suggestion>


    init {
        this.message = message
        this.sentBy = sentBy
        this.listSuggestions = listSuggestions
    }

    companion object {
        var SENT_BY_ME = "user"
        var SENT_BY_CHATGPT = "assistant"
    }

}