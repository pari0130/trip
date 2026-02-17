plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
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
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.flywaydb:flyway-core")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.register("installGitHooks") {
	description = "Git hooks 경로를 scripts/git-hooks로 설정"
	group = "setup"
	doLast {
		exec {
			commandLine("git", "config", "core.hooksPath", "scripts/git-hooks")
		}
		println("[setup] Git hooks 경로 설정 완료: scripts/git-hooks")
	}
}

tasks.named("build") {
	dependsOn("installGitHooks")
}

ktlint {
	version.set("1.1.1")
	android.set(false)
	outputToConsole.set(true)
	ignoreFailures.set(false)
	filter {
		exclude("**/generated/**")
	}
}
