/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.LegacyHiLoAlgorithmOptimizer;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 *
 * A hilo <tt>IdentifierGenerator</tt> that returns a <tt>Long</tt>, constructed using
 * a hi/lo algorithm. The hi value MUST be fetched in a seperate transaction
 * to the <tt>Session</tt> transaction so the generator must be able to obtain
 * a new connection and commit it. Hence this implementation may not
 * be used  when the user is supplying connections. In this
 * case a <tt>SequenceHiLoGenerator</tt> would be a better choice (where
 * supported).<br>
 * <br>
 *
 * A hilo <tt>IdentifierGenerator</tt> that uses a database
 * table to store the last generated values. A table can contains
 * several hi values. They are distinct from each other through a key
 * <p/>
 * <p>This implementation is not compliant with a user connection</p>
 * <p/>
 *
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

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       MultipleHiLoPerTableGenerator.class.getName());

	public static final String ID_TABLE = "table";
	public static final String PK_COLUMN_NAME = "primary_key_column";
	public static final String PK_VALUE_NAME = "primary_key_value";
	public static final String VALUE_COLUMN_NAME = "value_column";
	public static final String PK_LENGTH_NAME = "primary_key_length";

	private static final int DEFAULT_PK_LENGTH = 255;
	public static final String DEFAULT_TABLE = "hibernate_sequences";
	private static final String DEFAULT_PK_COLUMN = "sequence_name";
	private static final String DEFAULT_VALUE_COLUMN = "sequence_next_hi_value";

	private ObjectName qualifiedTableName;
	private Identifier qualifiedPkColumnName;
	private Identifier qualifiedValueColumnName;

	private String tableName;
	private Table table;
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


	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
			new StringBuilder( dialect.getCreateTableString() )
					.append( ' ' )
					.append( tableName )
					.append( " ( " )
					.append( pkColumnName )
					.append( ' ' )
					.append( dialect.getTypeName( Types.VARCHAR, keySize, 0, 0 ) )
					.append( ",  " )
					.append( valueColumnName )
					.append( ' ' )
					.append( dialect.getTypeName( Types.INTEGER ) )
					.append( " )" )
					.append( dialect.getTableTypeString() )
					.toString()
		};
	}

	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return new String[] { dialect.getDropTableString( tableName ) };
	}

	public Object generatorKey() {
		return tableName;
	}

	/**
	 * The bound Table for this generator.
	 *
	 * @return The table.
	 */
	public final Table getTable() {
		return table;
	}

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
					final PreparedStatement queryPreparedStatement = prepareStatement( connection, query, statementLogger, statsCollector );
					try {
						final ResultSet rs = executeQuery( queryPreparedStatement, statsCollector );
						boolean isInitialized = rs.next();
						if ( !isInitialized ) {
							value.initialize( 0 );
							final PreparedStatement insertPreparedStatement = prepareStatement( connection, insert, statementLogger, statsCollector );
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


					final PreparedStatement updatePreparedStatement = prepareStatement( connection, update, statementLogger, statsCollector );
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
				} while ( rows==0 );

				return value;
			}
		};

		// maxLo < 1 indicates a hilo generator with no hilo :?
		if ( maxLo < 1 ) {
			//keep the behavior consistent even for boundary usages
			IntegralDataTypeHolder value = null;
			while ( value == null || value.lt( 1 ) ) {
				value = session.getTransactionCoordinator().getTransaction().createIsolationDelegate().delegateWork( work, true );
			}
			return value.makeValue();
		}

		return hiloOptimizer.generate(
				new AccessCallback() {
					public IntegralDataTypeHolder getNextValue() {
						return session.getTransactionCoordinator().getTransaction().createIsolationDelegate().delegateWork(
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

	private ResultSet executeQuery(PreparedStatement ps, SessionEventListenerManager statsCollector) throws SQLException {
		try {
			statsCollector.jdbcExecuteStatementStart();
			return ps.executeQuery();
		}
		finally {
			statsCollector.jdbcExecuteStatementEnd();
		}
	}

	public void configure(Type type, Properties params, Dialect dialect, ClassLoaderService classLoaderService) throws MappingException {
		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );

		tableName = normalizer.normalizeIdentifierQuoting( ConfigurationHelper.getString( ID_TABLE, params, DEFAULT_TABLE ) );
		if ( tableName.indexOf( '.' ) < 0 ) {
			final String normalizedTableName = tableName;
			final String normalizedSchemaName = normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) );
			final String normalizedCatalogName = normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) );
			qualifiedTableName = new ObjectName( normalizedCatalogName, normalizedSchemaName, normalizedTableName );
			tableName = qualifiedTableName.toText( dialect );
		}
		else {
			qualifiedTableName = ObjectName.parse( tableName );
		}

		qualifiedPkColumnName = Identifier.toIdentifier(
				normalizer.normalizeIdentifierQuoting(
						ConfigurationHelper.getString( PK_COLUMN_NAME, params, DEFAULT_PK_COLUMN )
				)
		);
		pkColumnName = qualifiedPkColumnName.getText( dialect );

		qualifiedValueColumnName = Identifier.toIdentifier(
				normalizer.normalizeIdentifierQuoting(
						ConfigurationHelper.getString( VALUE_COLUMN_NAME, params, DEFAULT_VALUE_COLUMN )
				)
		);
		valueColumnName = qualifiedValueColumnName.getText( dialect );

		keySize = ConfigurationHelper.getInt(PK_LENGTH_NAME, params, DEFAULT_PK_LENGTH);
		String keyValue = ConfigurationHelper.getString(PK_VALUE_NAME, params, params.getProperty(TABLE) );

		query = "select " +
			valueColumnName +
			" from " +
			dialect.appendLockHint( new LockOptions( LockMode.PESSIMISTIC_WRITE ), tableName ) +
			" where " + pkColumnName + " = '" + keyValue + "'" +
			dialect.getForUpdateString();

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
			"(" + pkColumnName + ", " +	valueColumnName + ") " +
			"values('"+ keyValue +"', ?)";


		//hilo config
		maxLo = ConfigurationHelper.getInt(MAX_LO, params, Short.MAX_VALUE);
		returnClass = type.getReturnedClass();

		if ( maxLo >= 1 ) {
			hiloOptimizer = new LegacyHiLoAlgorithmOptimizer( returnClass, maxLo );
		}
	}

	@Override
	public void registerExportables(Database database) {
		final Dialect dialect = database.getJdbcEnvironment().getDialect();

		final Schema schema = database.getSchemaFor( qualifiedTableName );
		table = schema.createTable( qualifiedTableName.getName(), qualifiedTableName.getName() );

		final Column pkColumn = table.createColumn( qualifiedPkColumnName );
		table.getPrimaryKey().addColumn( pkColumn );
		// todo : leverage TypeInfo-like info from JdbcEnvironment
		pkColumn.setSqlType( dialect.getTypeName( Types.VARCHAR, keySize, 0, 0 ) );

		final Column valueColumn = table.createColumn( qualifiedValueColumnName );
		valueColumn.setSqlType( dialect.getTypeName( Types.INTEGER ) );
	}
}
