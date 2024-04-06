import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.22"
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.nomisrev"
version = "0.0.1"
description = "Pi4J Sample Code"

repositories {
  mavenCentral()
}

dependencies {
  api("org.slf4j:slf4j-api:2.0.12")
  api("org.slf4j:slf4j-simple:2.0.12")
  api("com.pi4j:pi4j-core:2.5.1")
  api("com.pi4j:pi4j-plugin-raspberrypi:2.5.1")
  api("com.pi4j:pi4j-library-gpiod:2.5.1")
  api("com.pi4j:pi4j-plugin-gpiod:2.5.1")
}

java {
  sourceCompatibility = VERSION_11
  targetCompatibility = VERSION_11
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
}

tasks.withType<Jar> {
  manifest { attributes["Main-Class"] = "io.github.nomisrev.AppKt" }
}
