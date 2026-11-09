package org.hibernate.id.enhanced;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.internal.TableLockHintRendererSupport;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.mapping.Table;
import org.hibernate.type.StandardBasicTypes;

import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.id.IdentifierGeneratorHelper.bindLong;
import static org.hibernate.id.IdentifierGeneratorHelper.extractLong;
import static org.hibernate.id.enhanced.ResyncHelper.getCurrentTableValue;
import static org.hibernate.id.enhanced.ResyncHelper.getMaxPrimaryKey;
import static org.hibernate.id.enhanced.TableGeneratorLogger.TABLE_GENERATOR_LOGGER;

/**
 * Describes a table used to mimic sequence behavior
 *
 * @author Steve Ebersole
 */
public class TableStructure implements DatabaseStructure {

	private final @Nonnull QualifiedName logicalQualifiedTableName;
	private final @Nonnull Identifier logicalValueColumnNameIdentifier;
	private final int initialValue;
	private final int incrementSize;
	private final @Nullable String options;

	private final @Nonnull String contributor;

	private @Nullable QualifiedName physicalTableName;
	private @Nullable String valueColumnNameText;

	private @Nullable String selectQuery;
	private @Nullable String updateQuery;

	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;


	public TableStructure(
			@Nonnull String contributor,
			@Nonnull QualifiedName qualifiedTableName,
			@Nonnull Identifier valueColumnNameIdentifier,
			int initialValue,
			int incrementSize,
			@Nullable Class<?> numberType) {
		this(
				contributor,
				qualifiedTableName,
				valueColumnNameIdentifier,
				initialValue,
				incrementSize,
				null,
				numberType
		);
	}

	public TableStructure(
			@Nonnull String contributor,
			@Nonnull QualifiedName qualifiedTableName,
			@Nonnull Identifier valueColumnNameIdentifier,
			int initialValue,
			int incrementSize,
			@Nullable String options,
			@Nullable Class<?> numberType) {
		this.contributor = contributor;
		this.logicalQualifiedTableName = qualifiedTableName;
		this.logicalValueColumnNameIdentifier = valueColumnNameIdentifier;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.options = options;
	}

	@Override
	public @Nonnull QualifiedName getPhysicalName() {
		if ( physicalTableName == null ) {
			throw new IllegalStateException( "The physical table name is unavailable before registerExportables()" );
		}
		return physicalTableName;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	public @Nonnull Identifier getLogicalValueColumnNameIdentifier() {
		return logicalValueColumnNameIdentifier;
	}

	@Override
	public int getInitialValue() {
		return initialValue;
	}

	@Override
	public int getIncrementSize() {
		return incrementSize;
	}

	@Override
	public int getTimesAccessed() {
		return accessCounter;
	}

	@Override @Deprecated
	public @Nonnull String[] getAllSqlForTests() {
		return new String[] { selectQuery, updateQuery };
	}

	@Override @Deprecated
	public void prepare(@Nonnull Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	@Override
	public @Nonnull AccessCallback buildCallback(final @Nonnull SharedSessionContractImplementor session) {

		final var statsCollector = session.getEventListenerManager();
		final var statementLogger =
				session.getFactory().getJdbcServices()
						.getSqlStatementLogger();

		return new AccessCallback() {
			@Override
			public long getNextValue() {
				return session.getTransactionCoordinator().createIsolationDelegate().delegateWork(
						new AbstractReturningWork<>() {
							@Override
							public @Nonnull Long execute(@Nonnull Connection connection) throws SQLException {
								if ( selectQuery == null || updateQuery == null || physicalTableName == null ) {
									throw new IllegalStateException( "The TableStructure was not properly initialized" );
								}
								long value;
								int rows;
								do {
									try ( var selectStatement = prepareStatement(
											connection,
											selectQuery,
											statementLogger,
											statsCollector,
											session
									) ) {
										try ( var resultSet = executeQuery(
												selectStatement,
												statsCollector,
												selectQuery,
												session
										) ) {
											if ( !resultSet.next() ) {
												throw new IdentifierGenerationException(
														"Could not read a hi value, populate the table: "
																+ physicalTableName );
											}
											value = extractLong( resultSet, 1 );
										}
									}
									catch (SQLException sqle) {
										TABLE_GENERATOR_LOGGER.unableToReadHiValue( physicalTableName.render(), sqle );
										throw sqle;
									}

									try ( var updateStatement = prepareStatement(
											connection,
											updateQuery,
											statementLogger,
											statsCollector,
											session
									) ) {
										final int increment = applyIncrementSizeToSourceValues ? incrementSize : 1;
										bindLong( updateStatement, 1, value + increment );
										bindLong( updateStatement, 2, value );
										rows = executeUpdate( updateStatement, statsCollector, updateQuery, session );
									}
									catch (SQLException e) {
										TABLE_GENERATOR_LOGGER.unableToUpdateHiValue( physicalTableName.render(), e );
										throw e;
									}
								} while ( rows == 0 );

								accessCounter++;

								return value;
							}
						},
						true
				);
			}

			@Override
			public @Nullable String getTenantIdentifier() {
				return session.getTenantIdentifier();
			}
		};
	}

	private @Nonnull PreparedStatement prepareStatement(
			@Nonnull Connection connection,
			@Nonnull String sql,
			@Nonnull SqlStatementLogger logger,
			@Nonnull SessionEventListenerManager statsCollector,
			@Nonnull SharedSessionContractImplementor session) throws SQLException {
		logger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
		final var eventMonitor = session.getEventMonitor();
		final var creationEvent = eventMonitor.beginJdbcPreparedStatementCreationEvent();
		final var statistics = session.getFactory().getStatistics();
		try {
			statsCollector.jdbcPrepareStatementStart();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.prepareStatement();
			}
			return connection.prepareStatement( sql );
		}
		finally {
			eventMonitor.completeJdbcPreparedStatementCreationEvent( creationEvent, sql );
			statsCollector.jdbcPrepareStatementEnd();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.closeStatement();
			}
		}
	}

	private int executeUpdate(
			@Nonnull PreparedStatement ps,
			@Nonnull SessionEventListenerManager statsCollector,
			@Nonnull String sql,
			@Nonnull SharedSessionContractImplementor session) throws SQLException {
		final var eventMonitor = session.getEventMonitor();
		final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
		try {
			statsCollector.jdbcExecuteStatementStart();
			return ps.executeUpdate();
		}
		finally {
			eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
			statsCollector.jdbcExecuteStatementEnd();
		}

	}

	private @Nonnull ResultSet executeQuery(
			@Nonnull PreparedStatement ps,
			@Nonnull SessionEventListenerManager statsCollector,
			@Nonnull String sql,
			@Nonnull SharedSessionContractImplementor session) throws SQLException {
		final var eventMonitor = session.getEventMonitor();
		final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
		try {
			statsCollector.jdbcExecuteStatementStart();
			return ps.executeQuery();
		}
		finally {
			eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
			statsCollector.jdbcExecuteStatementEnd();
		}
	}

	@Override
	public boolean isPhysicalSequence() {
		return false;
	}

	@Override
	public void registerExportables(@Nonnull Database database) {
		final var namespace = database.locateNamespace(
				logicalQualifiedTableName.getCatalogName(),
				logicalQualifiedTableName.getSchemaName()
		);

		final var objectName = logicalQualifiedTableName.getObjectName();
		Table table = namespace.locateTable( objectName );
		final boolean tableCreated;
		if ( table == null ) {
			table = namespace.createTable( objectName,
					identifier -> new Table( contributor, namespace, identifier, false ) );
			tableCreated = true;
		}
		else {
			tableCreated = false;
		}
		physicalTableName = table.getQualifiedTableName();

		valueColumnNameText = logicalValueColumnNameIdentifier.render( database.getDialect() );
		if ( tableCreated ) {
			final var typeConfiguration = database.getTypeConfiguration();
			final var type = typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LONG );
			final String typeName =
					typeConfiguration.getDdlTypeRegistry()
							.getTypeName( type.getJdbcType().getDdlTypeCode(), database.getDialect() );
			final var valueColumn =
					ExportableColumnHelper.column( database, table, valueColumnNameText, type, typeName );
			table.addColumn( valueColumn );
			table.setOptions( options );
			table.addInitCommand( context -> new InitCommand(
					"insert into " + context.format( physicalTableName )
					+ " ( " + valueColumnNameText + " ) values ( " + initialValue + " )"
			) );
		}
	}

	@Override
	public void initialize(@Nonnull SqlStringGenerationContext context) {
		final var dialect = context.getDialect();
		final String formattedPhysicalTableName = context.format( physicalTableName );
		final String lockingClause = dialect.getLockingSupport().getLockingClauseRenderer().render(
				new LockingClauseRequest( PessimisticLockKind.UPDATE, Timeouts.WAIT_FOREVER, List.of() )
		);
		final String lockedTable = TableLockHintRendererSupport.renderTableReference(
				dialect.getLockingSupport(),
				new LockOptions( PESSIMISTIC_WRITE ),
				formattedPhysicalTableName
		) + lockingClause;
		selectQuery = "select " + valueColumnNameText + " as id_val" +
				" from " + lockedTable ;
		updateQuery = "update " + formattedPhysicalTableName +
				" set " + valueColumnNameText + "= ?" +
				" where " + valueColumnNameText + "=?";
	}

	@Override
	public void registerExtraExportables(@Nonnull Table table, @Nonnull Optimizer optimizer) {
		table.addResyncCommand( (sqlContext, isolator) -> {
			final String sequenceTableName = sqlContext.format( physicalTableName );
			final String tableName = sqlContext.format( table.getQualifiedTableName() );
			final String primaryKeyColumnName = table.getPrimaryKey().getColumn( 0 ).getName();
			final int adjustment = optimizer.getAdjustment();
			final long max = getMaxPrimaryKey( isolator, primaryKeyColumnName, tableName );
			final long current = getCurrentTableValue( isolator, sequenceTableName, valueColumnNameText );
			if ( max + adjustment > current ) {
				optimizer.reset();
				final String update =
						"update " + sequenceTableName
						+ " set " + valueColumnNameText + " = " + (max + adjustment);
				return new InitCommand( update );
			}
			else {
				return new InitCommand();
			}
		} );
		table.addResetCommand( sqlContext -> {
			optimizer.reset();
			final String update =
					"update " + sqlContext.format( physicalTableName )
					+ " set " + valueColumnNameText + " = " + initialValue;
			return new InitCommand( update );
		} );
	}
}
