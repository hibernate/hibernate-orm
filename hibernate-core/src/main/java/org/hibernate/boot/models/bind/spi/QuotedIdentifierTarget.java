/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

/**
 * @author Steve Ebersole
 */
public enum QuotedIdentifierTarget {
	CATALOG_NAME,
	SCHEMA_NAME,
	TABLE_NAME,
	SEQUENCE_NAME,
	CALLABLE_NAME,
	FOREIGN_KEY,
	FOREIGN_DEFINITION,
	INDEX,
	TYPE_NAME,
	COLUMN_NAME,
	COLUMN_DEFINITION
}
