package ch.loewenfels.issuetrackingsync.syncclient

interface IssueQueryBuilder {

    /** Prepare issue query builder */
    fun build(field: Any, fieldValue: String): Any

}