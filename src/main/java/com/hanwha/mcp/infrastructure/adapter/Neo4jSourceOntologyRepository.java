package com.hanwha.mcp.infrastructure.adapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.hanwha.mcp.bootstrap.config.Neo4jOntologyProperties;
import com.hanwha.mcp.domain.model.SourceOntologyNode;
import com.hanwha.mcp.domain.model.SourceOntologyNodeType;
import com.hanwha.mcp.domain.repository.SourceOntologyRepository;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;

public class Neo4jSourceOntologyRepository implements SourceOntologyRepository, AutoCloseable {

	private static final String SEARCH_CYPHER = """
		MATCH (n)
		WHERE n.uid IS NOT NULL
		  AND any(nodeLabel IN labels(n) WHERE nodeLabel IN $labels)
		  AND ($projectId IS NULL OR n.projectId = $projectId)
		  AND (
		    $query IS NULL
		    OR toLower(coalesce(n.name, '')) CONTAINS $query
		    OR toLower(coalesce(n.simpleName, '')) CONTAINS $query
		    OR toLower(coalesce(n.fqn, '')) CONTAINS $query
		    OR toLower(coalesce(n.filePath, '')) CONTAINS $query
		    OR toLower(coalesce(n.signature, '')) CONTAINS $query
		    OR toLower(coalesce(n.declaringType, '')) CONTAINS $query
		    OR toLower(coalesce(n.path, '')) CONTAINS $query
		    OR toLower(coalesce(n.namespace, '')) CONTAINS $query
		    OR toLower(coalesce(n.statementId, '')) CONTAINS $query
		    OR toLower(coalesce(n.sql, '')) CONTAINS $query
		  )
		WITH n, head([nodeLabel IN labels(n) WHERE nodeLabel IN $labels]) AS nodeType
		RETURN n.uid AS id,
		       nodeType,
		       coalesce(n.name, n.simpleName, n.fqn, n.statementId, n.uid) AS name,
		       n.projectId AS projectId,
		       n.filePath AS filePath,
		       n.fqn AS fullyQualifiedName,
		       n.signature AS signature,
		       n.declaringType AS declaringType,
		       n.httpMethod AS httpMethod,
		       n.path AS endpointPath,
		       n.namespace AS namespace,
		       n.statementId AS statementId,
		       n.sql AS sql,
		       n.operation AS operation
		ORDER BY nodeType, name
		LIMIT $limit
		""";

	private final Driver driver;
	private final String database;

	public Neo4jSourceOntologyRepository(Neo4jOntologyProperties properties) {
		this(
			GraphDatabase.driver(
				properties.uri(),
				AuthTokens.basic(properties.username(), properties.password() == null ? "" : properties.password())),
			properties.database());
	}

	Neo4jSourceOntologyRepository(Driver driver, String database) {
		this.driver = driver;
		this.database = database;
	}

	@Override
	public List<SourceOntologyNode> search(
			Set<SourceOntologyNodeType> nodeTypes,
			String query,
			String projectId,
			int limit) {
		var parameters = new LinkedHashMap<String, Object>();
		parameters.put("labels", nodeTypes.stream().map(SourceOntologyNodeType::neo4jLabel).toList());
		parameters.put("query", normalizeQuery(query));
		parameters.put("projectId", text(projectId));
		parameters.put("limit", limit);

		try (var session = this.driver.session(SessionConfig.forDatabase(this.database))) {
			return session.executeRead(transaction -> transaction.run(SEARCH_CYPHER, parameters)
				.list(Neo4jSourceOntologyRepository::toNode));
		}
		catch (RuntimeException exception) {
			throw new IllegalStateException("Unable to query Neo4j source ontology", exception);
		}
	}

	@Override
	public void close() {
		this.driver.close();
	}

	private static SourceOntologyNode toNode(Record record) {
		return new SourceOntologyNode(
			record.get("id").asString(),
			SourceOntologyNodeType.from(record.get("nodeType").asString()),
			string(record, "name"),
			string(record, "projectId"),
			string(record, "filePath"),
			string(record, "fullyQualifiedName"),
			string(record, "signature"),
			string(record, "declaringType"),
			string(record, "httpMethod"),
			string(record, "endpointPath"),
			string(record, "namespace"),
			string(record, "statementId"),
			string(record, "sql"),
			string(record, "operation"));
	}

	private static String string(Record record, String key) {
		var value = record.get(key);
		return value.isNull() ? "" : value.asString("");
	}

	private static String normalizeQuery(String value) {
		var normalized = text(value);
		return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
	}

	private static String text(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

}
