/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
