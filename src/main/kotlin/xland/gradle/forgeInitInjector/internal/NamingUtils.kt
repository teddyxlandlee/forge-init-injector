package xland.gradle.forgeInitInjector.internal

import java.util.*
import kotlin.math.absoluteValue

private val b64Encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
internal fun asJavaIdentifier(s: String) : String {
    return b64Encoder.encodeToString(s.toByteArray(Charsets.UTF_8))
        .replace('-', '$')
}

internal fun lnHash(s: String) = s.hashCode().mod(100).absoluteValue * 100
