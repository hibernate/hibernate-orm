/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * Interpret the incoming catalog, returning the incoming value if it is non-null.
	 * Otherwise, returns the current {@linkplain #getDefaultCatalog() default catalog}.
	 *
	 * @apiNote May return {@code null} if {@linkplain #getDefaultCatalog() default catalog} is {@code null}.
	 */
	default Identifier catalogWithDefault(Identifier explicitCatalogOrNull) {
		return explicitCatalogOrNull != null ? explicitCatalogOrNull : getDefaultCatalog();
	}

	/**
	 * @return The default schema, used for table/sequence names that do not explicitly mention a schema.
	 * May be {@code null}.
	 * This default is generally applied automatically by the {@link #format(QualifiedName) format methods},
	 * but in some cases it can be useful to access it directly.
	 */
	Identifier getDefaultSchema();

	/**
	 * Interpret the incoming schema, returning the incoming value if it is non-null.
	 * Otherwise, returns the current {@linkplain #getDefaultSchema() default schema}.
	 *
	 * @apiNote May return {@code null} if {@linkplain #getDefaultSchema() default schema} is {@code null}.
	 */
	default Identifier schemaWithDefault(Identifier explicitSchemaOrNull) {
		return explicitSchemaOrNull != null ? explicitSchemaOrNull : getDefaultSchema();
	}

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

	/**
	 * Apply default catalog and schema, if necessary, to the given name.  May return a new reference.
	 */
	default QualifiedTableName withDefaults(QualifiedTableName name) {
		if ( name.getCatalogName() == null && getDefaultCatalog() != null
				|| name.getSchemaName() == null && getDefaultSchema() != null ) {
			return new QualifiedTableName(
					catalogWithDefault( name.getCatalogName() ),
					schemaWithDefault( name.getSchemaName() ),
					name.getTableName()
			);
		}
		return name;
	}

	/**
	 * Apply default catalog and schema, if necessary, to the given name.  May return a new reference.
	 */
	default QualifiedSequenceName withDefaults(QualifiedSequenceName name) {
		if ( name.getCatalogName() == null && getDefaultCatalog() != null
				|| name.getSchemaName() == null && getDefaultSchema() != null ) {
			return new QualifiedSequenceName(
					catalogWithDefault( name.getCatalogName() ),
					schemaWithDefault( name.getSchemaName() ),
					name.getSequenceName()
			);
		}
		return name;
	}

	/**
	 * Apply default catalog and schema, if necessary, to the given name.  May return a new reference.
	 */
	default QualifiedName withDefaults(QualifiedName name) {
		if ( name.getCatalogName() == null && getDefaultCatalog() != null
				|| name.getSchemaName() == null && getDefaultSchema() != null ) {
			return new QualifiedNameImpl(
					catalogWithDefault( name.getCatalogName() ),
					schemaWithDefault( name.getSchemaName() ),
					name.getObjectName()
			);
		}
		return name;
	}
}
