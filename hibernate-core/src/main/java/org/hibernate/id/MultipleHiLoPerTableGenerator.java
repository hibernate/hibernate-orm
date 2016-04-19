/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.LegacyHiLoAlgorithmOptimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

/**
 * A hilo <tt>IdentifierGenerator</tt> that returns a <tt>Long</tt>, constructed using
 * a hi/lo algorithm. The hi value MUST be fetched in a seperate transaction
 * to the <tt>Session</tt> transaction so the generator must be able to obtain
 * a new connection and commit it. Hence this implementation may not
 * be used  when the user is supplying connections. In this
 * case a <tt>SequenceHiLoGenerator</tt> would be a better choice (where
 * supported).<br>
 * <br>
 * <p/>
 * A hilo <tt>IdentifierGenerator</tt> that uses a database
 * table to store the last generated values. A table can contains
 * several hi values. They are distinct from each other through a key
 * <p/>
 * <p>This implementation is not compliant with a user connection</p>
 * <p/>
 * <p/>
 * <p>Allowed parameters (all of them are optional):</p>
 * <ul>
 * <li>table: table name (default <tt>hibernate_sequences</tt>)</li>
 * <li>primary_key_column: key column name (default <tt>sequence_name</tt>)</li>
 * <li>value_column: hi value column name(default <tt>sequence_next_hi_value</tt>)</li>
 * <li>primary_key_value: key value for the current entity (default to the entity's primary table name)</li>
 * <li>primary_key_length: length of the key column in DB represented as a varchar (default to 255)</li>
 * <li>max_lo: max low value beforeQuery increasing hi (default to Short.MAX_VALUE)</li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author <a href="mailto:kr@hbt.de">Klaus Richarz</a>.
 *
 * @deprecated Use {@link org.hibernate.id.enhanced.TableGenerator} instead.
 */
@Deprecated
public class MultipleHiLoPerTableGenerator implements PersistentIdentifierGenerator, Configurable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( MultipleHiLoPerTableGenerator.class );

	public static final String ID_TABLE = "table";
	public static final String PK_COLUMN_NAME = "primary_key_column";
	public static final String PK_VALUE_NAME = "primary_key_value";
	public static final String VALUE_COLUMN_NAME = "value_column";
	public static final String PK_LENGTH_NAME = "primary_key_length";

	private static final int DEFAULT_PK_LENGTH = 255;
	public static final String DEFAULT_TABLE = "hibernate_sequences";
	private static final String DEFAULT_PK_COLUMN = "sequence_name";
	private static final String DEFAULT_VALUE_COLUMN = "sequence_next_hi_value";

	private QualifiedName qualifiedTableName;
	private String tableName;
	private String segmentColumnName;
	private String segmentName;
	private String valueColumnName;
	private String query;
	private String insert;
	private String update;

	//hilo params
	public static final String MAX_LO = "max_lo";

	private int maxLo;
	private LegacyHiLoAlgorithmOptimizer hiloOptimizer;

	private Class returnClass;
	private int keySize;

	public synchronized Serializable generate(final SharedSessionContractImplementor session, Object obj) {
		DeprecationLogger.DEPRECATION_LOGGER.deprecatedTableGenerator( getClass().getName() );

		final SqlStatementLogger statementLogger = session.getFactory().getServiceRegistry()
				.getService( JdbcServices.class )
				.getSqlStatementLogger();
		final SessionEventListenerManager statsCollector = session.getEventListenerManager();

		final WorkExecutorVisitable<IntegralDataTypeHolder> work = new AbstractReturningWork<IntegralDataTypeHolder>() {
			@Override
			public IntegralDataTypeHolder execute(Connection connection) throws SQLException {
				IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( returnClass );

				int rows;
				do {
					final PreparedStatement queryPreparedStatement = prepareStatement(
							connection,
							query,
							statementLogger,
							statsCollector
					);
					try {
						final ResultSet rs = executeQuery( queryPreparedStatement, statsCollector );
						boolean isInitialized = rs.next();
						if ( !isInitialized ) {
							value.initialize( 0 );
							final PreparedStatement insertPreparedStatement = prepareStatement(
									connection,
									insert,
									statementLogger,
									statsCollector
							);
							try {
								value.bind( insertPreparedStatement, 1 );
								executeUpdate( insertPreparedStatement, statsCollector );
							}
							finally {
								insertPreparedStatement.close();
							}
						}
						else {
							value.initialize( rs, 0 );
						}
						rs.close();
					}
					catch (SQLException sqle) {
						LOG.unableToReadOrInitHiValue( sqle );
						throw sqle;
					}
					finally {
						queryPreparedStatement.close();
					}


					final PreparedStatement updatePreparedStatement = prepareStatement(
							connection,
							update,
							statementLogger,
							statsCollector
					);
					try {
						value.copy().increment().bind( updatePreparedStatement, 1 );
						value.bind( updatePreparedStatement, 2 );

						rows = executeUpdate( updatePreparedStatement, statsCollector );
					}
					catch (SQLException sqle) {
						LOG.error( LOG.unableToUpdateHiValue( tableName ), sqle );
						throw sqle;
					}
					finally {
						updatePreparedStatement.close();
					}
				} while ( rows == 0 );

				return value;
			}
		};

		// maxLo < 1 indicates a hilo generator with no hilo :?
		if ( maxLo < 1 ) {
			//keep the behavior consistent even for boundary usages
			IntegralDataTypeHolder value = null;
			while ( value == null || value.lt( 1 ) ) {
				value = session.getTransactionCoordinator().createIsolationDelegate().delegateWork( work, true );
			}
			return value.makeValue();
		}

		return hiloOptimizer.generate(
				new AccessCallback() {
					public IntegralDataTypeHolder getNextValue() {
						return session.getTransactionCoordinator().createIsolationDelegate().delegateWork(
								work,
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

	private PreparedStatement prepareStatement(
			Connection connection,
			String sql,
			SqlStatementLogger statementLogger,
			SessionEventListenerManager statsCollector) throws SQLException {
		statementLogger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
		try {
			statsCollector.jdbcPrepareStatementStart();
			return connection.prepareStatement( sql );
		}
		finally {
			statsCollector.jdbcPrepareStatementEnd();
		}
	}

	private int executeUpdate(PreparedStatement ps, SessionEventListenerManager statsCollector) throws SQLException {
		try {
			statsCollector.jdbcExecuteStatementStart();
			return ps.executeUpdate();
		}
		finally {
			statsCollector.jdbcExecuteStatementEnd();
		}

	}

	private ResultSet executeQuery(PreparedStatement ps, SessionEventListenerManager statsCollector)
			throws SQLException {
		try {
			statsCollector.jdbcExecuteStatementStart();
			return ps.executeQuery();
		}
		finally {
			statsCollector.jdbcExecuteStatementEnd();
		}
	}

	@SuppressWarnings({"StatementWithEmptyBody", "deprecation"})
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		returnClass = type.getReturnedClass();

		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		qualifiedTableName = determineGeneratorTableName( params, jdbcEnvironment );

		segmentColumnName = determineSegmentColumnName( params, jdbcEnvironment );
		keySize = ConfigurationHelper.getInt( PK_LENGTH_NAME, params, DEFAULT_PK_LENGTH );
		segmentName = ConfigurationHelper.getString( PK_VALUE_NAME, params, params.getProperty( TABLE ) );

		valueColumnName = determineValueColumnName( params, jdbcEnvironment );

		//hilo config
		maxLo = ConfigurationHelper.getInt( MAX_LO, params, Short.MAX_VALUE );

		if ( maxLo >= 1 ) {
			hiloOptimizer = new LegacyHiLoAlgorithmOptimizer( returnClass, maxLo );
		}
	}

	protected QualifiedName determineGeneratorTableName(Properties params, JdbcEnvironment jdbcEnvironment) {
		final String tableName = ConfigurationHelper.getString( ID_TABLE, params, DEFAULT_TABLE );

		if ( tableName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( tableName );
		}
		else {
			// todo : need to incorporate implicit catalog and schema names
			final Identifier catalog = jdbcEnvironment.getIdentifierHelper().toIdentifier(
					ConfigurationHelper.getString( CATALOG, params )
			);
			final Identifier schema = jdbcEnvironment.getIdentifierHelper().toIdentifier(
					ConfigurationHelper.getString( SCHEMA, params )
			);
			return new QualifiedNameParser.NameParts(
					catalog,
					schema,
					jdbcEnvironment.getIdentifierHelper().toIdentifier( tableName )
			);
		}
	}

	protected String determineSegmentColumnName(Properties params, JdbcEnvironment jdbcEnvironment) {
		final String name = ConfigurationHelper.getString( PK_COLUMN_NAME, params, DEFAULT_PK_COLUMN );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name ).render( jdbcEnvironment.getDialect() );
	}

	protected String determineValueColumnName(Properties params, JdbcEnvironment jdbcEnvironment) {
		final String name = ConfigurationHelper.getString( VALUE_COLUMN_NAME, params, DEFAULT_VALUE_COLUMN );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name ).render( jdbcEnvironment.getDialect() );
	}

	@Override
	public void registerExportables(Database database) {
		final Namespace namespace = database.locateNamespace(
				qualifiedTableName.getCatalogName(),
				qualifiedTableName.getSchemaName()
		);

		Table table = namespace.locateTable( qualifiedTableName.getObjectName() );
		if ( table == null ) {
			table = namespace.createTable( qualifiedTableName.getObjectName(), false );

			// todo : note sure the best solution here.  do we add the columns if missing?  other?
			table.setPrimaryKey( new PrimaryKey( table ) );

			final Column pkColumn = new ExportableColumn(
					database,
					table,
					segmentColumnName,
					StringType.INSTANCE,
					database.getDialect().getTypeName( Types.VARCHAR, keySize, 0, 0 )
			);
			pkColumn.setNullable( false );
			table.addColumn( pkColumn );
			table.getPrimaryKey().addColumn( pkColumn );

			final Column valueColumn = new ExportableColumn(
					database,
					table,
					valueColumnName,
					LongType.INSTANCE
			);
			table.addColumn( valueColumn );
		}

		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();

		// allow physical naming strategies a chance to kick in
		tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				table.getQualifiedTableName(),
				jdbcEnvironment.getDialect()
		);

		query = "select " +
				valueColumnName +
				" from " +
				jdbcEnvironment.getDialect().appendLockHint( LockMode.PESSIMISTIC_WRITE, tableName ) +
				" where " + segmentColumnName + " = '" + segmentName + "'" +
				jdbcEnvironment.getDialect().getForUpdateString();

		update = "update " +
				tableName +
				" set " +
				valueColumnName +
				" = ? where " +
				valueColumnName +
				" = ? and " +
				segmentColumnName +
				" = '" +
				segmentName
				+ "'";

		insert = "insert into " + tableName +
				"(" + segmentColumnName + ", " + valueColumnName + ") " +
				"values('" + segmentName + "', ?)";



	}

	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
				dialect.getCreateTableString()
						+ ' ' + tableName + " ( "
						+ segmentColumnName + ' ' + dialect.getTypeName( Types.VARCHAR, keySize, 0, 0 ) + ",  "
						+ valueColumnName + ' ' + dialect.getTypeName( Types.INTEGER )
						+ " )" + dialect.getTableTypeString()
		};
	}

	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return new String[] {dialect.getDropTableString( tableName )};
	}

	public Object generatorKey() {
		return tableName;
	}
}
