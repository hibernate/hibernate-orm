/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

/**
 * Enumerated values representing the level of support for catalog and schema.
 *
 * @author Steve Ebersole
 */
public enum NameQualifierSupport {
	/**
	 * Only catalog is supported
	 */
	CATALOG,
	/**
	 * Only schema is supported
	 */
	SCHEMA,
	/**
	 * Both catalog and schema are supported.
	 */
	BOTH,
	/**
	 * Neither catalog nor schema are supported.
	 */
	NONE;

	public boolean supportsCatalogs() {
		return this == CATALOG || this == BOTH;
	}

	public boolean supportsSchemas() {
		return this == SCHEMA || this == BOTH;
	}
}
