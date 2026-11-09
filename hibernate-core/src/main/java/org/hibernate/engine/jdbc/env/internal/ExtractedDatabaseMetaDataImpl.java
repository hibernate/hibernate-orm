package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import static java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE;
import static java.util.Collections.emptyList;
import static java.util.stream.StreamSupport.stream;
import static org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy.LOWER;
import static org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy.MIXED;
import static org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy.UPPER;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.internal.util.StringHelper.splitAtCommas;
import static org.hibernate.engine.jdbc.env.spi.SQLStateType.interpretReportedSQLStateType;

/**
 * Standard implementation of {@link ExtractedDatabaseMetaData}
 *
 * @author Steve Ebersole
 */
public class ExtractedDatabaseMetaDataImpl implements ExtractedDatabaseMetaData {

	private final @Nonnull JdbcEnvironment jdbcEnvironment;
	private final @Nullable JdbcConnectionAccess connectionAccess;

	private final boolean supportsSchemas;
	private final boolean supportsCatalogs;
	private final @Nullable String connectionCatalogName;
	private final @Nullable String connectionSchemaName;
	private final @Nonnull IdentifierCaseStrategy unquotedIdentifierCaseStrategy;
	private final @Nonnull IdentifierCaseStrategy quotedIdentifierCaseStrategy;
	private final @Nonnull Set<String> sqlKeywords;

	private final @Nullable String databaseProductName;
	private final @Nullable String databaseProductVersion;

	private final boolean supportsRefCursors;
	private final boolean supportsNamedParameters;
	private final boolean supportsScrollableResults;
	private final boolean supportsGetGeneratedKeys;
	private final boolean supportsBatchUpdates;
	private final boolean supportsDataDefinitionInTransaction;
	private final boolean doesDataDefinitionCauseTransactionCommit;
	private final @Nullable SQLStateType sqlStateType;
	private final int transactionIsolation;
	private final int defaultTransactionIsolation;
	private final @Nullable String url;
	private final @Nullable String driver;
	private final int driverMajorVersion;
	private final boolean jdbcMetadataAccessible;
	private final int defaultFetchSize;

	//Lazily initialized: loading all sequence information upfront has been
	//shown to be too slow in some cases. In this way we only load it
	//when there is an actual need for these details.
	private @Nullable List<SequenceInformation> sequenceInformationList;

	ExtractedDatabaseMetaDataImpl(@Nonnull JdbcEnvironment environment) {
		jdbcEnvironment = environment;
		connectionAccess = null;
		jdbcMetadataAccessible = false;
		connectionSchemaName = null;
		connectionCatalogName = null;
		unquotedIdentifierCaseStrategy = UPPER;
		quotedIdentifierCaseStrategy = MIXED;
		sqlKeywords = Set.of();
		supportsSchemas = true;
		supportsCatalogs = true;
		databaseProductName = null;
		databaseProductVersion = null;
		supportsRefCursors = false;
		supportsNamedParameters = false;
		supportsScrollableResults = false;
		supportsGetGeneratedKeys = false;
		supportsBatchUpdates = true;
		supportsDataDefinitionInTransaction = false;
		doesDataDefinitionCauseTransactionCommit = false;
		sqlStateType = null;
		url = null;
		driver = null;
		driverMajorVersion = -1;
		defaultTransactionIsolation = 0;
		transactionIsolation = 0;
		defaultFetchSize = -1;
	}

	ExtractedDatabaseMetaDataImpl(
			@Nonnull JdbcEnvironment environment,
			@Nonnull JdbcConnectionAccess connections,
			@Nonnull DatabaseMetaData metaData)
			throws SQLException {
		jdbcEnvironment = environment;
		connectionAccess = connections;
		jdbcMetadataAccessible = true;
		final Dialect dialect = environment.getDialect();
		final Connection connection = metaData.getConnection();
		supportsSchemas = metaData.supportsSchemasInDataManipulation();
		supportsCatalogs = metaData.supportsCatalogsInDataManipulation();
		connectionSchemaName = dialect.getSchemaNameResolver().resolveSchemaName( connection, dialect );
		connectionCatalogName = connection.getCatalog();
		unquotedIdentifierCaseStrategy = unquotedIdentifierCaseStrategy( metaData );
		quotedIdentifierCaseStrategy = quotedIdentifierCaseStrategy( metaData );
		sqlKeywords = sqlKeywords( metaData.getSQLKeywords() );
		databaseProductName = metaData.getDatabaseProductName();
		databaseProductVersion = metaData.getDatabaseProductVersion();
		supportsRefCursors = reportedRefCursorSupport( metaData );
		supportsNamedParameters = reportedNamedParameterSupport( metaData );
		supportsScrollableResults = metaData.supportsResultSetType( TYPE_SCROLL_INSENSITIVE );
		supportsGetGeneratedKeys = metaData.supportsGetGeneratedKeys();
		supportsBatchUpdates = reportedBatchUpdateSupport( metaData );
		supportsDataDefinitionInTransaction = !metaData.dataDefinitionIgnoredInTransactions();
		doesDataDefinitionCauseTransactionCommit = metaData.dataDefinitionCausesTransactionCommit();
		sqlStateType = interpretReportedSQLStateType( metaData.getSQLStateType() );
		url = metaData.getURL();
		driver = driverName( metaData );
		driverMajorVersion = driverMajorVersion( metaData, driver );
		defaultTransactionIsolation = metaData.getDefaultTransactionIsolation();
		transactionIsolation = connection.getTransactionIsolation();
		defaultFetchSize = defaultFetchSize( connection );
	}

	@Override
	public boolean supportsRefCursors() {
		return supportsRefCursors;
	}

	@Override
	@Nonnull
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	public boolean supportsSchemas() {
		return supportsSchemas;
	}

	@Override
	public boolean supportsCatalogs() {
		return supportsCatalogs;
	}

	@Override
	public boolean supportsNamedParameters() {
		return supportsNamedParameters;
	}

	@Override
	public boolean supportsScrollableResults() {
		return supportsScrollableResults;
	}

	@Override
	public boolean supportsGetGeneratedKeys() {
		return supportsGetGeneratedKeys;
	}

	@Override
	public boolean supportsBatchUpdates() {
		return supportsBatchUpdates;
	}

	@Override
	public boolean supportsDataDefinitionInTransaction() {
		return supportsDataDefinitionInTransaction;
	}

	@Override
	public boolean doesDataDefinitionCauseTransactionCommit() {
		return doesDataDefinitionCauseTransactionCommit;
	}

	@Override
	@Nullable
	public SQLStateType getSqlStateType() {
		return sqlStateType;
	}

	@Override
	@Nullable
	public String getConnectionCatalogName() {
		return connectionCatalogName;
	}

	@Override
	@Nullable
	public String getConnectionSchemaName() {
		return connectionSchemaName;
	}

	@Override
	@Nonnull
	public IdentifierCaseStrategy getUnquotedIdentifierCaseStrategy() {
		return unquotedIdentifierCaseStrategy;
	}

	@Override
	@Nonnull
	public IdentifierCaseStrategy getQuotedIdentifierCaseStrategy() {
		return quotedIdentifierCaseStrategy;
	}

	@Override
	@Nonnull
	public Set<String> getSqlKeywords() {
		return sqlKeywords;
	}

	@Override
	@Nullable
	public String getDatabaseProductName() {
		return databaseProductName;
	}

	@Override
	@Nullable
	public String getDatabaseProductVersion() {
		return databaseProductVersion;
	}

	@Override
	@Nullable
	public String getUrl() {
		return url;
	}

	@Override
	@Nullable
	public String getDriver() {
		return driver;
	}

	int getDriverMajorVersion() {
		return driverMajorVersion;
	}

	@Override
	public int getTransactionIsolation() {
		return transactionIsolation;
	}

	@Override
	public int getDefaultTransactionIsolation() {
		return defaultTransactionIsolation;
	}

	@Override
	public int getDefaultFetchSize() {
		return defaultFetchSize;
	}

	@Override
	@Nonnull
	public synchronized List<SequenceInformation> getSequenceInformationList() {
		if ( connectionAccess != null ) {
			//Loading the sequence information can take a while on large databases,
			//even minutes in some cases.
			//We trigger this lazily as only certain combinations of configurations,
			//mappings and used features actually trigger any use of such details.
			if ( sequenceInformationList == null ) {
				sequenceInformationList = sequenceInformationList( connectionAccess );
			}
			return sequenceInformationList;
		}
		else {
			return emptyList();
		}
	}

	// For tests
	@Override
	public boolean isJdbcMetadataAccessible() {
		return jdbcMetadataAccessible;
	}

	private static int defaultFetchSize(@Nonnull Connection connection) {
		try ( var statement = connection.createStatement() ) {
			return statement.getFetchSize();
		}
		catch (SQLException ignore) {
			return  -1;
		}
	}

	@Nonnull
	private static IdentifierCaseStrategy unquotedIdentifierCaseStrategy(@Nonnull DatabaseMetaData metadata)
			throws SQLException {
		final boolean lower = metadata.storesLowerCaseIdentifiers();
		final boolean upper = metadata.storesUpperCaseIdentifiers();
		final boolean mixed = metadata.storesMixedCaseIdentifiers();
		final int affirmativeCount = ( lower ? 1 : 0 ) + ( upper ? 1 : 0 ) + ( mixed ? 1 : 0 );
		if ( affirmativeCount == 0 ) {
			JDBC_LOGGER.trace( "JDBC driver metadata reported database stores unquoted identifiers in neither upper, lower nor mixed case" );
			return UPPER;
		}
		if ( affirmativeCount > 1 ) {
			JDBC_LOGGER.trace( "JDBC driver metadata reported database stores unquoted identifiers in more than one case" );
		}
		return upper ? UPPER : lower ? LOWER : MIXED;
	}

	@Nonnull
	private static IdentifierCaseStrategy quotedIdentifierCaseStrategy(@Nonnull DatabaseMetaData metadata)
			throws SQLException {
		final boolean lower = metadata.storesLowerCaseQuotedIdentifiers();
		final boolean upper = metadata.storesUpperCaseQuotedIdentifiers();
		final boolean mixed = metadata.storesMixedCaseQuotedIdentifiers();
		final int affirmativeCount = ( lower ? 1 : 0 ) + ( upper ? 1 : 0 ) + ( mixed ? 1 : 0 );
		if ( affirmativeCount == 0 ) {
			JDBC_LOGGER.trace( "JDBC driver metadata reported database stores quoted identifiers in neither upper, lower nor mixed case" );
			return MIXED;
		}
		if ( affirmativeCount > 1 ) {
			JDBC_LOGGER.trace( "JDBC driver metadata reported database stores quoted identifiers in more than one case" );
		}
		return mixed ? MIXED : lower ? LOWER : UPPER;
	}

	@Nonnull
	private static Set<String> sqlKeywords(@Nullable String keywordList) {
		final var keywords = new HashSet<String>();
		for ( String keyword : splitAtCommas( keywordList ) ) {
			final String normalized = keyword.trim().toLowerCase( Locale.ROOT );
			if ( !normalized.isEmpty() ) {
				keywords.add( normalized );
			}
		}
		return Set.copyOf( keywords );
	}

	private static boolean reportedNamedParameterSupport(@Nonnull DatabaseMetaData metadata) {
		try {
			return metadata.supportsNamedParameters();
		}
		catch (Exception ignore) {
			return false;
		}
	}

	private static boolean reportedBatchUpdateSupport(@Nonnull DatabaseMetaData metadata) {
		try {
			return metadata.supportsBatchUpdates();
		}
		catch (Exception ignore) {
			return true;
		}
	}

	private static boolean reportedRefCursorSupport(@Nonnull DatabaseMetaData metadata) {
		try {
			return metadata.supportsRefCursors();
		}
		catch (Exception ignore) {
			return false;
		}
	}

	@Nullable
	private static String driverName(@Nonnull DatabaseMetaData metadata) {
		try {
			return metadata.getDriverName();
		}
		catch (Exception ignore) {
			return null;
		}
	}

	private static int driverMajorVersion(@Nonnull DatabaseMetaData metadata, @Nullable String driverName) {
		if ( !"Oracle JDBC driver".equals( driverName ) ) {
			return -1;
		}
		try {
			return metadata.getDriverMajorVersion();
		}
		catch (Exception ignore) {
			return -1;
		}
	}

	/**
	 * Get the sequence information List from the database.
	 *
	 * @return sequence information List
	 */
	@Nonnull
	private List<SequenceInformation> sequenceInformationList(@Nonnull JdbcConnectionAccess connectionAccess) {
		Connection connection = null;
		try {
			connection = connectionAccess.obtainConnection();
			return stream( sequenceInformation( connection, jdbcEnvironment ).spliterator(), false )
					.toList();
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not fetch the SequenceInformation from the database", e );
		}
		finally {
			if ( connection != null ) {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException exception) {
					JDBC_LOGGER.unableToReleaseConnection( exception );
				}
			}
		}
	}

	@Nonnull
	private static Iterable<SequenceInformation> sequenceInformation(
			@Nonnull Connection connection,
			@Nonnull JdbcEnvironment jdbcEnvironment)
			throws SQLException {
		return jdbcEnvironment.getDialect().getSequenceInformationExtractor().extractMetadata(
				new ExtractionContext.EmptyExtractionContext() {
					@Override
					public @Nonnull Connection getJdbcConnection() {
						return connection;
					}
					@Override
					public @Nonnull JdbcEnvironment getJdbcEnvironment() {
						return jdbcEnvironment;
					}
				}
		);
	}
}
