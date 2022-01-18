/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

/**
 * A context provided to methods responsible for generating SQL strings on startup.
 */
public interface SqlStringGenerationContext {

	/**
	 * @return The database dialect of the current JDBC environment,
	 * to generate SQL fragments that are specific to each vendor.
	 */
	Dialect getDialect();

	/**
	 * @return The helper for dealing with identifiers in the current JDBC environment.
	 * <p>
	 * Note that the Identifiers returned from this helper already account for auto-quoting.
	 */
	IdentifierHelper getIdentifierHelper();

	/**
	 * @return The default catalog, used for table/sequence names that do not explicitly mention a catalog.
	 * May be {@code null}.
	 * This default is generally applied automatically by the {@link #format(QualifiedName) format methods},
	 * but in some cases it can be useful to access it directly.
	 */
	Identifier getDefaultCatalog();

	/**
	 * @param explicitCatalogOrNull An explicitly configured catalog, or {@code null}.
	 * @return The given identifier if non-{@code null}, or the default catalog otherwise.
	 */
	Identifier catalogWithDefault(Identifier explicitCatalogOrNull);

	/**
	 * @return The default schema, used for table/sequence names that do not explicitly mention a schema.
	 * May be {@code null}.
	 * This default is generally applied automatically by the {@link #format(QualifiedName) format methods},
	 * but in some cases it can be useful to access it directly.
	 */
	Identifier getDefaultSchema();

	/**
	 * @param explicitSchemaOrNull An explicitly configured schema, or {@code null}.
	 * @return The given identifier if non-{@code null}, or the default schema otherwise.
	 */
	Identifier schemaWithDefault(Identifier explicitSchemaOrNull);

	/**
	 * Render a formatted a table name
	 *
	 * @param qualifiedName The table name
	 *
	 * @return The formatted name,
	 */
	String format(QualifiedTableName qualifiedName);

	/**
	 * Render a formatted a table name, ignoring the default catalog/schema.
	 *
	 * @param qualifiedName The table name
	 *
	 * @return The formatted name
	 */
	String formatWithoutDefaults(QualifiedTableName qualifiedName);

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

	/**
	 * Render a formatted sequence name, without the catalog (even the default one).
	 *
	 * @param qualifiedName The sequence name
	 *
	 * @return The formatted name
	 */
	String formatWithoutCatalog(QualifiedSequenceName qualifiedName);

}
