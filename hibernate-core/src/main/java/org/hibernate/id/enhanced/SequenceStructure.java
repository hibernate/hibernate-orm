package org.hibernate.id.enhanced;

import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Table;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.id.IdentifierGeneratorHelper.extractLong;
import static org.hibernate.id.enhanced.ResyncHelper.getNextSequenceValue;
import static org.hibernate.id.enhanced.ResyncHelper.getMaxPrimaryKey;

/**
 * Describes a sequence.
 *
 * @author Steve Ebersole
 */
public class SequenceStructure implements DatabaseStructure {

	private final @Nonnull String contributor;
	private final @Nonnull QualifiedName logicalQualifiedSequenceName;
	private final int initialValue;
	private final int incrementSize;
	private final @Nullable String options;

	private @Nullable String sql;
	private String parameterizedSql;
	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;
	protected @Nullable QualifiedName physicalSequenceName;

	public SequenceStructure(
			@Nonnull String contributor,
			@Nonnull QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			@Nullable Class<?> numberType) {
		this.contributor = contributor;
		this.logicalQualifiedSequenceName = qualifiedSequenceName;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.options = null;
	}

	public SequenceStructure(
			@Nonnull String contributor,
			@Nonnull QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			@Nullable String options,
			@Nullable Class<?> numberType) {
		this.contributor = contributor;
		this.logicalQualifiedSequenceName = qualifiedSequenceName;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.options = options;
		}

	@Override
	public @Nonnull QualifiedName getPhysicalName() {
		if ( physicalSequenceName == null ) {
			throw new IllegalStateException( "The physical sequence name is unavailable before registerExportables()" );
		}
		return physicalSequenceName;
	}

	@Override
	public int getIncrementSize() {
		return incrementSize;
	}

	@Override
	public int getTimesAccessed() {
		return accessCounter;
	}

	@Override
	public int getInitialValue() {
		return initialValue;
	}

	@Override @Deprecated
	public @Nonnull String[] getAllSqlForTests() {
		return new String[] { sql };
	}

	@Override
	public @Nonnull AccessCallback buildCallback(final @Nonnull SharedSessionContractImplementor session) {
		if ( sql == null ) {
			throw new AssertionFailure( "SequenceStyleGenerator's SequenceStructure was not properly initialized" );
		}

		return new AccessCallback() {
			@Override
			public long getNextValue() {
				accessCounter++;
				try {
					final var jdbcCoordinator = session.getJdbcCoordinator();
					// a query statement leaves any pending JDBC batch queued
					final var statement = jdbcCoordinator.getStatementPreparer().prepareQueryStatement( sql );
					final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
					try {
						final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, sql );
						try {
							resultSet.next();
							final long value = extractLong( resultSet, 1 );
							if ( JDBC_LOGGER.isTraceEnabled() ) {
								JDBC_LOGGER.sequenceValueRetrievedFromDatabase( value );
							}
							return value;
						}
						finally {
							try {
								resourceRegistry.release( resultSet, statement );
							}
							catch (Throwable ignore) {
								// intentionally empty
							}
						}
					}
					finally {
						resourceRegistry.release( statement );
						jdbcCoordinator.afterStatementExecution();
					}

				}
				catch (SQLException sqle) {
					throw session.getJdbcServices().getSqlExceptionHelper().convert(
							sqle,
							"could not get next sequence value",
							sql
					);
				}
			}

			@Override
			public long[] getNextValues(int count) {
				accessCounter++;
				try {
					final var jdbcCoordinator = session.getJdbcCoordinator();
					final String sql;
					if ( parameterizedSql == null ) {
						final var baseSql = SequenceStructure.this.sql;
						final var unionSeparator = " union ";
						final var sb = new StringBuilder( baseSql.length() * count + unionSeparator.length() * ( count - 1 ) );
						sb.append( baseSql );
						for ( int i = 1; i < count; i++ ) {
							sb.append( unionSeparator );
							sb.append( baseSql );
						}
						sql = sb.toString();
					}
					else {
						sql = parameterizedSql;
					}
					final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
					final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
					try {
						if ( parameterizedSql != null ) {
							statement.setInt( 1, count );
						}
						final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, sql );
						final long[] values = new long[count];
						try {
							int i = 0;
							while (resultSet.next()) {
								final long value = extractLong( resultSet, 1 );
								if ( JDBC_LOGGER.isTraceEnabled() ) {
									JDBC_LOGGER.sequenceValueRetrievedFromDatabase( value );
								}
								assert i < values.length : "SQL statement returned more sequence values than were requested";
								values[i++] = value;
							}
							assert i == values.length : "SQL statement returned fewer sequence values than were requested";
							return values;
						}
						finally {
							try {
								resourceRegistry.release( resultSet, statement );
							}
							catch (Throwable ignore) {
								// intentionally empty
							}
						}
					}
					finally {
						resourceRegistry.release( statement );
						jdbcCoordinator.afterStatementExecution();
					}

				}
				catch (SQLException sqle) {
					throw session.getJdbcServices().getSqlExceptionHelper().convert(
							sqle,
							"could not get next sequence value",
							sql
					);
				}
			}

			@Override
			public @Nullable String getTenantIdentifier() {
				return session.getTenantIdentifier();
			}
		};
	}

	@Override
	public void configure(@Nonnull Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	@Override
	public void registerExportables(@Nonnull Database database) {
		buildSequence( database );
	}

	@Override
	public void initialize(@Nonnull SqlStringGenerationContext context) {
		final String sequenceName = context.format( physicalSequenceName );
		final SequenceSupport sequenceSupport = context.getDialect().getSequenceSupport();
		sql = sequenceSupport.getSequenceNextValString( sequenceName );
		parameterizedSql = sequenceSupport.supportsSelectNextValParameterized()
				? sequenceSupport.getSequenceNextValParameterizedString( sequenceName )
				: null;
	}

	@Override
	public void registerExtraExportables(@Nonnull Table table, @Nonnull Optimizer optimizer) {
		table.addResyncCommand( (sqlContext, isolator) -> {
			final String sequenceName = sqlContext.format( physicalSequenceName );
			final String tableName = sqlContext.format( table.getQualifiedTableName() );
			final String primaryKeyColumnName = table.getPrimaryKey().getColumn( 0 ).getName();
			final int adjustment = optimizer.getAdjustment();
			final long max = getMaxPrimaryKey( isolator, primaryKeyColumnName, tableName );
			final long current = getNextSequenceValue( isolator, sequenceName);
			final long startWith = Math.max( max + adjustment, current );
			optimizer.reset();
			return new InitCommand( sqlContext.getDialect().getSequenceSupport()
					.getRestartSequenceString( sequenceName, startWith ) );
		} );
		table.addResetCommand( sqlContext -> {
			optimizer.reset();
			final String sequenceName = sqlContext.format( physicalSequenceName );
			return new InitCommand( sqlContext.getDialect().getSequenceSupport()
					.getRestartSequenceString( sequenceName, initialValue ) );
		} );
	}

	@Override
	public boolean isPhysicalSequence() {
		return true;
	}

	protected final int getSourceIncrementSize() {
		return applyIncrementSizeToSourceValues ? incrementSize : 1;
	}

	protected @Nonnull QualifiedName getQualifiedName() {
		return logicalQualifiedSequenceName;
	}

	protected void buildSequence(@Nonnull Database database) {
		final var sequence =
				locateOrCreateSequence( database.locateNamespace(
						logicalQualifiedSequenceName.getCatalogName(),
						logicalQualifiedSequenceName.getSchemaName()
				) );
		physicalSequenceName = sequence.getName();
	}

	private @Nonnull Sequence locateOrCreateSequence(@Nonnull Namespace namespace) {
		final int sourceIncrementSize = getSourceIncrementSize();
		final var objectName = logicalQualifiedSequenceName.getObjectName();
		final var existingSequence = namespace.locateSequence( objectName );
		if ( existingSequence != null ) {
			existingSequence.validate( initialValue, sourceIncrementSize );
			return existingSequence;
		}
		else {
			return namespace.createSequence(
					objectName,
					physicalName -> new Sequence(
							contributor,
							namespace.getPhysicalName().catalog(),
							namespace.getPhysicalName().schema(),
							physicalName,
							initialValue,
							sourceIncrementSize,
							options
					)
			);
		}
	}
}
