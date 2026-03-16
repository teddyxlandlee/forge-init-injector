package xland.gradle.forgeInitInjector

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle

@FunctionalInterface
fun interface TargetMethodGen {
    fun genMethod(ownerClass : String, cv : ClassVisitor, definer: NewClassAcceptor,
                  nameIterator: Iterator<String>) : Handle
}

