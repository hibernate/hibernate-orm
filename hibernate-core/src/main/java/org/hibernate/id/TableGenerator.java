/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * An <tt>IdentifierGenerator</tt> that uses a database
 * table to store the last generated value. It is not
 * intended that applications use this strategy directly.
 * However, it may be used to build other (efficient)
 * strategies. The returned type is any supported by
 * {@link IntegralDataTypeHolder}
 * <p/>
 * The value MUST be fetched in a separate transaction
 * from that of the main {@link SessionImplementor session}
 * transaction so the generator must be able to obtain a new
 * connection and commit it. Hence this implementation may only
 * be used when Hibernate is fetching connections, not when the
 * user is supplying connections.
 * <p/>
 * Again, the return types supported here are any of the ones
 * supported by {@link IntegralDataTypeHolder}.  This is new
 * as of 3.5.  Prior to that this generator only returned {@link Integer}
 * values.
 * <p/>
 * Mapping parameters supported: table, column
 *
 * @see TableHiLoGenerator
 * @author Gavin King
 *
 * @deprecated Going away in 5.0, use {@link org.hibernate.id.enhanced.SequenceStyleGenerator} or
 * {@link org.hibernate.id.enhanced.TableGenerator} instead
 */
@Deprecated
public class TableGenerator implements PersistentIdentifierGenerator, Configurable {
	/* COLUMN and TABLE should be renamed but it would break the public API */
	/** The column parameter */
	public static final String COLUMN = "column";

	/** Default column name */
	public static final String DEFAULT_COLUMN_NAME = "next_hi";

	/** The table parameter */
	public static final String TABLE = "table";

	/** Default table name */
	public static final String DEFAULT_TABLE_NAME = "hibernate_unique_key";

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TableGenerator.class.getName());

	private Type identifierType;
	private String tableName;
	private String columnName;
	private String query;
	private String update;

	public void configure(Type type, Properties params, Dialect dialect, ClassLoaderService classLoaderService) {
		identifierType = type;

		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );

		tableName = ConfigurationHelper.getString( TABLE, params, DEFAULT_TABLE_NAME );
		if ( tableName.indexOf( '.' ) < 0 ) {
			final String schemaName = normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) );
			final String catalogName = normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) );
			tableName = new ObjectName(catalogName, schemaName, tableName).toText( dialect );
		}
		else {
			// if already qualified there is not much we can do in a portable manner so we pass it
			// through and assume the user has set up the name correctly.
		}

		columnName = dialect.quote(
				normalizer.normalizeIdentifierQuoting(
						ConfigurationHelper.getString( COLUMN, params, DEFAULT_COLUMN_NAME )
				)
		);

		query = "select " +
			columnName +
			" from " +
			dialect.appendLockHint(new LockOptions( LockMode.PESSIMISTIC_WRITE ), tableName) +
			dialect.getForUpdateString();

		update = "update " +
			tableName +
			" set " +
			columnName +
			" = ? where " +
			columnName +
			" = ?";
	}

	public synchronized Serializable generate(SessionImplementor session, Object object) {
		return generateHolder( session ).makeValue();
	}

	protected IntegralDataTypeHolder generateHolder(final SessionImplementor session) {
		final SqlStatementLogger statementLogger = session.getFactory().getServiceRegistry()
				.getService( JdbcServices.class )
				.getSqlStatementLogger();
		final SessionEventListenerManager listeners = session.getEventListenerManager();

		return session.getTransactionCoordinator().getTransaction().createIsolationDelegate().delegateWork(
				new AbstractReturningWork<IntegralDataTypeHolder>() {
					@Override
					public IntegralDataTypeHolder execute(Connection connection) throws SQLException {
						IntegralDataTypeHolder value = buildHolder();
						int rows;

						// The loop ensures atomicity of the select + update even for no transaction or
						// read committed isolation level
						do {
							final PreparedStatement qps = prepareStatement(
									connection,
									query,
									statementLogger,
									listeners
							);
							try {
								ResultSet rs = executeQuery( qps, listeners );
								if ( !rs.next() ) {
									String err = "could not read a hi value - you need to populate the table: " + tableName;
									LOG.error( err );
									throw new IdentifierGenerationException( err );
								}
								value.initialize( rs, 1 );
								rs.close();
							}
							catch ( SQLException e ) {
								LOG.error( "Could not read a hi value", e );
								throw e;
							}
							finally {
								qps.close();
							}

							final PreparedStatement ups = prepareStatement(
									connection,
									update,
									statementLogger,
									listeners
							);
							try {
								value.copy().increment().bind( ups, 1 );
								value.bind( ups, 2 );
								rows = executeUpdate( ups, listeners );
							}
							catch ( SQLException sqle ) {
								LOG.error( LOG.unableToUpdateHiValue( tableName ), sqle );
								throw sqle;
							}
							finally {
								ups.close();
							}
						}
						while ( rows == 0 );
						return value;
					}
				},
				true
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

	@Override
	public void registerExportables(Database database) {
		// not doing anything here as I expect this to go away
	}

	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
			dialect.getCreateTableString() + " " + tableName + " ( "
					+ columnName + " " + dialect.getTypeName(Types.INTEGER) + " )" + dialect.getTableTypeString(),
			"insert into " + tableName + " values ( 0 )"
		};
	}

	public String[] sqlDropStrings(Dialect dialect) {
		return new String[] { dialect.getDropTableString( tableName ) };
	}

	public Object generatorKey() {
		return tableName;
	}

	protected IntegralDataTypeHolder buildHolder() {
		return IdentifierGeneratorHelper.getIntegralDataTypeHolder( identifierType.getReturnedClass() );
	}
}
