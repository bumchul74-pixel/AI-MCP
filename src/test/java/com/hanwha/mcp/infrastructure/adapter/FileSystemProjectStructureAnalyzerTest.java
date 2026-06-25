package com.hanwha.mcp.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemProjectStructureAnalyzerTest {

	@TempDir
	Path projectDir;

	private final FileSystemProjectStructureAnalyzer analyzer = new FileSystemProjectStructureAnalyzer();

	@Test
	void analyzesGradleKotlinSpringBootProject() throws IOException {
		write("settings.gradle.kts", "rootProject.name = \"sample-app\"\n");
		write(
			"build.gradle.kts",
			"""
			plugins {
				java
				id("org.springframework.boot") version "3.5.1"
			}

			java {
				toolchain {
					languageVersion.set(JavaLanguageVersion.of(21))
				}
			}

			dependencies {
				implementation("org.springframework.boot:spring-boot-starter-web")
				testImplementation("org.springframework.boot:spring-boot-starter-test")
			}
			""");
		write(
			"src/main/java/com/example/app/Application.java",
			"""
			package com.example.app;

			class Application {
			}
			""");
		write(
			"src/main/java/com/example/app/service/UserService.java",
			"""
			package com.example.app.service;

			class UserService {
			}
			""");
		write(
			"src/test/java/com/example/app/ApplicationTests.java",
			"""
			package com.example.app;

			import org.junit.jupiter.api.Test;
			import org.springframework.boot.test.context.SpringBootTest;

			@SpringBootTest
			class ApplicationTests {
				@Test
				void contextLoads() {
				}
			}
			""");

		var analysis = this.analyzer.analyze(this.projectDir.toString());

		assertThat(analysis.projectName()).isEqualTo("sample-app");
		assertThat(analysis.build().present()).isTrue();
		assertThat(analysis.build().fileName()).isEqualTo("build.gradle.kts");
		assertThat(analysis.build().kotlinDsl()).isTrue();
		assertThat(analysis.build().javaVersion()).isEqualTo("21");
		assertThat(analysis.build().springBootVersion()).isEqualTo("3.5.1");
		assertThat(analysis.build().dependencies())
			.extracting("notation")
			.contains(
				"org.springframework.boot:spring-boot-starter-web",
				"org.springframework.boot:spring-boot-starter-test");
		assertThat(analysis.packages())
			.extracting("packageName")
			.containsExactly("com.example.app", "com.example.app.service");
		assertThat(analysis.testStructure().present()).isTrue();
		assertThat(analysis.testStructure().directories()).contains("src/test/java");
		assertThat(analysis.testStructure().testSourceFileCount()).isEqualTo(1);
		assertThat(analysis.testStructure().testClassCount()).isEqualTo(1);
		assertThat(analysis.testStructure().frameworks()).contains("JUnit 5", "Spring Boot Test");
	}

	@Test
	void rejectsMissingProjectPath() {
		Path missingPath = this.projectDir.resolve("missing");

		assertThatThrownBy(() -> this.analyzer.analyze(missingPath.toString()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("projectPath does not exist");
	}

	private void write(String relativePath, String content) throws IOException {
		Path file = this.projectDir.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

}
