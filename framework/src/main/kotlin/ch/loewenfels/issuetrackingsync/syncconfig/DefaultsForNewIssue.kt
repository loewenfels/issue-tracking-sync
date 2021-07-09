package ch.loewenfels.issuetrackingsync.syncconfig

/**
 * Defaults to be used when creating a new issue in the target system. Some of these
 * defaults might subsequently be overwritten by a [FieldMapping]
 */
data class DefaultsForNewIssue(
    /**
     * An internal ID of an issue type. For JIRA this is typically a number, for RTC a FQN
     */
    var issueType: String = "",
    /**
     * A project reference. For JIRA this is typically a 3-4 letter acronym, for RTC this is the
     * project area name.
     */
    var project: String = "",
    /**
     * The category within the project. JIRA does not use this value, for RTC theses are slash-separated
     * categories such as "BUS/SRV/FIN"
     */
    var category: String = "",

    /**
     * CustomFields which have to be entered to create a new JIRA issue.
     */
    var additionalFields: AdditionalFields = AdditionalFields()
)

