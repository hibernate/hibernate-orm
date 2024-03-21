/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
