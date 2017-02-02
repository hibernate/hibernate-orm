/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

/**
 * An enumeration of the way DatabaseMetaData might store and return identifiers
 *
 * @author Steve Ebersole
 */
public enum IdentifierCaseStrategy {
	/**
	 * The identifier is stored in mixed case.
	 *
	 * @see java.sql.DatabaseMetaData#storesMixedCaseIdentifiers()
	 * @see java.sql.DatabaseMetaData#storesMixedCaseQuotedIdentifiers()
	 */
	MIXED,
	/**
	 * The identifier is stored in upper case.
	 *
	 * @see java.sql.DatabaseMetaData#storesUpperCaseIdentifiers()
	 * @see java.sql.DatabaseMetaData#storesUpperCaseQuotedIdentifiers()
	 */
	UPPER,
	/**
	 * The identifier is stored in lower case.
	 *
	 * @see java.sql.DatabaseMetaData#storesLowerCaseIdentifiers()
	 * @see java.sql.DatabaseMetaData#storesLowerCaseQuotedIdentifiers()
	 */
	LOWER
}
