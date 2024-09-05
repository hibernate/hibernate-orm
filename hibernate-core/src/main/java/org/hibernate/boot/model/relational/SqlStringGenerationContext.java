/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;

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
	 * Generate an Identifier instance from its simple name as obtained from mapping
	 * information.
	 * <p>
	 * Note that Identifiers returned from here may be implicitly quoted based on
	 * 'globally quoted identifiers' or based on reserved words.
	 *
	 * @param text The text form of a name as obtained from mapping information.
	 *
	 * @return The identifier form of the name.
	 */
	Identifier toIdentifier(String text);

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

	/**
	 * Is the generated SQL for use in {@linkplain org.hibernate.tool.schema.spi.SchemaMigrator schema migration}?
	 *
	 * @return {@code true} if and only if this is a migration
	 */
	boolean isMigration();
}
