package com.hanwha.mcp.bootstrap.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.hanwha.mcp.application.service.AnalyzeProjectStructureService;
import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import com.hanwha.mcp.infrastructure.adapter.FileSystemProjectStructureAnalyzer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectStructureMcpAdapterTest {

	@TempDir
	Path projectDir;

	@Test
	void analyzeProjectStructureReturnsProjectMetadata() throws IOException {
		var registry = new SimpleMeterRegistry();
		var adapter = adapter(registry);
		write("settings.gradle", "rootProject.name = 'sample-app'\n");
		write(
			"build.gradle",
			"""
			plugins {
				id 'java'
				id 'org.springframework.boot' version '3.5.1'
			}

			java {
				toolchain {
					languageVersion = JavaLanguageVersion.of(21)
				}
			}

			dependencies {
				implementation 'org.springframework.boot:spring-boot-starter-web'
				testImplementation 'org.springframework.boot:spring-boot-starter-test'
			}
			""");
		write(
			"src/main/java/com/example/app/Application.java",
			"""
			package com.example.app;

			class Application {
			}
			""");

		var response = adapter.analyzeProjectStructure(this.projectDir.toString());

		assertThat(response.projectName()).isEqualTo("sample-app");
		assertThat(response.build().javaVersion()).isEqualTo("21");
		assertThat(response.build().springBootVersion()).isEqualTo("3.5.1");
		assertThat(response.packages()).extracting("packageName").containsExactly("com.example.app");
		assertThat(registry.counter("mcp.tool.invocation.count", "tool", "analyze_project_structure").count())
			.isEqualTo(1.0);
	}

	@Test
	void analyzeProjectStructureRejectsBlankPath() {
		var adapter = adapter(new SimpleMeterRegistry());

		assertThatThrownBy(() -> adapter.analyzeProjectStructure(" "))
			.isInstanceOf(InvalidMcpInputException.class)
			.hasMessageContaining("projectPath must not be blank");
	}

	private ProjectStructureMcpAdapter adapter(SimpleMeterRegistry registry) {
		return new ProjectStructureMcpAdapter(
			new AnalyzeProjectStructureService(new FileSystemProjectStructureAnalyzer()),
			Validation.buildDefaultValidatorFactory().getValidator(),
			registry);
	}

	private void write(String relativePath, String content) throws IOException {
		Path file = this.projectDir.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

}
