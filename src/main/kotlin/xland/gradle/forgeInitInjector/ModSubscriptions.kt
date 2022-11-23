package xland.gradle.forgeInitInjector

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import xland.gradle.forgeInitInjector.internal.ClassLoadedPredicate
import xland.gradle.forgeInitInjector.internal.HasModPredicate

class ModSubscriptions(private val modId: () -> String) {
    private val m : MutableMap<HasModPredicate, TargetMethodGen> = hashMapOf()
    @JvmSynthetic internal fun isEmpty() = m.isEmpty()

    private fun add(predicate: HasModPredicate, cvConsumer: TargetMethodGen) {
        m[predicate] = cvConsumer
    }

    fun addClassPredicate(className: String, cvConsumer: TargetMethodGen) =
        add(ClassLoadedPredicate(className), cvConsumer)

    fun addClassPredicate(className: String, cvConsumer: groovy.lang.Closure<Handle>) {
        addClassPredicate(className, TargetMethodGen { s, classVisitor, definer, itr ->
            cvConsumer.call(s, classVisitor, definer, itr)
        })
    }

    fun insertModConstructor(className: String, cv: ClassVisitor, mv: MethodVisitor,
                             definer: NewClassAcceptor, nameItr: Iterator<String>) {
        if (m.isEmpty()) return
        var insertion = 1000
        m.forEach { (predicate, cvConsumer) ->
            val b01 = Label(); val b02 = Label(); val b03 = Label()
            mv.visitLineNumber(insertion + 1, b01)
            mv.visitLineNumber(insertion + 2, b02)
            mv.visitLineNumber(insertion + 3, b03)

            mv.visitLabel(b01)
            predicate.genPredicateMethod(className, cv).let {
                mv.visitMethodInsn(INVOKESTATIC, className, it, "()Z", false)
            }
            mv.visitJumpInsn(IFEQ, b03)
            mv.visitLabel(b02)
            handleTag(cvConsumer.genMethod(className, cv, definer, nameItr), mv, className, modId)
            mv.visitLabel(b03)

            insertion += 100
        }
    }
}