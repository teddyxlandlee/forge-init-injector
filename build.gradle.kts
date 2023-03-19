import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    `java-gradle-plugin`
    `maven-publish`
}

group = "xland.gradle"
version = "1.0.7"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation("org.ow2.asm", "asm", "9.4")
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
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    repositories {
        mavenLocal()
    }
}
