/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.dialect.Dialect;

/**
 * A context provided to methods responsible for generating SQL strings on startup.
 */
public interface SqlStringGenerationContext {

	/**
	 * @return The database dialect, to generate SQL fragments that are specific to each vendor.
	 */
	Dialect getDialect();

	/**
	 * Render a formatted a table name
	 *
	 * @param qualifiedName The table name
	 *
	 * @return The formatted name,
	 */
	String format(QualifiedTableName qualifiedName);

	/**
	 * Render a formatted sequence name
	 *
	 * @param qualifiedName The sequence name
	 *
	 * @return The formatted name
	 */
	String format(QualifiedSequenceName qualifiedName);

	/**
	 * Render a formatted non-table and non-sequence qualified name
	 *
	 * @param qualifiedName The name
	 *
	 * @return The formatted name
	 */
	String format(QualifiedName qualifiedName);

}
