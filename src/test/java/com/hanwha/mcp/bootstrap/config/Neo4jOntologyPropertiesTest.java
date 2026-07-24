package com.hanwha.mcp.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Neo4jOntologyPropertiesTest {

	@Test
	void defaultsConnectionValues() {
		var properties = new Neo4jOntologyProperties(null, " ", null, null);

		assertThat(properties.uri()).isEqualTo("bolt://localhost:7687");
		assertThat(properties.username()).isEqualTo("neo4j");
		assertThat(properties.password()).isNull();
		assertThat(properties.database()).isEqualTo("neo4j");
	}

	@Test
	void trimsConfiguredValues() {
		var properties = new Neo4jOntologyProperties(
			" bolt://neo4j.internal:7687 ",
			" app-reader ",
			" secret ",
			" sourcegraph ");

		assertThat(properties.uri()).isEqualTo("bolt://neo4j.internal:7687");
		assertThat(properties.username()).isEqualTo("app-reader");
		assertThat(properties.password()).isEqualTo(" secret ");
		assertThat(properties.database()).isEqualTo("sourcegraph");
	}

}
