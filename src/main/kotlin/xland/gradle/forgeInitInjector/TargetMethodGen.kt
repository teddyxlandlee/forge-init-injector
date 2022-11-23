package xland.gradle.forgeInitInjector

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle

@FunctionalInterface
interface TargetMethodGen {
    fun genMethod(ownerClass : String, cv : ClassVisitor, definer: NewClassAcceptor,
                  nameIterator: Iterator<String>) : Handle
}

@JvmSynthetic
fun TargetMethodGen(lambda : (String, ClassVisitor, NewClassAcceptor, Iterator<String>) -> Handle)
        : TargetMethodGen {
    return object : TargetMethodGen {
        override fun genMethod(ownerClass: String, cv: ClassVisitor, definer: NewClassAcceptor,
                               nameIterator: Iterator<String>): Handle {
            return lambda(ownerClass, cv, definer, nameIterator)
        }
    }
}
