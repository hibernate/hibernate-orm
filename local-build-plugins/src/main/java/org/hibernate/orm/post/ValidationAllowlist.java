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

/// A versioned, exact-match set of temporary classification and SPI
/// validation exceptions.
///
/// @author Steve Ebersole
public final class ValidationAllowlist {
	public static final String SCHEMA = "hibernate-orm-classification-validation-allowlist";
	public static final int SCHEMA_VERSION = 2;

	private final List<Entry> entries;

	private ValidationAllowlist(List<Entry> entries) {
		this.entries = Collections.unmodifiableList( new ArrayList<>( entries ) );
	}

	public static ValidationAllowlist empty() {
		return new ValidationAllowlist( Collections.emptyList() );
	}

	/// Reads and validates the structured migration allowlist.
	@SuppressWarnings("unchecked")
	public static ValidationAllowlist read(File file) {
		final Map<String, Object> root;
		try ( InputStream stream = new FileInputStream( file ); Jsonb jsonb = JsonbBuilder.create() ) {
			root = jsonb.fromJson( stream, Map.class );
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Unable to parse validation allowlist " + file, e );
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
					requiredCause( values, "cause" ),
					requiredString( values, "element" ),
					requiredString( values, "target" ),
					requiredString( values, "edgeKind" ),
					requiredString( values, "owner" ),
					requiredString( values, "reason" ),
					requiredString( values, "removalRelease" )
			);
			require( keys.add( entry.key() ), "Duplicate allowlist entry for " + entry.key() );
			entries.add( entry );
		}
		entries.sort( (left, right) -> left.key().compareTo( right.key() ) );
		return new ValidationAllowlist( entries );
	}

	public List<Entry> getEntries() {
		return entries;
	}

	Entry find(ValidationDiagnostic diagnostic) {
		final String key = Entry.key(
				diagnostic.getCause(),
				diagnostic.getSourceElementId(),
				diagnostic.getTargetElementId(),
				diagnostic.getEdgeKind()
		);
		for ( Entry entry : entries ) {
			if ( entry.key().equals( key ) ) {
				return entry;
			}
		}
		return null;
	}

	private static ValidationCause requiredCause(Map<String, Object> values, String name) {
		final String cause = requiredString( values, name );
		try {
			return ValidationCause.valueOf( cause );
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException( "Unknown allowlist cause " + cause, e );
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

	/// One temporary migration exception. Cause, source element, target element,
	/// and edge kind form its exact diagnostic identity.
	public static final class Entry {
		private final ValidationCause cause;
		private final String element;
		private final String target;
		private final String edgeKind;
		private final String owner;
		private final String reason;
		private final String removalRelease;

		private Entry(
				ValidationCause cause,
				String element,
				String target,
				String edgeKind,
				String owner,
				String reason,
				String removalRelease) {
			this.cause = cause;
			this.element = element;
			this.target = target;
			this.edgeKind = edgeKind;
			this.owner = owner;
			this.reason = reason;
			this.removalRelease = removalRelease;
		}

		public ValidationCause getCause() {
			return cause;
		}

		public String getElement() {
			return element;
		}

		public String getTarget() {
			return target;
		}

		public String getEdgeKind() {
			return edgeKind;
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
			return key( cause, element, target, edgeKind );
		}

		private static String key(ValidationCause cause, String element, String target, String edgeKind) {
			return cause + "|" + element + "|" + target + "|" + edgeKind;
		}

		@Override
		public String toString() {
			return key() + " (" + owner + ", remove by " + removalRelease + ')';
		}
	}
}
