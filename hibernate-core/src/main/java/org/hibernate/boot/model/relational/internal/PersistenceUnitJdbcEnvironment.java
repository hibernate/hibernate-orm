package org.hibernate.boot.model.relational.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;

import java.util.function.BooleanSupplier;

import org.hibernate.Internal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;

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
	private final @Nonnull JdbcEnvironment delegate;
	private final @Nonnull IdentifierHelper identifierHelper;

	public PersistenceUnitJdbcEnvironment(
			@Nonnull JdbcEnvironment delegate,
			@Nonnull BooleanSupplier globallyQuoteIdentifiers,
			boolean globallyQuoteIdentifiersSkipColumnDefinitions) {
		this.delegate = requireNonNull( delegate );
		identifierHelper = new PersistenceUnitIdentifierHelper(
				requireNonNull( globallyQuoteIdentifiers ),
				globallyQuoteIdentifiersSkipColumnDefinitions
		);
	}

	@Override
	@Nonnull
	public Dialect getDialect() {
		return delegate.getDialect();
	}

	@Override
	@Nonnull
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return delegate.getSqlAstTranslatorFactory();
	}

	@Override
	@Nonnull
	public JdbcMetadata getJdbcMetadata() {
		return delegate.getJdbcMetadata();
	}

	@Override
	@Nonnull
	public TransactionConcurrency getTransactionConcurrency() {
		return delegate.getTransactionConcurrency();
	}

	@Override
	@Nonnull
	public ExtractedDatabaseMetaData getExtractedDatabaseMetaData() {
		return delegate.getExtractedDatabaseMetaData();
	}

	@Override
	@Nullable
	public Identifier getCurrentCatalog() {
		return identifierHelper.normalizeQuoting( delegate.getCurrentCatalog() );
	}

	@Override
	@Nullable
	public Identifier getCurrentSchema() {
		return identifierHelper.normalizeQuoting( delegate.getCurrentSchema() );
	}

	@Override
	@SuppressWarnings("deprecation")
	@Nonnull
	public QualifiedObjectNameFormatter getQualifiedObjectNameFormatter() {
		return delegate.getQualifiedObjectNameFormatter();
	}

	@Override
	@Nonnull
	public IdentifierHelper getIdentifierHelper() {
		return identifierHelper;
	}

	@Override
	@Nonnull
	public NameQualifierSupport getNameQualifierSupport() {
		return delegate.getNameQualifierSupport();
	}

	@Override
	@Nonnull
	public SqlExceptionHelper getSqlExceptionHelper() {
		return delegate.getSqlExceptionHelper();
	}

	@Override
	@Nonnull
	public LobCreatorBuilder getLobCreatorBuilder() {
		return delegate.getLobCreatorBuilder();
	}

	private final class PersistenceUnitIdentifierHelper implements IdentifierHelper {
		private final @Nonnull BooleanSupplier globallyQuoteIdentifiers;
		private final boolean globallyQuoteIdentifiersSkipColumnDefinitions;

		private PersistenceUnitIdentifierHelper(
				@Nonnull BooleanSupplier globallyQuoteIdentifiers,
				boolean globallyQuoteIdentifiersSkipColumnDefinitions) {
			this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
			this.globallyQuoteIdentifiersSkipColumnDefinitions =
					globallyQuoteIdentifiersSkipColumnDefinitions;
		}

		@Override
		@Nullable
		public Identifier normalizeQuoting(@Nullable Identifier identifier) {
			return withGlobalQuoting( delegateIdentifierHelper().normalizeQuoting( identifier ) );
		}

		@Nullable
		private Identifier withGlobalQuoting(@Nullable Identifier identifier) {
			return identifier == null
				|| identifier.isQuoted()
				|| !globallyQuoteIdentifiers.getAsBoolean()
					? identifier
					: new Identifier( identifier.getText(), true, identifier.isExplicit() );
		}

		@Override
		@Nullable
		public Identifier toIdentifier(@Nullable String text) {
			return withGlobalQuoting( delegateIdentifierHelper().toIdentifier( text ) );
		}

		@Override
		@Nullable
		public Identifier toIdentifier(@Nullable String text, boolean quoted) {
			return withGlobalQuoting( delegateIdentifierHelper().toIdentifier( text, quoted ) );
		}

		@Override
		@Nullable
		public Identifier toIdentifier(@Nullable String text, boolean quoted, boolean isExplicit) {
			return withGlobalQuoting( delegateIdentifierHelper().toIdentifier( text, quoted, isExplicit ) );
		}

		@Override
		@Nullable
		public Identifier applyGlobalQuoting(@Nullable String text) {
			return globallyQuoteIdentifiers.getAsBoolean()
					? Identifier.toIdentifier(
							text,
							!globallyQuoteIdentifiersSkipColumnDefinitions,
							false
					)
					: delegateIdentifierHelper().applyGlobalQuoting( text );
		}

		@Override
		public boolean isReservedWord(@Nonnull String word) {
			return delegateIdentifierHelper().isReservedWord( word );
		}

		@Override
		@Nullable
		public String toMetaDataCatalogName(@Nullable Identifier catalogIdentifier) {
			return delegateIdentifierHelper().toMetaDataCatalogName(
					catalogIdentifier == null ? getCurrentCatalog() : catalogIdentifier
			);
		}

		@Override
		@Nullable
		public String toMetaDataSchemaName(@Nullable Identifier schemaIdentifier) {
			return delegateIdentifierHelper().toMetaDataSchemaName(
					schemaIdentifier == null ? getCurrentSchema() : schemaIdentifier
			);
		}

		@Override
		@Nonnull
		public String toMetaDataObjectName(@Nonnull Identifier identifier) {
			return delegateIdentifierHelper().toMetaDataObjectName( identifier );
		}

		@Nonnull
		private IdentifierHelper delegateIdentifierHelper() {
			return delegate.getIdentifierHelper();
		}
	}
}
