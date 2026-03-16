package xland.gradle.forgeInitInjector

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import javax.inject.Inject

private const val TASK_NAME = "generateStubForgeInitInjectorClasses"
val pluginVersion : String get() = ForgeInitInjectorPlugin::class.java.`package`.specificationVersion

@Suppress("UNUSED")
open class ForgeInitInjectorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("forgeInitInjector", ForgeInitInjectorExtension::class.java)

        project.tasks.findByName("processResources").let {
            if (it !is Copy) return@let
            it.dependsOn(TASK_NAME)
            it.from(project.layout.buildDirectory.dir("forgeInitInjector/classes")) { p ->
                p.into("")  // root
            }
        }

        project.tasks.findByName("jar").let {
            if (it !is Jar) return@let
            it.manifest { m ->
                m.attributes(mapOf("ForgeInitInjector" to pluginVersion))
            }
        }
    }
}

@Suppress("UNUSED")
open class ForgeInitInjectorExtension @Inject constructor(project: Project)  {
    private val wrappedProvider: TaskProvider<StubClassGenTask> = project.tasks.register(TASK_NAME, StubClassGenTask::class.java)
    val wrapped: StubClassGenTask get() = wrappedProvider.get()

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
    var supportLegacyForgeLifecycle: Boolean
        get() = wrapped.supportLegacyForgeLifecycle
        set(value) { wrapped.supportLegacyForgeLifecycle = value }
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
