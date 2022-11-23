package xland.gradle.forgeInitInjector.internal

import org.objectweb.asm.ClassVisitor

internal sealed class HasModPredicate {
    /* @return generated method name */
    /* Generated method must be static, no-args and return boolean. */
    abstract fun genPredicateMethod(ownerClass: String, cv: ClassVisitor) : String

    //final override fun genMethod(ownerClass: String, cv: ClassVisitor): FieldOrMethodInfo =
    //    genPredicateMethod(ownerClass, cv).run { FieldOrMethodInfo(ownerClass, this, "()Z") }
}
