plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.kotlin.jpa)
	alias(libs.plugins.ktlint)
}

group = "com.trip.hotel"
version = "0.0.1-SNAPSHOT"
description = "trip"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.flyway.core)
	implementation(libs.kotlin.reflect)
	implementation(libs.springdoc.openapi.webmvc.ui)
	implementation(libs.commons.lang3)
	runtimeOnly(libs.h2)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.mockito.kotlin)
	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

ktlint {
	version.set(libs.versions.ktlint.core.get())
	android.set(false)
	outputToConsole.set(true)
	ignoreFailures.set(false)
	filter {
		exclude("**/generated/**")
	}
}
