/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational.internal;

import java.util.function.BooleanSupplier;

import org.hibernate.Internal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;

import static java.util.Objects.requireNonNull;

/**
 * Persistence-unit-scoped view of the registry-level {@link JdbcEnvironment}.
 *
 * @implNote Jakarta Persistence XML defaults are discovered while processing the
 * mapping sources, after the registry-level environment has been initialized.
 * This view preserves all database-specific identifier handling while applying
 * the persistence unit's {@code delimited-identifiers} default.
 */
@Internal
public final class PersistenceUnitJdbcEnvironment implements JdbcEnvironment {
	private final JdbcEnvironment delegate;
	private final IdentifierHelper identifierHelper;

	public PersistenceUnitJdbcEnvironment(
			JdbcEnvironment delegate,
			BooleanSupplier globallyQuoteIdentifiers,
			boolean globallyQuoteIdentifiersSkipColumnDefinitions) {
		this.delegate = requireNonNull( delegate );
		identifierHelper = new PersistenceUnitIdentifierHelper(
				requireNonNull( globallyQuoteIdentifiers ),
				globallyQuoteIdentifiersSkipColumnDefinitions
		);
	}

	@Override
	public Dialect getDialect() {
		return delegate.getDialect();
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return delegate.getSqlAstTranslatorFactory();
	}

	@Override
	public ExtractedDatabaseMetaData getExtractedDatabaseMetaData() {
		return delegate.getExtractedDatabaseMetaData();
	}

	@Override
	public Identifier getCurrentCatalog() {
		return identifierHelper.normalizeQuoting( delegate.getCurrentCatalog() );
	}

	@Override
	public Identifier getCurrentSchema() {
		return identifierHelper.normalizeQuoting( delegate.getCurrentSchema() );
	}

	@Override
	@SuppressWarnings("deprecation")
	public QualifiedObjectNameFormatter getQualifiedObjectNameFormatter() {
		return delegate.getQualifiedObjectNameFormatter();
	}

	@Override
	public IdentifierHelper getIdentifierHelper() {
		return identifierHelper;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return delegate.getNameQualifierSupport();
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		return delegate.getSqlExceptionHelper();
	}

	@Override
	public LobCreatorBuilder getLobCreatorBuilder() {
		return delegate.getLobCreatorBuilder();
	}

	private final class PersistenceUnitIdentifierHelper implements IdentifierHelper {
		private final BooleanSupplier globallyQuoteIdentifiers;
		private final boolean globallyQuoteIdentifiersSkipColumnDefinitions;

		private PersistenceUnitIdentifierHelper(
				BooleanSupplier globallyQuoteIdentifiers,
				boolean globallyQuoteIdentifiersSkipColumnDefinitions) {
			this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
			this.globallyQuoteIdentifiersSkipColumnDefinitions =
					globallyQuoteIdentifiersSkipColumnDefinitions;
		}

		@Override
		public Identifier normalizeQuoting(Identifier identifier) {
			return withGlobalQuoting( delegateIdentifierHelper().normalizeQuoting( identifier ) );
		}

		private Identifier withGlobalQuoting(Identifier identifier) {
			return identifier == null
				|| identifier.isQuoted()
				|| !globallyQuoteIdentifiers.getAsBoolean()
					? identifier
					: new Identifier( identifier.getText(), true, identifier.isExplicit() );
		}

		@Override
		public Identifier toIdentifier(String text) {
			return withGlobalQuoting( delegateIdentifierHelper().toIdentifier( text ) );
		}

		@Override
		public Identifier toIdentifier(String text, boolean quoted) {
			return withGlobalQuoting( delegateIdentifierHelper().toIdentifier( text, quoted ) );
		}

		@Override
		public Identifier toIdentifier(String text, boolean quoted, boolean isExplicit) {
			return withGlobalQuoting( delegateIdentifierHelper().toIdentifier( text, quoted, isExplicit ) );
		}

		@Override
		public Identifier applyGlobalQuoting(String text) {
			return globallyQuoteIdentifiers.getAsBoolean()
					? Identifier.toIdentifier(
							text,
							!globallyQuoteIdentifiersSkipColumnDefinitions,
							false
					)
					: delegateIdentifierHelper().applyGlobalQuoting( text );
		}

		@Override
		public boolean isReservedWord(String word) {
			return delegateIdentifierHelper().isReservedWord( word );
		}

		@Override
		public String toMetaDataCatalogName(Identifier catalogIdentifier) {
			return delegateIdentifierHelper().toMetaDataCatalogName(
					catalogIdentifier == null ? getCurrentCatalog() : catalogIdentifier
			);
		}

		@Override
		public String toMetaDataSchemaName(Identifier schemaIdentifier) {
			return delegateIdentifierHelper().toMetaDataSchemaName(
					schemaIdentifier == null ? getCurrentSchema() : schemaIdentifier
			);
		}

		@Override
		public String toMetaDataObjectName(Identifier identifier) {
			return delegateIdentifierHelper().toMetaDataObjectName( identifier );
		}

		private IdentifierHelper delegateIdentifierHelper() {
			return delegate.getIdentifierHelper();
		}
	}
}
