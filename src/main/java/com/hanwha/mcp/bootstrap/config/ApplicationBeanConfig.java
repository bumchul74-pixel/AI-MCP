package com.hanwha.mcp.bootstrap.config;

import java.time.Clock;

import com.hanwha.mcp.application.dto.MyBatisGeneratorDefaults;
import com.hanwha.mcp.application.service.AnalyzeProjectStructureService;
import com.hanwha.mcp.application.service.DescribeServerService;
import com.hanwha.mcp.application.service.GenerateMyBatisMapperService;
import com.hanwha.mcp.application.usecase.AnalyzeProjectStructureUseCase;
import com.hanwha.mcp.application.usecase.DescribeServerUseCase;
import com.hanwha.mcp.application.usecase.GenerateMyBatisMapperUseCase;
import com.hanwha.mcp.domain.model.ServerMetadata;
import com.hanwha.mcp.domain.repository.DatabaseSchemaInspector;
import com.hanwha.mcp.domain.repository.ProjectStructureAnalyzer;
import com.hanwha.mcp.domain.repository.ServerMetadataRepository;
import com.hanwha.mcp.infrastructure.adapter.ConfiguredServerMetadataRepository;
import com.hanwha.mcp.infrastructure.adapter.FileSystemProjectStructureAnalyzer;
import com.hanwha.mcp.infrastructure.adapter.JdbcDatabaseSchemaInspector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeanConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	ServerMetadataRepository serverMetadataRepository(ServerMetadataProperties properties) {
		var metadata = new ServerMetadata(properties.name(), properties.version(), properties.description());
		return new ConfiguredServerMetadataRepository(metadata);
	}

	@Bean
	DescribeServerUseCase describeServerUseCase(ServerMetadataRepository repository, Clock clock) {
		return new DescribeServerService(repository, clock);
	}

	@Bean
	ProjectStructureAnalyzer projectStructureAnalyzer() {
		return new FileSystemProjectStructureAnalyzer();
	}

	@Bean
	AnalyzeProjectStructureUseCase analyzeProjectStructureUseCase(ProjectStructureAnalyzer analyzer) {
		return new AnalyzeProjectStructureService(analyzer);
	}

	@Bean
	DatabaseSchemaInspector databaseSchemaInspector(MyBatisDataSourceProperties properties) {
		return new JdbcDatabaseSchemaInspector(properties);
	}

	@Bean
	MyBatisGeneratorDefaults myBatisGeneratorDefaults(MyBatisGeneratorProperties properties) {
		return new MyBatisGeneratorDefaults(properties.basePackage(), properties.defaultSchema());
	}

	@Bean
	GenerateMyBatisMapperUseCase generateMyBatisMapperUseCase(
			DatabaseSchemaInspector databaseSchemaInspector,
			MyBatisGeneratorDefaults defaults) {
		return new GenerateMyBatisMapperService(databaseSchemaInspector, defaults);
	}

}