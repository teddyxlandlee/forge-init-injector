package xland.gradle.forgeInitInjector.internal

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

internal class ClassLoadedPredicate(theClass: String) : HasModPredicate() {
    private val c : String = theClass.replace('/', '.')

    override fun genPredicateMethod(ownerClass: String, cv: ClassVisitor): String {
        val mn = "cld$" + asJavaIdentifier(c)
        val hash : Int = lnHash(mn)
        cv.visitMethod(
            Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE + Opcodes.ACC_SYNTHETIC,
                mn, "()Z", null, null).run {
            val b01 = Label(); val b02 = Label(); val b03 = Label()
            visitCode()

            visitLineNumber(hash + 1, b01)
            visitLineNumber(hash + 2, b02)
            visitLineNumber(hash + 3, b03)
            visitTryCatchBlock(b01, b02, b03, "java/lang/ClassNotFoundException")

            visitJumpInsn(Opcodes.GOTO, b01)
            visitLabel(b03)
            visitInsn(Opcodes.POP)
            visitInsn(Opcodes.ICONST_0)
            visitInsn(Opcodes.IRETURN)
            visitLabel(b01)
            visitLdcInsn(c) // String net.example.Main
            visitInsn(Opcodes.ICONST_0)
            visitLdcInsn(Type.getObjectType(ownerClass))
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader",
                "()Ljava/lang/ClassLoader;", false)
            visitMethodInsn(
                Opcodes.INVOKESTATIC, "java/lang/Class", "forName",
                        "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false)
            visitInsn(Opcodes.POP)
            visitLabel(b02)
            visitInsn(Opcodes.ICONST_1)
            visitInsn(Opcodes.IRETURN)
            visitMaxs(-1, -1)
            visitEnd()
        }
        return mn
    }
}