package com.hanwha.mcp.infrastructure.adapter;

import java.util.Objects;

import com.hanwha.mcp.domain.model.ServerMetadata;
import com.hanwha.mcp.domain.repository.ServerMetadataRepository;

public class ConfiguredServerMetadataRepository implements ServerMetadataRepository {

	private final ServerMetadata metadata;

	public ConfiguredServerMetadataRepository(ServerMetadata metadata) {
		this.metadata = Objects.requireNonNull(metadata);
	}

	@Override
	public ServerMetadata getServerMetadata() {
		return this.metadata;
	}

}