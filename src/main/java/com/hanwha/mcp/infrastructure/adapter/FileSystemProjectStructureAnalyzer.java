package com.hanwha.mcp.infrastructure.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.hanwha.mcp.domain.model.GradleBuildAnalysis;
import com.hanwha.mcp.domain.model.GradleDependency;
import com.hanwha.mcp.domain.model.JavaPackageSummary;
import com.hanwha.mcp.domain.model.ProjectStructureAnalysis;
import com.hanwha.mcp.domain.model.TestStructureAnalysis;
import com.hanwha.mcp.domain.repository.ProjectStructureAnalyzer;

public class FileSystemProjectStructureAnalyzer implements ProjectStructureAnalyzer {

	private static final int MAX_JAVA_FILES = 3_000;
	private static final int PACKAGE_SCAN_LINE_LIMIT = 80;
	private static final String DEFAULT_PACKAGE = "(default)";
	private static final Pattern PACKAGE_DECLARATION = Pattern.compile(
		"^\\s*package\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)\\s*;");
	private static final Pattern ROOT_PROJECT_NAME = Pattern.compile("rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]");
	private static final Pattern SPRING_BOOT_PLUGIN = Pattern.compile(
		"id\\s*(?:\\(\\s*)?['\"]org\\.springframework\\.boot['\"]\\s*(?:\\))?\\s*version\\s*['\"]([^'\"]+)['\"]");
	private static final List<Pattern> JAVA_VERSION_PATTERNS = List.of(
		Pattern.compile("JavaLanguageVersion\\.of\\((\\d+)\\)"),
		Pattern.compile("JavaVersion\\.VERSION_(\\d+)"),
		Pattern.compile("sourceCompatibility\\s*=\\s*['\"]?(\\d+)['\"]?"),
		Pattern.compile("targetCompatibility\\s*=\\s*['\"]?(\\d+)['\"]?"));
	private static final Pattern WRAPPED_DEPENDENCY = Pattern.compile(
		"^([A-Za-z][A-Za-z0-9_-]*)\\s*(?:\\(\\s*)?(?:platform|enforcedPlatform)\\s*\\(\\s*['\"]([^'\"]+:[^'\"]+)['\"]");
	private static final Pattern DIRECT_DEPENDENCY = Pattern.compile(
		"^([A-Za-z][A-Za-z0-9_-]*)\\s*(?:\\(\\s*)?['\"]([^'\"]+:[^'\"]+)['\"]");
	private static final Set<String> DEPENDENCY_CONFIGURATIONS = Set.of(
			"api",
			"implementation",
			"compileOnly",
			"runtimeOnly",
			"annotationProcessor",
			"testImplementation",
			"testCompileOnly",
			"testRuntimeOnly",
			"testAnnotationProcessor",
			"developmentOnly",
			"providedRuntime");
	private static final List<String> TEST_DIRECTORY_CANDIDATES = List.of(
		"src/test/java",
		"src/test/kotlin",
		"src/test/resources",
		"src/integrationTest/java",
		"src/integrationTest/kotlin",
		"src/integrationTest/resources",
		"src/e2e/java",
		"src/e2e/kotlin");

	@Override
	public ProjectStructureAnalysis analyze(String projectPath) {
		Path root = resolveProjectRoot(projectPath);
		var warnings = new ArrayList<String>();
		var build = analyzeBuild(root, warnings);
		var mainPackages = collectPackages(root.resolve("src/main/java"), "main", warnings);
		var testPackages = collectPackages(root.resolve("src/test/java"), "test", warnings);
		var testStructure = analyzeTests(root, testPackages, build.dependencies());

		if (mainPackages.isEmpty()) {
			warnings.add("No Java source packages found under src/main/java");
		}

		return new ProjectStructureAnalysis(
			root.toString(),
			resolveProjectName(root),
			mainPackages,
			build,
			testStructure,
			warnings);
	}

	private Path resolveProjectRoot(String projectPath) {
		if (projectPath == null || projectPath.isBlank()) {
			throw new IllegalArgumentException("projectPath must not be blank");
		}
		try {
			Path root = Path.of(projectPath.trim()).toAbsolutePath().normalize();
			if (!Files.exists(root)) {
				throw new IllegalArgumentException("projectPath does not exist: " + root);
			}
			if (!Files.isDirectory(root)) {
				throw new IllegalArgumentException("projectPath must be a directory: " + root);
			}
			if (!Files.isReadable(root)) {
				throw new IllegalArgumentException("projectPath is not readable: " + root);
			}
			return root;
		}
		catch (InvalidPathException exception) {
			throw new IllegalArgumentException("projectPath is not a valid filesystem path", exception);
		}
	}

	private GradleBuildAnalysis analyzeBuild(Path root, List<String> warnings) {
		Path buildFile = selectBuildFile(root);
		if (buildFile == null) {
			warnings.add("No build.gradle.kts or build.gradle file found");
			return GradleBuildAnalysis.missing();
		}

		List<String> lines = readLines(buildFile);
		String content = String.join("\n", lines);
		return new GradleBuildAnalysis(
			true,
			buildFile.getFileName().toString(),
			relative(root, buildFile),
			buildFile.getFileName().toString().endsWith(".kts"),
			firstMatch(content, JAVA_VERSION_PATTERNS),
			firstMatch(content, SPRING_BOOT_PLUGIN),
			extractDependencies(lines));
	}

	private Path selectBuildFile(Path root) {
		Path kotlinBuild = root.resolve("build.gradle.kts");
		if (Files.isRegularFile(kotlinBuild)) {
			return kotlinBuild;
		}
		Path groovyBuild = root.resolve("build.gradle");
		if (Files.isRegularFile(groovyBuild)) {
			return groovyBuild;
		}
		return null;
	}

	private String resolveProjectName(Path root) {
		for (String settingsFileName : List.of("settings.gradle.kts", "settings.gradle")) {
			Path settingsFile = root.resolve(settingsFileName);
			if (Files.isRegularFile(settingsFile)) {
				String name = firstMatch(readString(settingsFile), ROOT_PROJECT_NAME);
				if (name != null && !name.isBlank()) {
					return name;
				}
			}
		}
		Path fileName = root.getFileName();
		return fileName == null ? root.toString() : fileName.toString();
	}

	private List<GradleDependency> extractDependencies(List<String> lines) {
		var dependencies = new ArrayList<GradleDependency>();
		for (String line : lines) {
			String trimmed = stripLineComment(line).trim();
			if (trimmed.isBlank()) {
				continue;
			}
			var wrapped = WRAPPED_DEPENDENCY.matcher(trimmed);
			if (wrapped.find()) {
				addDependency(dependencies, wrapped.group(1), wrapped.group(2));
				continue;
			}
			var direct = DIRECT_DEPENDENCY.matcher(trimmed);
			if (direct.find()) {
				addDependency(dependencies, direct.group(1), direct.group(2));
			}
		}
		return dependencies;
	}

	private void addDependency(List<GradleDependency> dependencies, String configuration, String notation) {
		if (DEPENDENCY_CONFIGURATIONS.contains(configuration)) {
			dependencies.add(new GradleDependency(configuration, notation));
		}
	}

	private List<JavaPackageSummary> collectPackages(Path sourceRoot, String sourceSet, List<String> warnings) {
		List<Path> javaFiles = javaFiles(sourceRoot, warnings);
		Map<String, Long> packageCounts = javaFiles.stream()
			.map(this::packageName)
			.collect(Collectors.groupingBy(packageName -> packageName, Collectors.counting()));

		return packageCounts.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> new JavaPackageSummary(sourceSet, entry.getKey(), entry.getValue()))
			.toList();
	}

	private TestStructureAnalysis analyzeTests(
			Path root,
			List<JavaPackageSummary> testPackages,
			List<GradleDependency> dependencies) {
		List<String> directories = TEST_DIRECTORY_CANDIDATES.stream()
			.map(root::resolve)
			.filter(Files::isDirectory)
			.map(path -> relative(root, path))
			.toList();

		List<Path> testJavaFiles = javaFiles(root.resolve("src/test/java"), new ArrayList<>());
		long testClassCount = testJavaFiles.stream()
			.filter(this::looksLikeTestClass)
			.count();

		return new TestStructureAnalysis(
			!directories.isEmpty(),
			directories,
			testPackages,
			testJavaFiles.size(),
			testClassCount,
			detectTestFrameworks(dependencies, testJavaFiles));
	}

	private List<String> detectTestFrameworks(List<GradleDependency> dependencies, List<Path> testJavaFiles) {
		var frameworks = new LinkedHashSet<String>();
		String dependencyText = dependencies.stream()
			.map(GradleDependency::notation)
			.collect(Collectors.joining("\n"))
			.toLowerCase(Locale.ROOT);

		if (dependencyText.contains("spring-boot-starter-test")) {
			frameworks.add("Spring Boot Test");
			frameworks.add("JUnit 5");
		}
		if (dependencyText.contains("junit-jupiter")) {
			frameworks.add("JUnit 5");
		}
		if (dependencyText.contains("junit:junit")) {
			frameworks.add("JUnit 4");
		}
		if (dependencyText.contains("assertj")) {
			frameworks.add("AssertJ");
		}
		if (dependencyText.contains("mockito")) {
			frameworks.add("Mockito");
		}
		if (dependencyText.contains("testcontainers")) {
			frameworks.add("Testcontainers");
		}

		for (Path file : testJavaFiles) {
			String content = readString(file);
			if (content.contains("org.junit.jupiter")) {
				frameworks.add("JUnit 5");
			}
			if (content.contains("org.springframework.boot.test")) {
				frameworks.add("Spring Boot Test");
			}
			if (content.contains("org.assertj")) {
				frameworks.add("AssertJ");
			}
			if (content.contains("org.mockito")) {
				frameworks.add("Mockito");
			}
		}

		return List.copyOf(frameworks);
	}

	private List<Path> javaFiles(Path sourceRoot, List<String> warnings) {
		if (!Files.isDirectory(sourceRoot)) {
			return List.of();
		}
		try (Stream<Path> paths = Files.walk(sourceRoot)) {
			List<Path> files = paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".java"))
				.sorted(Comparator.comparing(Path::toString))
				.limit(MAX_JAVA_FILES + 1L)
				.toList();
			if (files.size() > MAX_JAVA_FILES) {
				warnings.add("Java source scan reached " + MAX_JAVA_FILES + " files and was truncated");
				return files.subList(0, MAX_JAVA_FILES);
			}
			return files;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to scan Java sources under " + sourceRoot, exception);
		}
	}

	private String packageName(Path javaFile) {
		try (BufferedReader reader = Files.newBufferedReader(javaFile, StandardCharsets.UTF_8)) {
			for (int lineNumber = 0; lineNumber < PACKAGE_SCAN_LINE_LIMIT; lineNumber++) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				var matcher = PACKAGE_DECLARATION.matcher(line);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
			return DEFAULT_PACKAGE;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to read Java source file " + javaFile, exception);
		}
	}

	private boolean looksLikeTestClass(Path path) {
		String fileName = path.getFileName().toString();
		return fileName.endsWith("Test.java") || fileName.endsWith("Tests.java") || fileName.endsWith("IT.java");
	}

	private String firstMatch(String content, Pattern pattern) {
		var matcher = pattern.matcher(content);
		return matcher.find() ? matcher.group(1) : null;
	}

	private String firstMatch(String content, List<Pattern> patterns) {
		for (Pattern pattern : patterns) {
			String match = firstMatch(content, pattern);
			if (match != null) {
				return match;
			}
		}
		return null;
	}

	private List<String> readLines(Path file) {
		try {
			return Files.readAllLines(file, StandardCharsets.UTF_8);
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to read " + file.getFileName(), exception);
		}
	}

	private String readString(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to read " + file.getFileName(), exception);
		}
	}

	private String stripLineComment(String line) {
		int commentIndex = line.indexOf("//");
		return commentIndex < 0 ? line : line.substring(0, commentIndex);
	}

	private String relative(Path root, Path path) {
		return root.relativize(path).toString().replace('\\', '/');
	}

}
