/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

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
			String defaultCatalog,
			String defaultSchema,
			SessionFactoryImplementor factory) {
		this.factory = factory;
		this.defaultCatalog = defaultCatalog != null
				? toIdentifier( defaultCatalog )
				: toIdentifier( factory.getSessionFactoryOptions().getDefaultCatalog() );
		this.defaultSchema = defaultSchema != null
				? toIdentifier( defaultSchema )
				: toIdentifier( factory.getSessionFactoryOptions().getDefaultSchema() );
	}

	private JdbcEnvironment getJdbcEnvironment() {
		return factory.getJdbcServices().getJdbcEnvironment();
	}

	@Override
	public Dialect getDialect() {
		return factory.getJdbcServices().getDialect();
	}

	@Override
	public Identifier toIdentifier(String text) {
		return getJdbcEnvironment().getIdentifierHelper().toIdentifier( text );
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
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	private QualifiedObjectNameFormatter nameFormater() {
		//noinspection deprecation
		return getJdbcEnvironment().getQualifiedObjectNameFormatter();
	}

	@Override
	public String format(QualifiedSequenceName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Override
	public String format(QualifiedName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Override
	public String formatWithoutCatalog(QualifiedSequenceName qualifiedName) {
		return nameFormater().format( nameToFormat( qualifiedName ), getDialect() );
	}

	private QualifiedSequenceName nameToFormat(QualifiedSequenceName qualifiedName) {
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
