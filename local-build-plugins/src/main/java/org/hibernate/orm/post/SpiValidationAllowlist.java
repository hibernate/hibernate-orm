/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/// A versioned, exact-match set of temporary SPI validation exceptions.
///
/// @author Steve Ebersole
public final class SpiValidationAllowlist {
	public static final String SCHEMA = "hibernate-orm-spi-validation-allowlist";
	public static final int SCHEMA_VERSION = 1;

	private final List<Entry> entries;

	private SpiValidationAllowlist(List<Entry> entries) {
		this.entries = Collections.unmodifiableList( new ArrayList<>( entries ) );
	}

	public static SpiValidationAllowlist empty() {
		return new SpiValidationAllowlist( Collections.emptyList() );
	}

	/// Reads and validates the structured allowlist.
	@SuppressWarnings("unchecked")
	public static SpiValidationAllowlist read(File file) {
		final Map<String, Object> root;
		try ( InputStream stream = new FileInputStream( file ); Jsonb jsonb = JsonbBuilder.create() ) {
			root = jsonb.fromJson( stream, Map.class );
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Unable to parse SPI validation allowlist " + file, e );
		}

		require( root != null, "Allowlist root must be an object" );
		require( SCHEMA.equals( root.get( "schema" ) ), "Unexpected or missing allowlist schema" );
		final Object schemaVersion = root.get( "schemaVersion" );
		require(
				schemaVersion instanceof Number && ((Number) schemaVersion).intValue() == SCHEMA_VERSION,
				"Unexpected or missing allowlist schemaVersion"
		);
		final Object rawEntries = root.get( "entries" );
		require( rawEntries instanceof List, "Allowlist entries must be an array" );

		final List<Entry> entries = new ArrayList<>();
		final Set<String> keys = new HashSet<>();
		for ( Object rawEntry : (List<?>) rawEntries ) {
			require( rawEntry instanceof Map, "Each allowlist entry must be an object" );
			final Map<String, Object> values = (Map<String, Object>) rawEntry;
			final Entry entry = new Entry(
					requiredString( values, "rule" ),
					requiredString( values, "element" ),
					requiredString( values, "owner" ),
					requiredString( values, "reason" ),
					requiredString( values, "removalRelease" )
			);
			requireRule( entry.rule );
			require( keys.add( entry.key() ), "Duplicate allowlist entry for " + entry.key() );
			entries.add( entry );
		}
		entries.sort( (left, right) -> left.key().compareTo( right.key() ) );
		return new SpiValidationAllowlist( entries );
	}

	public List<Entry> getEntries() {
		return entries;
	}

	Entry find(SpiValidator.Rule rule, String elementId) {
		final String key = Entry.key( rule.getId(), elementId );
		for ( Entry entry : entries ) {
			if ( entry.key().equals( key ) ) {
				return entry;
			}
		}
		return null;
	}

	private static void requireRule(String ruleId) {
		try {
			SpiValidator.Rule.fromId( ruleId );
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException( "Unknown allowlist rule " + ruleId, e );
		}
	}

	private static String requiredString(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		require( value instanceof String && !((String) value).trim().isEmpty(), "Missing allowlist field " + name );
		return ((String) value).trim();
	}

	private static void require(boolean condition, String message) {
		if ( !condition ) {
			throw new IllegalArgumentException( message );
		}
	}

	/// One temporary migration exception.
	public static final class Entry {
		private final String rule;
		private final String element;
		private final String owner;
		private final String reason;
		private final String removalRelease;

		private Entry(String rule, String element, String owner, String reason, String removalRelease) {
			this.rule = rule;
			this.element = element;
			this.owner = owner;
			this.reason = reason;
			this.removalRelease = removalRelease;
		}

		public String getRule() {
			return rule;
		}

		public String getElement() {
			return element;
		}

		public String getOwner() {
			return owner;
		}

		public String getReason() {
			return reason;
		}

		public String getRemovalRelease() {
			return removalRelease;
		}

		String key() {
			return key( rule, element );
		}

		private static String key(String rule, String element) {
			return rule + '|' + element;
		}

		@Override
		public String toString() {
			return rule + " " + element + " (" + owner + ", remove by " + removalRelease + ')';
		}
	}
}
