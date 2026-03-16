package xland.gradle.forgeInitInjector.internal

import java.util.*
import kotlin.math.absoluteValue

private val b64Encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
internal fun String.asJavaIdentifier() : String {
    return b64Encoder.encodeToString(toByteArray(Charsets.UTF_8))
        .replace('-', '$')
}

internal fun String.lnHash() = hashCode().mod(100).absoluteValue * 100
