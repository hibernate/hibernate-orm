/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Properties;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.id.ExportableColumn;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import static java.util.Collections.singletonMap;
import static org.hibernate.cfg.AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY;
import static org.hibernate.id.enhanced.OptimizerFactory.determineImplicitOptimizerName;
import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInt;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * An enhanced version of table-based id generation.
 * <p>
 * Unlike the simplistic legacy one (which  was only ever intended for subclassing
 * support) we "segment" the table into multiple values.  Thus, a single table can
 * actually serve as the persistent storage for multiple independent generators.  One
 * approach would be to segment the values by the name of the entity for which we are
 * performing generation, which would mean that we would have a row in the generator
 * table for each entity name.  Or any configuration really; the setup is very flexible.
 * <p>
 * By default, we use a single row for all generators (the {@value #DEF_SEGMENT_VALUE}
 * segment). The configuration parameter {@value #CONFIG_PREFER_SEGMENT_PER_ENTITY} can
 * be used to change that to instead default to using a row for each entity name.
 * <p>
 * <table>
 * <caption>Configuration parameters</caption>
 * 	 <tr>
 *     <td><b>Parameter name</b></td>
 *     <td><b>Default value</b></td>
 *     <td><b>Interpretation</b></td>
 *   </tr>
 *   <tr>
 *     <td>{@value #TABLE_PARAM}</td>
 *     <td>{@value #DEF_TABLE}</td>
 *     <td>The name of the table to use to store/retrieve values</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #VALUE_COLUMN_PARAM}</td>
 *     <td>{@value #DEF_VALUE_COLUMN}</td>
 *     <td>The name of column which holds the sequence value for the given segment</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #SEGMENT_COLUMN_PARAM}</td>
 *     <td>{@value #DEF_SEGMENT_COLUMN}</td>
 *     <td>The name of the column which holds the segment key</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #SEGMENT_VALUE_PARAM}</td>
 *     <td>{@value #DEF_SEGMENT_VALUE}</td>
 *     <td>The value indicating which segment is used by this generator;
 *         refers to values in the {@value #SEGMENT_COLUMN_PARAM} column</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #SEGMENT_LENGTH_PARAM}</td>
 *     <td>{@value #DEF_SEGMENT_LENGTH}</td>
 *     <td>The data length of the {@value #SEGMENT_COLUMN_PARAM} column;
 *         used for schema creation</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #INITIAL_PARAM}</td>
 *     <td>{@value #DEFAULT_INITIAL_VALUE}</td>
 *     <td>The initial value to be stored for the given segment</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #INCREMENT_PARAM}</td>
 *     <td>{@value #DEFAULT_INCREMENT_SIZE}</td>
 *     <td>The increment size for the underlying segment;
 *         see the discussion on {@link Optimizer} for more details.</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #OPT_PARAM}</td>
 *     <td><em>depends on defined increment size</em></td>
 *     <td>Allows explicit definition of which optimization strategy to use</td>
 *   </tr>
 * </table>
 *
 * @author Steve Ebersole
 */
public class TableGenerator implements PersistentIdentifierGenerator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TableGenerator.class.getName()
	);

	/**
	 * By default, in the absence of a {@value #SEGMENT_VALUE_PARAM} setting, we use a single row for all
	 * generators.  This setting can be used to change that to instead default to using a row for each entity name.
	 */
	public static final String CONFIG_PREFER_SEGMENT_PER_ENTITY = "prefer_entity_table_as_segment_value";

	/**
	 * Configures the name of the table to use.  The default value is {@value #DEF_TABLE}
	 */
	public static final String TABLE_PARAM = "table_name";

	/**
	 * The default {@value #TABLE_PARAM} value
	 */
	public static final String DEF_TABLE = "hibernate_sequences";

	/**
	 * The name of column which holds the sequence value.  The default value is {@value #DEF_VALUE_COLUMN}
	 */
	public static final String VALUE_COLUMN_PARAM = "value_column_name";

	/**
	 * The default {@value #VALUE_COLUMN_PARAM} value
	 */
	public static final String DEF_VALUE_COLUMN = "next_val";

	/**
	 * The name of the column which holds the segment key.  The segment defines the different buckets (segments)
	 * of values currently tracked in the table.  The default value is {@value #DEF_SEGMENT_COLUMN}
	 */
	public static final String SEGMENT_COLUMN_PARAM = "segment_column_name";

	/**
	 * The default {@value #SEGMENT_COLUMN_PARAM} value
	 */
	public static final String DEF_SEGMENT_COLUMN = "sequence_name";

	/**
	 * The value indicating which segment is used by this generator, as indicated by the actual value stored in the
	 * column indicated by {@value #SEGMENT_COLUMN_PARAM}.  The default value for setting is {@link #DEF_SEGMENT_VALUE},
	 * although {@value #CONFIG_PREFER_SEGMENT_PER_ENTITY} effects the default as well.
	 */
	public static final String SEGMENT_VALUE_PARAM = "segment_value";

	/**
	 * The default {@value #SEGMENT_VALUE_PARAM} value, unless {@link #CONFIG_PREFER_SEGMENT_PER_ENTITY} is specified
	 */
	public static final String DEF_SEGMENT_VALUE = "default";

	/**
	 * Indicates the length of the column defined by {@link #SEGMENT_COLUMN_PARAM}.  Used in schema export.  The
	 * default value is {@value #DEF_SEGMENT_LENGTH}
	 */
	public static final String SEGMENT_LENGTH_PARAM = "segment_value_length";

	/**
	 * The default {@value #SEGMENT_LENGTH_PARAM} value
	 */
	public static final int DEF_SEGMENT_LENGTH = 255;

	private boolean storeLastUsedValue;


	private Type identifierType;

	private QualifiedName qualifiedTableName;
	private QualifiedName physicalTableName;

	private String segmentColumnName;
	private String segmentValue;
	private int segmentValueLength;

	private String valueColumnName;
	private int initialValue;
	private int incrementSize;

	private String selectQuery;
	private String insertQuery;
	private String updateQuery;

	private Optimizer optimizer;
	private long accessCount;

	private String contributor;

	/**
	 * Type mapping for the identifier.
	 *
	 * @return The identifier type mapping.
	 */
	public final Type getIdentifierType() {
		return identifierType;
	}

	/**
	 * The name of the table in which we store this generator's persistent state.
	 *
	 * @return The table name.
	 */
	public final String getTableName() {
		return qualifiedTableName.render();
	}

	/**
	 * The name of the column in which we store the segment to which each row
	 * belongs.  The value here acts as PK.
	 *
	 * @return The segment column name
	 */
	public final String getSegmentColumnName() {
		return segmentColumnName;
	}

	/**
	 * The value in {@link #getSegmentColumnName segment column}
	 * corresponding to this generator instance.  In other words this value
	 * indicates the row in which this generator instance will store values.
	 *
	 * @return The segment value for this generator instance.
	 */
	public final String getSegmentValue() {
		return segmentValue;
	}

	/**
	 * The size of the {@link #getSegmentColumnName segment column} in the
	 * underlying table.
	 *
	 * @apiNote This should really have been called {@code segmentColumnLength}
	 *          or even better {@code segmentColumnSize}.
	 *
	 * @return the column size.
	 */
	public final int getSegmentValueLength() {
		return segmentValueLength;
	}

	/**
	 * The name of the column in which we store our persistent generator value.
	 *
	 * @return The name of the value column.
	 */
	public final String getValueColumnName() {
		return valueColumnName;
	}

	/**
	 * The initial value to use when we find no previous state in the
	 * generator table corresponding to our sequence.
	 *
	 * @return The initial value to use.
	 */
	public final int getInitialValue() {
		return initialValue;
	}

	/**
	 * The amount of increment to use.  The exact implications of this
	 * depends on the {@linkplain #getOptimizer() optimizer} being used.
	 *
	 * @return The increment amount.
	 */
	public final int getIncrementSize() {
		return incrementSize;
	}

	/**
	 * The optimizer being used by this generator.
	 *
	 * @return Out optimizer.
	 */
	@Override
	public final Optimizer getOptimizer() {
		return optimizer;
	}

	/**
	 * Getter for property 'tableAccessCount'.  Only really useful for unit test
	 * assertions.
	 *
	 * @return Value for property 'tableAccessCount'.
	 */
	public final long getTableAccessCount() {
		return accessCount;
	}

	/**
	 * @deprecated Exposed for tests only.
	 */
	@Deprecated
	public String[] getAllSqlForTests() {
		return new String[] { selectQuery, insertQuery, updateQuery };
	}

	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException {
		storeLastUsedValue = serviceRegistry.getService( ConfigurationService.class )
				.getSetting( AvailableSettings.TABLE_GENERATOR_STORE_LAST_USED, StandardConverters.BOOLEAN, true );
		identifierType = type;

		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		qualifiedTableName = determineGeneratorTableName( parameters, jdbcEnvironment, serviceRegistry );
		segmentColumnName = determineSegmentColumnName( parameters, jdbcEnvironment );
		valueColumnName = determineValueColumnName( parameters, jdbcEnvironment );

		segmentValue = determineSegmentValue( parameters );

		segmentValueLength = determineSegmentColumnSize( parameters );
		initialValue = determineInitialValue( parameters );
		incrementSize = determineIncrementSize( parameters );

		optimizer = OptimizerFactory.buildOptimizer(
				determineOptimizationStrategy( parameters, incrementSize ),
				identifierType.getReturnedClass(),
				incrementSize,
				getInt( INITIAL_PARAM, parameters, -1 )
		);

		contributor = parameters.getProperty( CONTRIBUTOR_NAME );
		if ( contributor == null ) {
			contributor = "orm";
		}
	}

	private static OptimizerDescriptor determineOptimizationStrategy(Properties parameters, int incrementSize) {
		return StandardOptimizerDescriptor.fromExternalName(
				getString( OPT_PARAM, parameters, determineImplicitOptimizerName( incrementSize, parameters ) )
		);
	}

	/**
	 * Determine the table name to use for the generator values.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @see #getTableName()
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnvironment The JDBC environment
	 * @return The table name to use.
	 */
	protected QualifiedName determineGeneratorTableName(Properties params, JdbcEnvironment jdbcEnvironment, ServiceRegistry serviceRegistry) {
		final String explicitTableName = getString( TABLE_PARAM, params );
		if ( StringHelper.isNotEmpty( explicitTableName ) ) {
			if ( explicitTableName.contains( "." ) ) {
				return QualifiedNameParser.INSTANCE.parse( explicitTableName );
			}
			else {
				final Identifier catalog = jdbcEnvironment.getIdentifierHelper().toIdentifier(
						getString( CATALOG, params )
				);
				final Identifier schema = jdbcEnvironment.getIdentifierHelper().toIdentifier(
						getString( SCHEMA, params )
				);
				return new QualifiedNameParser.NameParts(
						catalog,
						schema,
						jdbcEnvironment.getIdentifierHelper().toIdentifier( explicitTableName )
				);
			}
		}

		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );

		final String namingStrategySetting = coalesceSuppliedValues(
				() -> {
					final String localSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, params );
					if ( localSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return localSetting;
				},
				() -> {
					final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
					final String globalSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, configurationService.getSettings() );
					if ( globalSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return globalSetting;
				},
				StandardNamingStrategy.class::getName
		);

		final ImplicitDatabaseObjectNamingStrategy namingStrategy = strategySelector.resolveStrategy(
				ImplicitDatabaseObjectNamingStrategy.class,
				namingStrategySetting
		);

		final Identifier catalog = jdbcEnvironment.getIdentifierHelper().toIdentifier(
				getString( CATALOG, params )
		);
		final Identifier schema = jdbcEnvironment.getIdentifierHelper().toIdentifier(
				getString( SCHEMA, params )
		);

		return namingStrategy.determineTableName(  catalog, schema, params, serviceRegistry );
	}

	/**
	 * Determine the name of the column used to indicate the segment for each
	 * row.  This column acts as the primary key.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @see #getSegmentColumnName()
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnvironment The JDBC environment
	 * @return The name of the segment column
	 */
	protected String determineSegmentColumnName(Properties params, JdbcEnvironment jdbcEnvironment) {
		final String name = getString( SEGMENT_COLUMN_PARAM, params, DEF_SEGMENT_COLUMN );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name ).render( jdbcEnvironment.getDialect() );
	}

	/**
	 * Determine the name of the column in which we will store the generator persistent value.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @see #getValueColumnName()
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnvironment The JDBC environment
	 * @return The name of the value column
	 */
	protected String determineValueColumnName(Properties params, JdbcEnvironment jdbcEnvironment) {
		final String name = getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name ).render( jdbcEnvironment.getDialect() );
	}

	/**
	 * Determine the segment value corresponding to this generator instance.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @see #getSegmentValue()
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @return The name of the value column
	 */
	protected String determineSegmentValue(Properties params) {
		String segmentValue = params.getProperty( SEGMENT_VALUE_PARAM );
		if ( StringHelper.isEmpty( segmentValue ) ) {
			segmentValue = determineDefaultSegmentValue( params );
		}
		return segmentValue;
	}

	/**
	 * Used in the cases where {@link #determineSegmentValue} is unable to
	 * determine the value to use.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @return The default segment value to use.
	 */
	protected String determineDefaultSegmentValue(Properties params) {
		final boolean preferSegmentPerEntity = getBoolean( CONFIG_PREFER_SEGMENT_PER_ENTITY, params );
		final String defaultToUse = preferSegmentPerEntity ? params.getProperty( TABLE ) : DEF_SEGMENT_VALUE;
		LOG.usingDefaultIdGeneratorSegmentValue( qualifiedTableName.render(), segmentColumnName, defaultToUse );
		return defaultToUse;
	}

	/**
	 * Determine the size of the {@link #getSegmentColumnName segment column}
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @see #getSegmentValueLength()
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @return The size of the segment column
	 */
	protected int determineSegmentColumnSize(Properties params) {
		return getInt( SEGMENT_LENGTH_PARAM, params, DEF_SEGMENT_LENGTH );
	}

	protected int determineInitialValue(Properties params) {
		return getInt( INITIAL_PARAM, params, DEFAULT_INITIAL_VALUE );
	}

	protected int determineIncrementSize(Properties params) {
		return getInt( INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE );
	}

	protected String buildSelectQuery(String formattedPhysicalTableName, SqlStringGenerationContext context) {
		final String alias = "tbl";
		final String query = "select " + StringHelper.qualify( alias, valueColumnName )
				+ " from " + formattedPhysicalTableName + ' ' + alias
				+ " where " + StringHelper.qualify( alias, segmentColumnName ) + "=?";
		final LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		lockOptions.setAliasSpecificLockMode( alias, LockMode.PESSIMISTIC_WRITE );
		final Map<String,String[]> updateTargetColumnsMap = singletonMap( alias, new String[] { valueColumnName } );
		return context.getDialect().applyLocksToSql( query, lockOptions, updateTargetColumnsMap );
	}

	protected String buildUpdateQuery(String formattedPhysicalTableName, SqlStringGenerationContext context) {
		return "update " + formattedPhysicalTableName
				+ " set " + valueColumnName + "=? "
				+ " where " + valueColumnName + "=? and " + segmentColumnName + "=?";
	}

	protected String buildInsertQuery(String formattedPhysicalTableName, SqlStringGenerationContext context) {
		return "insert into " + formattedPhysicalTableName
				+ " (" + segmentColumnName + ", " + valueColumnName + ") " + " values (?,?)";
	}

	protected InitCommand generateInsertInitCommand(SqlStringGenerationContext context) {
		String renderedTableName = context.format( physicalTableName );
		int value = initialValue;
		if ( storeLastUsedValue ) {
			value = initialValue - 1;
		}
		return new InitCommand( "insert into " + renderedTableName
				+ "(" + segmentColumnName + ", " + valueColumnName + ")"
				+ " values ('" + segmentValue + "'," + ( value ) + ")" );
	}

	private IntegralDataTypeHolder makeValue() {
		return IdentifierGeneratorHelper.getIntegralDataTypeHolder( identifierType.getReturnedClass() );
	}

	@Override
	public Object generate(final SharedSessionContractImplementor session, final Object obj) {
		final SqlStatementLogger statementLogger = session.
				getFactory().getJdbcServices()
				.getSqlStatementLogger();
		final SessionEventListenerManager statsCollector = session.getEventListenerManager();

		return optimizer.generate(
				new AccessCallback() {
					@Override
					public IntegralDataTypeHolder getNextValue() {
						return session.getTransactionCoordinator().createIsolationDelegate().delegateWork(
								new AbstractReturningWork<>() {
									@Override
									public IntegralDataTypeHolder execute(Connection connection) throws SQLException {
										return nextValue( connection, statementLogger, statsCollector, session );
									}
								},
								true
						);
					}

					@Override
					public String getTenantIdentifier() {
						return session.getTenantIdentifier();
					}
				}
		);
	}

	private IntegralDataTypeHolder nextValue(
			Connection connection,
			SqlStatementLogger logger,
			SessionEventListenerManager listener,
			SharedSessionContractImplementor session)
			throws SQLException {
		final IntegralDataTypeHolder value = makeValue();
		int rows;
		do {

			try ( PreparedStatement selectPS = prepareStatement( connection, selectQuery, logger, listener, session ) ) {
				selectPS.setString( 1, segmentValue );
				final ResultSet selectRS = executeQuery( selectPS, listener, selectQuery, session );
				if ( !selectRS.next() ) {
					long initializationValue;
					if ( storeLastUsedValue ) {
						initializationValue = initialValue - 1;
					}
					else {
						initializationValue = initialValue;
					}
					value.initialize( initializationValue );

					try ( PreparedStatement statement = prepareStatement( connection, insertQuery, logger, listener, session ) ) {
						LOG.tracef( "binding parameter [%s] - [%s]", 1, segmentValue );
						statement.setString( 1, segmentValue );
						value.bind( statement, 2 );
						executeUpdate( statement, listener, insertQuery, session);
					}
				}
				else {
					int defaultValue;
					if ( storeLastUsedValue ) {
						defaultValue = 0;
					}
					else {
						defaultValue = 1;
					}
					value.initialize( selectRS, defaultValue );
				}
				selectRS.close();
			}
			catch (SQLException e) {
				LOG.unableToReadOrInitHiValue( e );
				throw e;
			}


			try ( PreparedStatement statement = prepareStatement( connection, updateQuery, logger, listener, session ) ) {
				final IntegralDataTypeHolder updateValue = value.copy();
				if ( optimizer.applyIncrementSizeToSourceValues() ) {
					updateValue.add( incrementSize );
				}
				else {
					updateValue.increment();
				}
				updateValue.bind( statement, 1 );
				value.bind( statement, 2 );
				statement.setString( 3, segmentValue );
				rows = executeUpdate( statement, listener, updateQuery, session );
			}
			catch (SQLException e) {
				LOG.unableToUpdateQueryHiValue( physicalTableName.render(), e );
				throw e;
			}
		}
		while ( rows == 0 );

		accessCount++;
		if ( storeLastUsedValue ) {
			return value.increment();
		}
		else {
			return value;
		}
	}

	private PreparedStatement prepareStatement(
			Connection connection,
			String sql,
			SqlStatementLogger logger,
			SessionEventListenerManager listener,
			SharedSessionContractImplementor session) throws SQLException {
		logger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
		final EventManager eventManager = session.getEventManager();
		final HibernateEvent jdbcPreparedStatementCreation = eventManager.beginJdbcPreparedStatementCreationEvent();
		try {
			listener.jdbcPrepareStatementStart();
			return connection.prepareStatement( sql );
		}
		finally {
			eventManager.completeJdbcPreparedStatementCreationEvent( jdbcPreparedStatementCreation, sql );
			listener.jdbcPrepareStatementEnd();
		}
	}

	private int executeUpdate(
			PreparedStatement ps,
			SessionEventListenerManager listener,
			String sql,
			SharedSessionContractImplementor session) throws SQLException {
		final EventManager eventManager = session.getEventManager();
		final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
		try {
			listener.jdbcExecuteStatementStart();
			return ps.executeUpdate();
		}
		finally {
			eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
			listener.jdbcExecuteStatementEnd();
		}
	}

	private ResultSet executeQuery(
			PreparedStatement ps,
			SessionEventListenerManager listener,
			String sql,
			SharedSessionContractImplementor session) throws SQLException {
		final EventManager eventManager = session.getEventManager();
		final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
		try {
			listener.jdbcExecuteStatementStart();
			return ps.executeQuery();
		}
		finally {
			eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
			listener.jdbcExecuteStatementEnd();
		}
	}

	@Override
	public void registerExportables(Database database) {
		final Namespace namespace = database.locateNamespace(
				qualifiedTableName.getCatalogName(),
				qualifiedTableName.getSchemaName()
		);

		Table table = namespace.locateTable( qualifiedTableName.getObjectName() );
		if ( table == null ) {
			table = namespace.createTable(
					qualifiedTableName.getObjectName(),
					(identifier) -> new Table( contributor, namespace, identifier, false )
			);

			final BasicTypeRegistry basicTypeRegistry = database.getTypeConfiguration().getBasicTypeRegistry();
			// todo : not sure the best solution here.  do we add the columns if missing?  other?
			final Column segmentColumn = new ExportableColumn(
					database,
					table,
					segmentColumnName,
					basicTypeRegistry.resolve( StandardBasicTypes.STRING ),
					database.getTypeConfiguration()
							.getDdlTypeRegistry()
							.getTypeName( Types.VARCHAR, Size.length( segmentValueLength ) )
			);
			segmentColumn.setNullable( false );
			table.addColumn( segmentColumn );

			// lol
			table.setPrimaryKey( new PrimaryKey( table ) );
			table.getPrimaryKey().addColumn( segmentColumn );

			final Column valueColumn = new ExportableColumn(
					database,
					table,
					valueColumnName,
					basicTypeRegistry.resolve( StandardBasicTypes.LONG )
			);
			table.addColumn( valueColumn );
		}

		// allow physical naming strategies a chance to kick in
		this.physicalTableName = table.getQualifiedTableName();
		table.addInitCommand( this::generateInsertInitCommand );
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		String formattedPhysicalTableName = context.format( physicalTableName );
		this.selectQuery = buildSelectQuery( formattedPhysicalTableName, context );
		this.updateQuery = buildUpdateQuery( formattedPhysicalTableName, context );
		this.insertQuery = buildInsertQuery( formattedPhysicalTableName, context );
	}
}
