/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.naming;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.junit.platform.commons.util.StringUtils;

/**
 * An example PhysicalNamingStrategy that implements database object naming standards
 * for our fictitious company Acme Corp.
 * <p>
 * In general Acme Corp prefers underscore-delimited words rather than camel casing.
 * <p>
 * Additionally standards call for the replacement of certain words with abbreviations.
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class AcmeCorpPhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	private static final Map<String, String> ABBREVIATIONS;

	static {
		ABBREVIATIONS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		ABBREVIATIONS.put("account", "acct");
		ABBREVIATIONS.put("number", "num");
	}

	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		final List<String> parts = splitAndReplace( logicalName.getText());
		return jdbcEnvironment.getIdentifierHelper().toIdentifier(
				String.join("_", parts),
				logicalName.isQuoted()
		);
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		final List<String> parts = splitAndReplace( logicalName.getText());
		// Acme Corp says all sequences should end with _seq
		if (!"seq".equals(parts.get(parts.size() - 1))) {
			parts.add("seq");
		}
		return jdbcEnvironment.getIdentifierHelper().toIdentifier(
				String.join("_", parts),
				logicalName.isQuoted()
		);
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		final List<String> parts = splitAndReplace( logicalName.getText());
		return jdbcEnvironment.getIdentifierHelper().toIdentifier(
				String.join("_", parts),
				logicalName.isQuoted()
		);
	}

	private List<String> splitAndReplace(String name) {
		return Arrays.stream(splitByCharacterTypeCamelCase(name))
				.filter(StringUtils::isNotBlank)
				.map(p -> ABBREVIATIONS.getOrDefault(p, p).toLowerCase(Locale.ROOT))
				.collect(Collectors.toList());
	}

	private String[] splitByCharacterTypeCamelCase(String s) {
		return s.split( "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])" );
	}
}
