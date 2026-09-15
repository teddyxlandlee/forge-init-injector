import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
    `java-gradle-plugin`
    `maven-publish`
}

group = "xland.gradle"
version = project.ext["project_version"]!!

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation("org.ow2.asm:asm:9.8")
    testImplementation(kotlin("test"))
    testImplementation(gradleKotlinDsl())
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

gradlePlugin {
    plugins.create("forge-init-injector") {
        id = "xland.gradle.forge-init-injector"
        displayName = "Forge Init Injector"
        implementationClass = "xland.gradle.forgeInitInjector.ForgeInitInjectorPlugin"
        description = "Add Forge initializer to the mods that doesn't introduce Forge dependency"
    }
}

tasks.processResources {
    from("LICENSE.txt") {
        rename {"META-INF/LICENSE_${project.name}.txt" }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
}

tasks.jar {
    manifest.attributes(
        "Specification-Title" to "Forge Init Injector",
        "Specification-Vendor" to "teddyxlandlee",
        "Specification-Version" to project.ext["spec_version"]!!,
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
    )
}

publishing {
    repositories {
        mavenLocal()
    }
}
