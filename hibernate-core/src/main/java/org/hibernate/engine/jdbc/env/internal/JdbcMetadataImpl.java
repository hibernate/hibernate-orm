package org.hibernate.engine.jdbc.env.internal;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

/// Standard effective JDBC metadata view.
///
/// @author Steve Ebersole
public final class JdbcMetadataImpl implements JdbcMetadata {
	private final @Nonnull ExtractedDatabaseMetaData extractedMetadata;
	private final boolean supportsNamedParameters;
	private final boolean supportsRefCursors;
	private final boolean supportsBatchUpdates;

	JdbcMetadataImpl(
			@Nonnull ExtractedDatabaseMetaDataImpl extractedMetadata,
			@Nonnull JdbcMetadataOverrides overrides) {
		this.extractedMetadata = extractedMetadata;
		supportsNamedParameters = overrides.getNamedParameterSupport()
				.resolve( extractedMetadata.supportsNamedParameters() );
		supportsRefCursors = overrides.getStandardRefCursorSupport()
				.resolve( interpretedRefCursorSupport( extractedMetadata ) );
		supportsBatchUpdates = overrides.getBatchUpdateSupport()
				.resolve( extractedMetadata.supportsBatchUpdates() );
	}

	private static boolean interpretedRefCursorSupport(@Nonnull ExtractedDatabaseMetaDataImpl extractedMetadata) {
		if ( !extractedMetadata.supportsRefCursors() ) {
			return false;
		}
		final String driver = extractedMetadata.getDriver();
		if ( driver == null ) {
			return false;
		}
		if ( "Oracle JDBC driver".equals( driver ) ) {
			final int driverMajorVersion = extractedMetadata.getDriverMajorVersion();
			return driverMajorVersion >= 19;
		}
		return true;
	}

	@Override
	@Nonnull
	public JdbcEnvironment getJdbcEnvironment() {
		return extractedMetadata.getJdbcEnvironment();
	}

	@Override
	public boolean isJdbcMetadataAccessible() {
		return extractedMetadata.isJdbcMetadataAccessible();
	}

	@Override
	@Nonnull
	public ExtractedDatabaseMetaData getExtractedDatabaseMetaData() {
		return extractedMetadata;
	}

	@Override
	@Nullable
	public String getDatabaseProductName() {
		return extractedMetadata.getDatabaseProductName();
	}

	@Override
	@Nullable
	public String getDatabaseProductVersion() {
		return extractedMetadata.getDatabaseProductVersion();
	}

	@Override
	public boolean supportsSchemas() {
		return extractedMetadata.supportsSchemas();
	}

	@Override
	public boolean supportsCatalogs() {
		return extractedMetadata.supportsCatalogs();
	}

	@Override
	@Nullable
	public String getConnectionCatalogName() {
		return extractedMetadata.getConnectionCatalogName();
	}

	@Override
	@Nullable
	public String getConnectionSchemaName() {
		return extractedMetadata.getConnectionSchemaName();
	}

	@Override
	@Nonnull
	public IdentifierCaseStrategy getUnquotedIdentifierCaseStrategy() {
		return extractedMetadata.getUnquotedIdentifierCaseStrategy();
	}

	@Override
	@Nonnull
	public IdentifierCaseStrategy getQuotedIdentifierCaseStrategy() {
		return extractedMetadata.getQuotedIdentifierCaseStrategy();
	}

	@Override
	@Nonnull
	public Set<String> getSqlKeywords() {
		return extractedMetadata.getSqlKeywords();
	}

	@Override
	public boolean supportsNamedParameters() {
		return supportsNamedParameters;
	}

	@Override
	public boolean supportsRefCursors() {
		return supportsRefCursors;
	}

	@Override
	public boolean supportsScrollableResults() {
		return extractedMetadata.supportsScrollableResults();
	}

	@Override
	public boolean supportsGetGeneratedKeys() {
		return extractedMetadata.supportsGetGeneratedKeys();
	}

	@Override
	public boolean supportsBatchUpdates() {
		return supportsBatchUpdates;
	}

	@Override
	public boolean supportsDataDefinitionInTransaction() {
		return extractedMetadata.supportsDataDefinitionInTransaction();
	}

	@Override
	public boolean doesDataDefinitionCauseTransactionCommit() {
		return extractedMetadata.doesDataDefinitionCauseTransactionCommit();
	}

	@Override
	@Nullable
	public SQLStateType getSqlStateType() {
		return extractedMetadata.getSqlStateType();
	}

	@Override
	@Nullable
	public String getUrl() {
		return extractedMetadata.getUrl();
	}

	@Override
	@Nullable
	public String getDriver() {
		return extractedMetadata.getDriver();
	}

	@Override
	public int getTransactionIsolation() {
		return extractedMetadata.getTransactionIsolation();
	}

	@Override
	public int getDefaultTransactionIsolation() {
		return extractedMetadata.getDefaultTransactionIsolation();
	}

	@Override
	public int getDefaultFetchSize() {
		return extractedMetadata.getDefaultFetchSize();
	}

	@Override
	@Nonnull
	public List<SequenceInformation> getSequenceInformationList() {
		return extractedMetadata.getSequenceInformationList();
	}
}
