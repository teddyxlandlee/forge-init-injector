package xland.gradle.forgeInitInjector.test

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.task
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import xland.gradle.forgeInitInjector.*

/*
*   class WhitelistRegistryEvent {
*       private val charList: MutableSet<Char> = DEFAULT_WHITELIST.toMutableSet()
*       fun add(c: Char) { charList.add(c) }
*       fun add(vararg cs: Char) { cs.forEach(charList::add) }
*       internal fun charList() : Set<Char> = charList
*   }
*/
const val eeeModRegisterEvent = "wiki/mcbbs/mod/eee/WhitelistRegistryEvent"

@Suppress("UNUSED")
fun test(project : Project) {

    project.run {
        task("stubClassGen", type = StubClassGenTask::class) {
            stubPackage = "D5eludvRaDf3_XNFd4ul9"
            modId = "example_mod"
            setClientEntrypoint("net/example/example_mod/ExampleModMain", name = "initClient")
            setMainEntrypoint("net/example/example_mod/ExampleModMain")
            setServerEntrypoint("net/example/example_mod/server/ExampleModServerSupport")
            subscriptions.addClassPredicate("WmILjFiPRJHZhS7zflHaI/A",
                object : groovy.lang.Closure<Handle>(this@task, this@task) {
                    fun doCall(
                        className: String,
                        cv: ClassVisitor,
                        definer: NewClassAcceptor,
                        nameItr: Iterator<String>
                    ): Handle {
                        val stubName = nameItr.next()
                        ClassWriter(3).also { cw ->
                            cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, stubName, null, "java/lang/Object", null)
                            cw.visitMethod(
                                ACC_STATIC + ACC_PUBLIC,
                                nameItr.next(),
                                "(L${eeeModRegisterEvent};)V",
                                null,
                                null
                            ).run {
                                subscribeEvent(this)
                                visitCode()
                                visitVarInsn(ALOAD, 0)
                                visitLdcInsn(0xe901)
                                visitMethodInsn(INVOKEVIRTUAL, eeeModRegisterEvent, "add", "(C)V", false)
                                visitVarInsn(ALOAD, 0)
                                visitMethodInsn(
                                    INVOKESTATIC,
                                    "net/example/example_mod/ExampleModMain",
                                    "getWhitelistedChars",
                                    "()[C",
                                    false
                                )
                                visitMethodInsn(INVOKEVIRTUAL, eeeModRegisterEvent, "add", "([C)V", false)
                                visitInsn(RETURN)
                                visitEnd()
                            }
                        }.let { definer.accept(stubName, it) }

                        val mn = "el$" + nameItr.next()
                        cv.visitMethod(ACC_STATIC, mn, "()V", null, null).run {
                            visitLdcInsn(Type.getObjectType(stubName))
                            registerStackTopToModBus(this)

                        }

                        return Handle(H_INVOKESTATIC, className, mn, "()V", false)
                    }
                }
            )
        }

        tasks.getByName("processResources", Copy::class) {
            this.dependsOn("stubClassGen")
            this.from("$buildDir/forgeInitInjector/classes") {
                it.into("")
            }
            //this.include("$buildDir/forgeInitInjector/classes")
        }
    }
}