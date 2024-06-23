package xland.gradle.forgeInitInjector

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes

private const val TASK_NAME = "generateStubForgeInitInjectorClasses"
const val PLUGIN_VERSION = 2

@Suppress("UNUSED")
open class ForgeInitInjectorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val t : StubClassGenTask = project.tasks.create(TASK_NAME, StubClassGenTask::class.java)
        project.extensions.create("forgeInitInjector", ForgeInitInjectorExtension::class.java, t)

        project.tasks.findByName("processResources").let {
            if (it !is Copy) return@let
            it.dependsOn(t)
            it.from(project.buildDir.resolve("forgeInitInjector/classes")) { p ->
                p.into("")  // root
            }
        }

        project.tasks.findByName("jar").let {
            if (it !is org.gradle.api.tasks.bundling.Jar) return@let
            it.manifest { m ->
                m.attributes(mapOf("ForgeInitInjector" to PLUGIN_VERSION))
            }
        }
    }
}

@Suppress("UNUSED")
open class ForgeInitInjectorExtension(val wrapped: StubClassGenTask) {
    var stubPackage : String
        get() = wrapped.stubPackage
        set(value) { wrapped.stubPackage = value }
    var modId : String
        get() = wrapped.modId
        set(value) { wrapped.modId = value }
    var mainEntrypoint : Handle?
        get() = wrapped.mainEntrypoint
        set(value) { wrapped.mainEntrypoint = value }
    var clientEntrypoint : Handle?
        get() = wrapped.clientEntrypoint
        set(value) { wrapped.clientEntrypoint = value }
    var serverEntrypoint: Handle?
        get() = wrapped.serverEntrypoint
        set(value) { wrapped.serverEntrypoint = value }
    val neoFlags: MutableSet<NeoForgeFlag>
        get() = wrapped.neoFlags
    fun neoFlag(vararg flags: NeoForgeFlag) = wrapped.neoFlag(*flags)
    fun neoFlag(vararg flags: String) = wrapped.neoFlag(*flags)
    var supportNeo: Boolean
        //@Deprecated("NeoForge Flag provides a more comprehensive toggle", ReplaceWith("neoFlags.isNotEmpty()"))
    	get() = wrapped.supportNeo
    	@Deprecated("NeoForge Flag provides a more comprehensive toggle. Use `neoFlag()` instead.")
        @Suppress("DEPRECATION")
    	set(value) { wrapped.supportNeo = value }
    val subscriptions: ModSubscriptions get() = wrapped.subscriptions
    @JvmOverloads
    fun setMainEntrypoint(owner: String, name: String = "init", desc: String = "()V", handle: Int = Opcodes.H_INVOKESTATIC, isInterface : Boolean = false)
        = wrapped.setMainEntrypoint(owner, name, desc, handle, isInterface)
    @JvmOverloads
    fun setClientEntrypoint(owner: String, name: String = "init", desc: String = "()V", handle: Int = Opcodes.H_INVOKESTATIC, isInterface : Boolean = false)
        = wrapped.setClientEntrypoint(owner, name, desc, handle, isInterface)
    @JvmOverloads
    fun setServerEntrypoint(owner: String, name: String = "init", desc: String = "()V", handle: Int = Opcodes.H_INVOKESTATIC, isInterface : Boolean = false)
        = wrapped.setServerEntrypoint(owner, name, desc, handle, isInterface)
}
