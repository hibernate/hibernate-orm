/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/// Minimal provider-facing view of Hibernate's classification metadata.
///
/// @author Steve Ebersole
public final class ClassificationMetadata {
	public static final String SCHEMA = "hibernate-orm-classifications";
	public static final int SCHEMA_VERSION = 1;

	private final String family;
	private final String sourceVersion;
	private final Map<String, Element> elements;

	public ClassificationMetadata(String family, String sourceVersion, Map<String, Element> elements) {
		this.family = family;
		this.sourceVersion = sourceVersion;
		this.elements = Collections.unmodifiableMap( new LinkedHashMap<>( elements ) );
	}

	public String family() {
		return family;
	}

	public String sourceVersion() {
		return sourceVersion;
	}

	public Map<String, Element> elements() {
		return elements;
	}

	public Element element(String id) {
		return elements.get( id );
	}

	/// Classification facts needed by provider-boundary validation.
	public record Element(String id, String category, Set<String> roles, String artifact) {
		public Element {
			roles = Collections.unmodifiableSet( new LinkedHashSet<>( roles ) );
		}

		public boolean internal() {
			return "INTERNAL".equals( category );
		}

		public boolean api() {
			return "API".equals( category );
		}

		public boolean implementableSpi() {
			return "SPI".equals( category ) && roles.contains( "IMPLEMENT" );
		}
	}
}
