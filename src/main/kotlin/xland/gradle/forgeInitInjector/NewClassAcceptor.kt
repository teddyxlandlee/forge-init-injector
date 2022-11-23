package xland.gradle.forgeInitInjector

import org.objectweb.asm.ClassWriter

interface NewClassAcceptor {
    fun accept(className: String, cw: ClassWriter)
}

fun NewClassAcceptor(lambda: (String, ClassWriter) -> Unit) : NewClassAcceptor =
    object : NewClassAcceptor {
        override fun accept(className: String, cw: ClassWriter) = lambda(className, cw)
    }
