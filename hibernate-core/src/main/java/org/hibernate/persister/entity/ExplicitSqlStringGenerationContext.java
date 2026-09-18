/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * SqlStringGenerationContext implementation with support for overriding the
 * default catalog and schema
 *
 * @author Steve Ebersole
 */
public class ExplicitSqlStringGenerationContext implements SqlStringGenerationContext {
	private final SessionFactoryImplementor factory;
	private final Identifier defaultCatalog;
	private final Identifier defaultSchema;

	public ExplicitSqlStringGenerationContext(
			@Nullable String defaultCatalog,
			@Nullable String defaultSchema,
			@Nonnull SessionFactoryImplementor factory) {
		this.factory = factory;
		this.defaultCatalog = defaultCatalog != null
				? toIdentifier( defaultCatalog )
				: toIdentifier( factory.getSessionFactoryOptions().getDefaultCatalog() );
		this.defaultSchema = defaultSchema != null
				? toIdentifier( defaultSchema )
				: toIdentifier( factory.getSessionFactoryOptions().getDefaultSchema() );
	}

	@Nonnull
	private JdbcEnvironment getJdbcEnvironment() {
		return factory.getJdbcServices().getJdbcEnvironment();
	}

	@Nonnull
	@Override
	public Dialect getDialect() {
		return factory.getJdbcServices().getDialect();
	}

	@Nonnull
	@Override
	public Identifier toIdentifier(@Nonnull String text) {
		return getJdbcEnvironment().getIdentifierHelper().toIdentifier( text );
	}

	@Nonnull
	@Override
	public Identifier getDefaultCatalog() {
		return defaultCatalog;
	}

	@Nonnull
	@Override
	public Identifier getDefaultSchema() {
		return defaultSchema;
	}

	@Nonnull
	@Override
	public String format(@Nonnull QualifiedTableName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Nonnull
	private QualifiedObjectNameFormatter nameFormater() {
		//noinspection deprecation
		return getJdbcEnvironment().getQualifiedObjectNameFormatter();
	}

	@Nonnull
	@Override
	public String format(@Nonnull QualifiedSequenceName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Nonnull
	@Override
	public String format(@Nonnull QualifiedName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Nonnull
	@Override
	public String formatWithoutCatalog(@Nonnull QualifiedSequenceName qualifiedName) {
		return nameFormater().format( nameToFormat( qualifiedName ), getDialect() );
	}

	@Nonnull
	private QualifiedSequenceName nameToFormat(@Nonnull QualifiedSequenceName qualifiedName) {
		if ( qualifiedName.getCatalogName() != null
				|| qualifiedName.getSchemaName() == null && defaultSchema != null ) {
			return new QualifiedSequenceName(
					null,
					schemaWithDefault( qualifiedName.getSchemaName() ),
					qualifiedName.getSequenceName()
			);
		}
		else {
			return qualifiedName;
		}
	}

	@Override
	public boolean isMigration() {
		return false;
	}
}
