package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.*
import ch.loewenfels.issuetrackingsync.syncclient.IssueClientException
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

/**
 * This mapper updates the issue state. This is not simply a field update, but a state transition (JIRA: 'transition', RTC: 'action')
 * The mapper expects state display names in the [associations]
 *
 * This mapper works with [Pair] values
 */
open class StatusFieldMapper(fieldMappingDefinition: FieldMappingDefinition) : FieldMapper, Logging {
    private val associations: Map<String, String> = fieldMappingDefinition.associations
    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Any? =
        Pair(issueTrackingClient.getState(proprietaryIssue), issueTrackingClient.getStateHistory(proprietaryIssue))

    @Suppress("UNCHECKED_CAST")
    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        if (value !is Pair<*, *>) {
            throw IssueClientException("This mapper can only set Pair values, got $value instead")
        }
        val internalTargetIssue = issue.proprietaryTargetInstance
            ?: throw IllegalStateException("This mapper expects a previous action to have loaded the target instance")
        val currentStateOfTarget = issueTrackingClient.getState(internalTargetIssue as T)
        logger().info("Current state of source: ${(value.first as String)}, target: $currentStateOfTarget")
        val states = value.second as List<StateHistory>
        if (states.isNotEmpty()) {
            val finalState = states.last()
            val correspondingStates = (associations[finalState.toState].orEmpty()).split("[,;/]".toRegex())
            if (correspondingStates.last() == currentStateOfTarget) {
                // Current state of target is same as source state, so ignore transition
                return
            }
        }
        getStatePath(states, currentStateOfTarget).forEach {
            logger().info("Attempting to transition to state $it")
            issueTrackingClient.setState(internalTargetIssue as T, it)
            issue.hasChanges = true
        }
    }

    /**
     * Expects [sourceStateHistory] to be ordered with oldest entry at index 0.
     */
    private fun getStatePath(sourceStateHistory: List<StateHistory>, currentTargetState: String): List<String> {
        val result = mutableListOf<String>()
        var statesInSync = true

        sourceStateHistory.forEach { stateHistory ->
            val correspondingStateInTargetWorld = getCorrespondingStatesInTargetWorld(stateHistory)
            if (correspondingStateInTargetWorld.contains(currentTargetState)) {
                // sync for multi-transitions,
                val positionInMultiTransition = correspondingStateInTargetWorld.indexOf(currentTargetState) + 1
                if (correspondingStateInTargetWorld.size > positionInMultiTransition) {
                    addMissingStates(
                        correspondingStateInTargetWorld.subList(
                            positionInMultiTransition,
                            correspondingStateInTargetWorld.size
                        ), result
                    )
                }
                // and then from here on:
                statesInSync = false
            } else if (!statesInSync) {
                addMissingStates(correspondingStateInTargetWorld, result)
            }
        }
        return result.filter(String::isNotBlank)
    }

    /**
     * The [associations] can define a comma-separated list of target states. This is useful if a source transition
     * spans multiple transitions on the target side.
     */
    private fun getCorrespondingStatesInTargetWorld(sourceStateHistory: StateHistory): List<String> {
        val states = associations["${sourceStateHistory.fromState}->${sourceStateHistory.toState}"] ?:
        associations[sourceStateHistory.toState]

        return (states.orEmpty()).split("[,;/]".toRegex())
    }

    /**
     * Add entries while ensuring that there are no states immediately repeating themselves. Thus
     * `'In Work', 'Interrupted', 'In Work'` is allowed, but `'In Work', 'In Work'` is not
     */
    private fun addMissingStates(states: List<String>, result: MutableList<String>) {
        if (result.isEmpty().not()) {
            val last = result.last()

            if (states.contains(last)) {
                states.subList(states.indexOf(last) + 1, states.size).forEach { result.add(it) }
            } else {
                states
                    .filter { result.isEmpty() || last != it }
                    .forEach { result.add(it) }
            }

        } else {
            states.forEach { result.add(it) }
        }
    }
}