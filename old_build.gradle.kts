//plugins {
//  kotlin("jvm") version "1.9.22"
////    application
////    id("com.github.johnrengelman.shadow") version "8.1.1"
//}
//
//group = "io.github.nomisrev"
//version = "1.0-SNAPSHOT"
//
////application {
////    mainClass = "io.github.nomisrev.ExampleKt"
////}
//
//repositories {
//  mavenCentral()
//}
//
//val slf4jVersion = "1.7.32"
//val pi4jVersion = "2.5.1"
//
//dependencies {
//  implementation("org.slf4j:slf4j-api:$slf4jVersion")
//  implementation("org.slf4j:slf4j-simple:$slf4jVersion")
//  api("com.pi4j:pi4j-core:$pi4jVersion")
//  api("com.pi4j:pi4j-plugin-raspberrypi:$pi4jVersion")
//  api("com.pi4j:pi4j-plugin-gpiod:$pi4jVersion")
//
//  testImplementation("org.jetbrains.kotlin:kotlin-test")
//}
//
////java {
////  modularity.inferModulePath.set(true)
////}
//
//tasks {
//  withType<JavaCompile> {
//    options.encoding = "UTF-8"
////    modularity.inferModulePath.set(true)
//  }
//  register("copyDistribution", Copy::class) {
//    from(configurations.runtimeClasspath)
//    from(named("jar"))
//    from(layout.projectDirectory.file("assets/run.sh"))
//    into(layout.buildDirectory.dir("distribution"))
//  }
//  register("run", Exec::class) {
//    commandLine("sh", "build/distribution/run.sh")
//  }
//  named("build") {
//    dependsOn("copyDistribution")
//  }
//}
//
//tasks.test {
//  useJUnitPlatform()
//}
//
//kotlin {
//  jvmToolchain(11)
//}
