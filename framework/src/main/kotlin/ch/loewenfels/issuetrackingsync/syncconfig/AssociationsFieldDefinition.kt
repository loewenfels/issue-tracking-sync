package ch.loewenfels.issuetrackingsync.syncconfig

abstract class AssociationsFieldDefinition(
    /**
     * The class can hold some associations
     */
    var associations: MutableMap<String, String> = mutableMapOf()
)