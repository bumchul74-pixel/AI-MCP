package com.hanwha.mcp.application.dto;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum MyBatisGenerationOperation {

	SELECT_BY_ID,
	SELECT_ALL,
	INSERT,
	UPDATE_BY_ID,
	DELETE_BY_ID;

	public static Set<MyBatisGenerationOperation> parse(String operations) {
		if (operations == null || operations.isBlank() || "CRUD".equalsIgnoreCase(operations.trim())) {
			return EnumSet.allOf(MyBatisGenerationOperation.class);
		}
		var selected = EnumSet.noneOf(MyBatisGenerationOperation.class);
		for (String token : operations.split(",")) {
			var normalized = token.trim().toUpperCase(Locale.ROOT);
			switch (normalized) {
				case "SELECT" -> {
					selected.add(SELECT_BY_ID);
					selected.add(SELECT_ALL);
				}
				case "UPDATE" -> selected.add(UPDATE_BY_ID);
				case "DELETE" -> selected.add(DELETE_BY_ID);
				case "SELECT_BY_ID", "SELECT_ALL", "INSERT", "UPDATE_BY_ID", "DELETE_BY_ID" ->
					selected.add(valueOf(normalized));
				default -> throw new IllegalArgumentException(
					"operations must contain only CRUD, SELECT, SELECT_BY_ID, SELECT_ALL, INSERT, UPDATE, UPDATE_BY_ID, DELETE, DELETE_BY_ID. Supported values: "
						+ supportedValues());
			}
		}
		if (selected.isEmpty()) {
			throw new IllegalArgumentException("operations must not be empty");
		}
		return selected;
	}

	private static String supportedValues() {
		return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
	}

}