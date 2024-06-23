package xland.gradle.forgeInitInjector

enum class NeoForgeFlag {
    /** 1.20.2-1.20.4. Using `mods.toml` as meta file. */
	PRE_20_5,
	/** Since 1.20.5, meta file has been moved to `neoforge.mods.toml`.
    Several APIs are changed, e.g. EventBusSubscriber is no longer an inner class.
	*/
	POST_20_5,
	;
}
