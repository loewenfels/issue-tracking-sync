package ch.loewenfels.issuetrackingsync.syncconfig

data class AdditionalFields(
    /**
     *  SimpleField is used to save a singleSelection or text field
     */
    var enumerationFields: MutableMap<String, String> = mutableMapOf(),
    /**
     * MultiselectField will save a
     */
    var multiselectFields: MutableMap<String, String> = mutableMapOf(),

    /**
     * simpleTextFields will just store the given text
     */
    var simpleTextFields: MutableMap<String, String> = mutableMapOf()
)