package ch.loewenfels.issuetrackingsync

import org.mockito.AdditionalMatchers.not
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito

/**
 * Kotlin's "not null" paradigm clashing with Mockito is well documented, as an example see
 * eg. https://stackoverflow.com/questions/51868577/how-do-you-get-mockito-to-play-nice-with-kotlin-non-nullable-types
 */
@Suppress("UNCHECKED_CAST")
fun <T> any(type: Class<T>): T {
    Mockito.any(type)
    return null as T
}

fun <T> any(): T = Mockito.any<T>()

fun <T : Any> safeEq(value: T): T = eq(value) ?: value

fun <T : Any> safeNot(value: T): T = not(value) ?: value

inline fun <reified T : Any> genericMock() = Mockito.mock(T::class.java)