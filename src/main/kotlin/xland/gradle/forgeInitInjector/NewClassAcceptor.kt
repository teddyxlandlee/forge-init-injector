package xland.gradle.forgeInitInjector

import org.objectweb.asm.ClassWriter

fun interface NewClassAcceptor {
    fun accept(className: String, cw: ClassWriter)
}
