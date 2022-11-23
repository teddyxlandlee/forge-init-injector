@file:JvmName("StubClassGenKt")

package xland.gradle.forgeInitInjector

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import xland.gradle.forgeInitInjector.internal.ByteVec
import java.io.File
import java.security.MessageDigest

abstract class StubClassGenTask : DefaultTask() {
    @Input lateinit var stubPackage : String
    @Input lateinit var modId : String
    @Input var mainEntrypoint : Handle? = null
    @Input var clientEntrypoint : Handle? = null
    @Input var serverEntrypoint: Handle? = null
    fun setMainEntrypoint(owner: String, name: String = "init", desc: String = "()V", handle: Int = H_INVOKESTATIC, isInterface : Boolean = false) {
        mainEntrypoint = Handle(handle, owner, name, desc, isInterface)
    }
    fun setClientEntrypoint(owner: String, name: String = "init", desc: String = "()V", handle: Int = H_INVOKESTATIC, isInterface : Boolean = false) {
        clientEntrypoint = Handle(handle, owner, name, desc, isInterface)
    }
    fun setServerEntrypoint(owner: String, name: String = "init", desc: String = "()V", handle: Int = H_INVOKESTATIC, isInterface : Boolean = false) {
        serverEntrypoint = Handle(handle, owner, name, desc, isInterface)
    }
    @Input
    val subscriptions = ModSubscriptions { modId }

    private val rootOutputDir get() =
        project.buildDir.resolve("forgeInitInjector")
    private val outputDir get() = rootOutputDir.resolve("classes")
    private val checksumDir get() = rootOutputDir.resolve("checksums")

    init {
        this.outputs.upToDateWhen { isUpToDate() }
    }

    @TaskAction
    fun generate() {
        val checksumEngine = ByteVec()
        val nameItr = nameItr()
        val newClassAcceptor = NewClassAcceptor { s, classWriter ->
            val b = classWriter.toByteArray()
            outputDir.resolve("${s}.class").writeBytes(b)
            synchronized(checksumEngine) {
                checksumEngine.putUtf8(s)
                checksumEngine.putBytes(b.sha256)
            }
        }

        val modClassName = "$stubPackage/${nameItr.next()}"
        val cw = ClassWriter(3)
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, modClassName, null,
                    "java/lang/Object", null)
        cw.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true).run {
            visit("value", modId)
            visitEnd()
        }
        cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).run {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            subscriptions.insertModConstructor(modClassName, cw, this, newClassAcceptor, nameItr)
            visitInsn(RETURN)
            visitMaxs(-1, -1)
            visitEnd()
        }
        newClassAcceptor.accept(modClassName, cw)

        listOf(
            PresetEntrypointContainer("net/minecraftforge/fml/event/lifecycle/FMLCommonSetupEvent", 0),
            PresetEntrypointContainer("net/minecraftforge/fml/event/lifecycle/FMLClientSetupEvent", -1),
            PresetEntrypointContainer("net/minecraftforge/fml/event/lifecycle/FMLDedicatedServerSetupEvent", 1)
        ).forEach {
            it.writeClass(nameItr::next)?.let { p -> newClassAcceptor.accept(p.first, p.second) }
        }

        // Finalize
        checksumDir.resolve("header.dat").writeBytes(headerToBytes().data)
        checksumDir.resolve("checksum.dat").writeBytes(checksumEngine.data)
    }

    private inner class PresetEntrypointContainer(val lifecycleEvent: String,
                                            val dist: Int /*-1 client, 1 server, 0 normal*/) {
        fun writeClass(nameGen: () -> String) : Pair<String, ClassWriter>? {
            val handle = entrypoint ?: return null
            val name = nameGen()
            val cw = ClassWriter(3)
            cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, name, null,
                "java/lang/Object", arrayOf("java/lang/Runnable")
            )
            cw.visitAnnotation("Lnet/minecraftforge/fml/common/Mod\$EventBusSubscriber;", true).run {
                visit("modid", modId)
                visitEnum("bus", "Lnet/minecraftforge/fml/common/Mod\$EventBusSubscriber\$Bus;",
                    "MOD")
                if (dist != 0) {
                    visitArray("value").run {
                        visitEnum(null,
                            "Lnet/minecraftforge/api/distmarker/Dist;",
                            if (dist < 0) "CLIENT" else "SERVER")
                        visitEnd()
                    }
                }
                visitEnd()
            }
            val methodNameItr = nameItr()

            cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "ev\$${methodNameItr.next()}",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(lifecycleEvent)),
                    null, null).run {
                subscribeEvent(this)
                visitCode()
                visitTypeInsn(NEW, name)
                visitInsn(DUP)
                visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false)
                visitVarInsn(ALOAD, 0)
                visitInsn(SWAP)
                visitMethodInsn(INVOKEVIRTUAL, "net/minecraftforge/fml/event/lifecycle/ParallelDispatchEvent",
                    "enqueueWork", "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", false)
                visitInsn(POP)
                visitInsn(RETURN)
                visitMaxs(-1, -1)
                visitEnd()
            }

            cw.visitMethod(0, "<init>", "()V", null, null).run {
                visitCode()
                visitVarInsn(ALOAD, 0)
                visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                visitInsn(RETURN)
                visitMaxs(-1, -1)
                visitEnd()
            }

            cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null).run {
                visitCode()
                handleTag(handle, this, name) { modId }
                visitInsn(RETURN)
                visitMaxs(-1, -1)
                visitEnd()
            }

            return name to cw
        }

        private val entrypoint : Handle? get() =
            if (dist < 0)
                clientEntrypoint
            else if (dist > 0)
                serverEntrypoint
            else mainEntrypoint
    }

    private fun isUpToDate() : Boolean {
        outputDir.resolve(stubPackage).mkdirs()
        checksumDir.resolve(stubPackage).mkdirs()
        if (subscriptions.isEmpty()) return false   // predicates not serializable
        // verify up-to-date
        return checksumDir.resolve("header.dat").let root@ {
            if (!it.exists()) {
                return@root false
            } else {
                val vec = headerToBytes()
                if (vec.checkEqual(it.readBytes())) {
                    // metadata up-to-date
                    val vec2 = ByteVec(68u)
                    for (file in outputDir.walk()) {
                        if (file.isDirectory) continue
                        val f = file.relativeToOrNull(outputDir)?.name ?: return@root false
                        vec2.putUtf8(f)
                        vec2.putBytes(file.sha256)
                    }
                    return@root vec2.checkEqual(checksumDir.resolve("files.dat").run {
                        if (exists()) readBytes()
                        else ByteArray(0)
                    })
                }
                return@root false
            }
        }
    }

    private fun headerToBytes() : ByteVec {
        return ByteVec().apply {
            putShort(0x99F1)  // magic
            putByte(1)   // schema
            putUtf8(stubPackage)
            putUtf8(modId)
            putStringNullable(mainEntrypoint?.toString())
            putStringNullable(clientEntrypoint?.toString())
            putStringNullable(serverEntrypoint?.toString())
        }
    }
}

private val ByteArray.sha256 : ByteArray get() =
    MessageDigest.getInstance("SHA-256").digest(this)

private val File.sha256 : ByteArray get() {
    inputStream().buffered().use { bis ->
        MessageDigest.getInstance("SHA-256").run {
            val ba = ByteArray(8192)
            while (true) {
                val len = bis.read(ba)
                if (len < 0) break
                update(ba, 0, len)
            }
            return digest()
        }
    }
}

// len: 54
private val nameMap = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$".toCharArray()

fun nameItr() : Iterator<String> {
    return object : Iterator<String> {
        private var nm : ByteArray = ByteArray(0)

        override fun hasNext() = true

        override fun next(): String {
            val i = nm.lastIndex
            if (nm.isEmpty() || nm[i].toInt() == 53 /*nameMap.lastIndex, '$'*/)
                nm = ByteArray(i + 2)   // auto-fill zeros
            else
                nm[i] = (nm[i] + 1).toByte()
            return nm.map { nameMap[it.toInt()].code.toByte() }
                .toByteArray().let { String(it, Charsets.ISO_8859_1) }
        }

    }
}

@JvmSynthetic
internal fun handleTag(handle: Handle, mv: MethodVisitor,
                       className : String, modId: () -> String) {
    val mt = Type.getMethodType(handle.desc)
    mt.argumentTypes.let { argumentTypes ->
        if (argumentTypes.isNotEmpty()) {
            argumentTypes.forEach { typeEach ->
                when (typeEach.descriptor) {
                    "Ljava/lang/String;" -> mv.visitLdcInsn(modId()) /* String mod_id */
                    "Ljava/lang/Class;"  -> mv.visitLdcInsn(Type.getObjectType(className))
                    "Ljava/lang/Object;" -> mv.visitVarInsn(ALOAD, 0) /* this, when not static */
                    "Ljava/lang/invoke/MethodHandles\$Lookup;" ->
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles",
                            "lookup", "()Ljava/lang/invoke/MethodHandles\$Lookup;",
                            false)
                    else -> when (typeEach.sort) {
                        Type.ARRAY, Type.OBJECT -> mv.visitInsn(ACONST_NULL)
                        Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> mv.visitInsn(ICONST_0)
                        Type.FLOAT -> mv.visitInsn(FCONST_0)
                        Type.LONG -> mv.visitInsn(LCONST_0)
                        Type.DOUBLE -> mv.visitInsn(DCONST_0)
                    }
                }
            }
        }
    }
    var op = when (handle.tag) {
        H_GETFIELD -> GETFIELD
        H_GETSTATIC -> GETSTATIC
        H_PUTFIELD -> PUTFIELD
        H_PUTSTATIC -> PUTSTATIC
        H_INVOKEVIRTUAL -> INVOKEVIRTUAL
        H_INVOKESTATIC -> INVOKESTATIC
        H_INVOKESPECIAL -> INVOKESPECIAL
        H_INVOKEINTERFACE -> INVOKEINTERFACE
        else -> -1 /* newInvokeSpecial */
    }
    if (op < 0) {
        mv.visitTypeInsn(NEW, handle.owner)
        // cancel DUP: we don't want the returned object
        op = INVOKESPECIAL
    }
    mv.visitMethodInsn(op, handle.owner, handle.name, handle.desc, handle.isInterface)

    when (mt.returnType.size) {
        1 -> mv.visitInsn(POP)
        2 -> mv.visitInsn(POP2)
    }
}

// Stack: -1 object, max +1
fun registerStackTopToModBus(mv: MethodVisitor) {
    mv.run {
        visitMethodInsn(INVOKESTATIC, "net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext", "get",
            "()Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext", false)
        visitMethodInsn(INVOKEVIRTUAL, "net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext", "getModEventBus",
            "()Lnet/minecraftforge/eventbus/api/IEventBus;", false)
        visitInsn(SWAP)
        visitMethodInsn(INVOKEINTERFACE, "net/minecraftforge/eventbus/api/IEventBus", "register",
            "(Ljava/lang/Object;)V", true)
    }
}

fun subscribeEvent(mv: MethodVisitor) {
    mv.visitAnnotation("Lnet/minecraftforge/eventbus/api/SubscribeEvent;", true)
        .visitEnd()
}
