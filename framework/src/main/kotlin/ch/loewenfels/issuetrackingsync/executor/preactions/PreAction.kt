package ch.loewenfels.issuetrackingsync.executor.preactions

interface PreAction {

    fun execute(
        event: PreActionEvent
    )

}