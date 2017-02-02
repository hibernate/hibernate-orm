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

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.ExportableColumn;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.mapping.Table;
import org.hibernate.type.LongType;

import org.jboss.logging.Logger;

/**
 * Describes a table used to mimic sequence behavior
 *
 * @author Steve Ebersole
 */
public class TableStructure implements DatabaseStructure {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TableStructure.class.getName()
	);

	private final QualifiedName logicalQualifiedTableName;
	private final Identifier logicalValueColumnNameIdentifier;
	private final int initialValue;
	private final int incrementSize;
	private final Class numberType;

	private String tableNameText;
	private String valueColumnNameText;

	private String selectQuery;
	private String updateQuery;

	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;

	public TableStructure(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName qualifiedTableName,
			Identifier valueColumnNameIdentifier,
			int initialValue,
			int incrementSize,
			Class numberType) {
		this.logicalQualifiedTableName = qualifiedTableName;
		this.logicalValueColumnNameIdentifier = valueColumnNameIdentifier;

		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.numberType = numberType;
	}

	@Override
	public String getName() {
		return tableNameText;
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

	@Override
	public void prepare(Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	private IntegralDataTypeHolder makeValue() {
		return IdentifierGeneratorHelper.getIntegralDataTypeHolder( numberType );
	}

	@Override
	public AccessCallback buildCallback(final SharedSessionContractImplementor session) {
		final SqlStatementLogger statementLogger = session.getFactory().getServiceRegistry()
				.getService( JdbcServices.class )
				.getSqlStatementLogger();
		if ( selectQuery == null || updateQuery == null ) {
			throw new AssertionFailure( "SequenceStyleGenerator's TableStructure was not properly initialized" );
		}

		final SessionEventListenerManager statsCollector = session.getEventListenerManager();

		return new AccessCallback() {
			@Override
			public IntegralDataTypeHolder getNextValue() {
				return session.getTransactionCoordinator().createIsolationDelegate().delegateWork(
						new AbstractReturningWork<IntegralDataTypeHolder>() {
							@Override
							public IntegralDataTypeHolder execute(Connection connection) throws SQLException {
								final IntegralDataTypeHolder value = makeValue();
								int rows;
								do {
									try (PreparedStatement selectStatement = prepareStatement(
											connection,
											selectQuery,
											statementLogger,
											statsCollector
									)) {
										final ResultSet selectRS = executeQuery( selectStatement, statsCollector );
										if ( !selectRS.next() ) {
											final String err = "could not read a hi value - you need to populate the table: " + tableNameText;
											LOG.error( err );
											throw new IdentifierGenerationException( err );
										}
										value.initialize( selectRS, 1 );
										selectRS.close();
									}
									catch (SQLException sqle) {
										LOG.error( "could not read a hi value", sqle );
										throw sqle;
									}


									try (PreparedStatement updatePS = prepareStatement(
											connection,
											updateQuery,
											statementLogger,
											statsCollector
									)) {
										final int increment = applyIncrementSizeToSourceValues ? incrementSize : 1;
										final IntegralDataTypeHolder updateValue = value.copy().add( increment );
										updateValue.bind( updatePS, 1 );
										value.bind( updatePS, 2 );
										rows = executeUpdate( updatePS, statsCollector );
									}
									catch (SQLException e) {
										LOG.unableToUpdateQueryHiValue( tableNameText, e );
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
			public String getTenantIdentifier() {
				return session.getTenantIdentifier();
			}
		};
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
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
				dialect.getCreateTableString() + " " + tableNameText + " ( " + valueColumnNameText + " " + dialect.getTypeName( Types.BIGINT ) + " )",
				"insert into " + tableNameText + " values ( " + initialValue + " )"
		};
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return new String[] { dialect.getDropTableString( tableNameText ) };
	}

	@Override
	public boolean isPhysicalSequence() {
		return false;
	}

	@Override
	public void registerExportables(Database database) {
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final Namespace namespace = database.locateNamespace(
				logicalQualifiedTableName.getCatalogName(),
				logicalQualifiedTableName.getSchemaName()
		);

		Table table = namespace.locateTable( logicalQualifiedTableName.getObjectName() );
		if ( table == null ) {
			table = namespace.createTable( logicalQualifiedTableName.getObjectName(), false );
		}

		this.tableNameText = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				table.getQualifiedTableName(),
				dialect
		);

		this.valueColumnNameText = logicalValueColumnNameIdentifier.render( dialect );


		this.selectQuery = "select " + valueColumnNameText + " as id_val" +
				" from " + dialect.appendLockHint( LockMode.PESSIMISTIC_WRITE, tableNameText ) +
				dialect.getForUpdateString();

		this.updateQuery = "update " + tableNameText +
				" set " + valueColumnNameText + "= ?" +
				" where " + valueColumnNameText + "=?";

		ExportableColumn valueColumn = new ExportableColumn(
				database,
				table,
				valueColumnNameText,
				LongType.INSTANCE
		);
		table.addColumn( valueColumn );

		table.addInitCommand(
				new InitCommand( "insert into " + tableNameText + " values ( " + initialValue + " )" )
		);
	}
}
