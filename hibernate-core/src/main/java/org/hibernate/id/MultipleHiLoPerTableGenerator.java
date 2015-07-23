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
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.LegacyHiLoAlgorithmOptimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
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
 * <li>max_lo: max low value before increasing hi (default to Short.MAX_VALUE)</li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author <a href="mailto:kr@hbt.de">Klaus Richarz</a>.
 */
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
	private String pkColumnName;
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

	public synchronized Serializable generate(final SessionImplementor session, Object obj) {
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
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		final ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( IDENTIFIER_NORMALIZER );

		qualifiedTableName = QualifiedNameParser.INSTANCE.parse(
				ConfigurationHelper.getString( ID_TABLE, params, DEFAULT_TABLE ),
				normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) ),
				normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) )
		);

		tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				qualifiedTableName,
				jdbcEnvironment.getDialect()
		);
		pkColumnName = normalizer.toDatabaseIdentifierText(
				ConfigurationHelper.getString( PK_COLUMN_NAME, params, DEFAULT_PK_COLUMN )
		);
		valueColumnName = normalizer.toDatabaseIdentifierText(
				ConfigurationHelper.getString( VALUE_COLUMN_NAME, params, DEFAULT_VALUE_COLUMN )
		);

		keySize = ConfigurationHelper.getInt( PK_LENGTH_NAME, params, DEFAULT_PK_LENGTH );
		String keyValue = ConfigurationHelper.getString( PK_VALUE_NAME, params, params.getProperty( TABLE ) );

		query = "select " +
				valueColumnName +
				" from " +
				jdbcEnvironment.getDialect().appendLockHint( LockMode.PESSIMISTIC_WRITE, tableName ) +
				" where " + pkColumnName + " = '" + keyValue + "'" +
				jdbcEnvironment.getDialect().getForUpdateString();

		update = "update " +
				tableName +
				" set " +
				valueColumnName +
				" = ? where " +
				valueColumnName +
				" = ? and " +
				pkColumnName +
				" = '" +
				keyValue
				+ "'";

		insert = "insert into " + tableName +
				"(" + pkColumnName + ", " + valueColumnName + ") " +
				"values('" + keyValue + "', ?)";


		//hilo config
		maxLo = ConfigurationHelper.getInt( MAX_LO, params, Short.MAX_VALUE );
		returnClass = type.getReturnedClass();

		if ( maxLo >= 1 ) {
			hiloOptimizer = new LegacyHiLoAlgorithmOptimizer( returnClass, maxLo );
		}
	}

	@Override
	public void registerExportables(Database database) {
		final Namespace namespace = database.locateNamespace(
				qualifiedTableName.getCatalogName(),
				qualifiedTableName.getSchemaName()
		);

		final Table table = namespace.createTable( qualifiedTableName.getObjectName(), false );
		table.setPrimaryKey( new PrimaryKey() );

		final Column pkColumn = new ExportableColumn(
				database,
				table,
				pkColumnName,
				StringType.INSTANCE,
				database.getDialect().getTypeName( Types.VARCHAR, keySize, 0, 0 )
		);
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

	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
				dialect.getCreateTableString()
						+ ' ' + tableName + " ( "
						+ pkColumnName + ' ' + dialect.getTypeName( Types.VARCHAR, keySize, 0, 0 ) + ",  "
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
