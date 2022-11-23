package xland.gradle.forgeInitInjector.internal

internal class ByteVec(initialCapacity : UShort = 64u) {
    private var b : ByteArray = ByteArray(initialCapacity.toInt())
    private var len = 0
    //val size get() = len
    val data get() = b.copyOf(len)

    private fun ensureCapacity(i : Int) {
        val ns = len + i
        if (ns > b.size) b = b.copyOf(ns)
    }

    fun putByte(byte: Int) {
        ensureCapacity(1)
        b[len++] = byte.toByte()
    }

    fun putShort(short: Int) {
        ensureCapacity(2)
        b[len] = short.ushr(8).toByte()
        b[len+1] = short.toByte()
        len += 2
    }

    fun putInt(i: Int) {
        ensureCapacity(4)
        b[len] = i.ushr(24).toByte()
        b[len+1] = i.ushr(16).toByte()
        b[len+2] = i.ushr(8).toByte()
        b[len+3] = i.toByte()
        len += 4
    }

    fun putBytes(arr: ByteArray, offset: Int, length: Int) {
        ensureCapacity(length)
        for (i in 0 until length) {
            b[len + i] = arr[offset + i]
        }
        len += length
    }

    fun putBytes(arr: ByteArray) = putBytes(arr, 0, arr.size)

    fun putStringNullable(s: String?) {
        if (s != null) putUtf8(s)
        else putInt(0)
    }

    fun putUtf8(s: String) {
        val b = s.toByteArray()
        ensureCapacity(4 + b.size)
        putInt(b.size)
        putBytes(b)
    }

    fun checkEqual(byteArray: ByteArray) : Boolean {
        if (len != byteArray.size) return false
        for (i in byteArray.indices) {
            if (b[i] != byteArray[i]) return false
        }
        return true
    }
}