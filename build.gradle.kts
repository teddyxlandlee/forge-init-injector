import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

group = "xland.gradle"
version = "1.0-SNAPSHOT"

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

tasks.processResources {
    from("LICENSE.txt") {
        into("META-INF/LICENSE_${project.name}.txt")
    }
    from(file("doc")) {
        into("")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}