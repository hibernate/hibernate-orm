/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational.internal;

import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;

public class SqlStringGenerationContextImpl
		implements SqlStringGenerationContext {

	/**
	 * @param jdbcEnvironment The JDBC environment, to extract the dialect, identifier helper, etc.
	 * @param database The database metadata, to retrieve the implicit namespace name configured through XML mapping.
	 * @param configurationMap The configuration map, holding settings such as {@value AvailableSettings#DEFAULT_SCHEMA}.
	 * @return An {@link SqlStringGenerationContext}.
	 */
	public static SqlStringGenerationContext fromConfigurationMap(
			JdbcEnvironment jdbcEnvironment,
			Database database,
			Map<String, Object> configurationMap) {
		String defaultCatalog = (String) configurationMap.get( AvailableSettings.DEFAULT_CATALOG );
		String defaultSchema = (String) configurationMap.get( AvailableSettings.DEFAULT_SCHEMA );
		return create( jdbcEnvironment, database, defaultCatalog, defaultSchema, false );
	}

	/**
	 * @param jdbcEnvironment The JDBC environment, to extract the dialect, identifier helper, etc.
	 * @param database The database metadata, to retrieve the implicit namespace name configured through XML mapping.
	 * @param configurationMap The configuration map, holding settings such as {@value AvailableSettings#DEFAULT_SCHEMA}.
	 * @return An {@link SqlStringGenerationContext}.
	 */
	public static SqlStringGenerationContext fromConfigurationMapForMigration(
			JdbcEnvironment jdbcEnvironment,
			Database database,
			Map<String, Object> configurationMap) {
		String defaultCatalog = (String) configurationMap.get( AvailableSettings.DEFAULT_CATALOG );
		String defaultSchema = (String) configurationMap.get( AvailableSettings.DEFAULT_SCHEMA );
		return create( jdbcEnvironment, database, defaultCatalog, defaultSchema, true );
	}

	/**
	 * @param jdbcEnvironment The JDBC environment, to extract the dialect, identifier helper, etc.
	 * @param database The database metadata, to retrieve the implicit namespace name configured through XML mapping.
	 * @param defaultCatalog The default catalog to use; if {@code null}, will use the implicit catalog that was configured through XML mapping.
	 * @param defaultSchema The default schema to use; if {@code null}, will use the implicit schema that was configured through XML mapping.
	 * @return An {@link SqlStringGenerationContext}.
	 */
	public static SqlStringGenerationContext fromExplicit(
			JdbcEnvironment jdbcEnvironment,
			Database database,
			String defaultCatalog,
			String defaultSchema) {
		return create( jdbcEnvironment, database, defaultCatalog, defaultSchema, false );
	}

	private static SqlStringGenerationContext create(
			JdbcEnvironment jdbcEnvironment,
			Database database,
			String defaultCatalog,
			String defaultSchema,
			boolean forMigration) {
		final Namespace.Name implicitNamespaceName = database.getPhysicalImplicitNamespaceName();
		final IdentifierHelper identifierHelper = jdbcEnvironment.getIdentifierHelper();
		final NameQualifierSupport nameQualifierSupport = jdbcEnvironment.getNameQualifierSupport();

		Identifier actualDefaultCatalog = null;
		if ( nameQualifierSupport.supportsCatalogs() ) {
			actualDefaultCatalog = identifierHelper.toIdentifier( defaultCatalog );
			if ( actualDefaultCatalog == null ) {
				actualDefaultCatalog = implicitNamespaceName.catalog();
			}
		}

		Identifier actualDefaultSchema = null;
		if ( nameQualifierSupport.supportsSchemas() ) {
			actualDefaultSchema = identifierHelper.toIdentifier( defaultSchema );
			if ( defaultSchema == null ) {
				actualDefaultSchema = implicitNamespaceName.schema();
			}
		}

		return new SqlStringGenerationContextImpl( jdbcEnvironment, actualDefaultCatalog, actualDefaultSchema, forMigration );
	}

	public static SqlStringGenerationContext forTests(JdbcEnvironment jdbcEnvironment) {
		return forTests( jdbcEnvironment, null, null );
	}

	public static SqlStringGenerationContext forTests(JdbcEnvironment jdbcEnvironment,
			String defaultCatalog, String defaultSchema) {
		IdentifierHelper identifierHelper = jdbcEnvironment.getIdentifierHelper();
		return new SqlStringGenerationContextImpl( jdbcEnvironment,
				identifierHelper.toIdentifier( defaultCatalog ), identifierHelper.toIdentifier( defaultSchema ),
				false );
	}

	private final Dialect dialect;
	private final IdentifierHelper identifierHelper;
	private final QualifiedObjectNameFormatter qualifiedObjectNameFormatter;
	private final Identifier defaultCatalog;
	private final Identifier defaultSchema;

	private final boolean migration;

	@SuppressWarnings("deprecation")
	private SqlStringGenerationContextImpl(
			JdbcEnvironment jdbcEnvironment,
			Identifier defaultCatalog,
			Identifier defaultSchema,
			boolean migration) {
		this.dialect = jdbcEnvironment.getDialect();
		this.identifierHelper = jdbcEnvironment.getIdentifierHelper();
		this.qualifiedObjectNameFormatter = jdbcEnvironment.getQualifiedObjectNameFormatter();
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
		this.migration = migration;
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public Identifier toIdentifier(String text) {
		return identifierHelper != null ? identifierHelper.toIdentifier( text ) : Identifier.toIdentifier( text );
	}

	@Override
	public Identifier getDefaultCatalog() {
		return defaultCatalog;
	}

	@Override
	public Identifier getDefaultSchema() {
		return defaultSchema;
	}

	@Override
	public String format(QualifiedTableName qualifiedName) {
		return qualifiedObjectNameFormatter.format( withDefaults( qualifiedName ), dialect );
	}

	@Override
	public String format(QualifiedSequenceName qualifiedName) {
		return qualifiedObjectNameFormatter.format( withDefaults( qualifiedName ), dialect );
	}

	@Override
	public String format(QualifiedName qualifiedName) {
		return qualifiedObjectNameFormatter.format( withDefaults( qualifiedName ), dialect );
	}

	@Override
	public String formatWithoutCatalog(QualifiedSequenceName qualifiedName) {
		QualifiedSequenceName nameToFormat;
		if ( qualifiedName.getCatalogName() != null
				|| qualifiedName.getSchemaName() == null && defaultSchema != null ) {
			nameToFormat = new QualifiedSequenceName( null,
					schemaWithDefault( qualifiedName.getSchemaName() ), qualifiedName.getSequenceName() );
		}
		else {
			nameToFormat = qualifiedName;
		}
		return qualifiedObjectNameFormatter.format( nameToFormat, dialect );
	}

	@Override
	public boolean isMigration() {
		return migration;
	}
}
