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
package org.hibernate.id.enhanced;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.AbstractReturningWork;

/**
 * Describes a table used to mimic sequence behavior
 *
 * @author Steve Ebersole
 */
public class TableStructure implements DatabaseStructure {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TableStructure.class.getName());

	private final String tableName;
	private final String valueColumnName;
	private final int initialValue;
	private final int incrementSize;
	private final Class numberType;
	private final String selectQuery;
	private final String updateQuery;

	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;

	public TableStructure(
			Dialect dialect,
			String tableName,
			String valueColumnName,
			int initialValue,
			int incrementSize,
			Class numberType) {
		this.tableName = tableName;
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.valueColumnName = valueColumnName;
		this.numberType = numberType;

		selectQuery = "select " + valueColumnName + " as id_val" +
				" from " + dialect.appendLockHint( LockMode.PESSIMISTIC_WRITE, tableName ) +
				dialect.getForUpdateString();

		updateQuery = "update " + tableName +
				" set " + valueColumnName + "= ?" +
				" where " + valueColumnName + "=?";
	}

	@Override
	public String getName() {
		return tableName;
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

	@Override
	public AccessCallback buildCallback(final SessionImplementor session) {
		return new AccessCallback() {
			@Override
			public IntegralDataTypeHolder getNextValue() {
				return session.getTransactionCoordinator().getTransaction().createIsolationDelegate().delegateWork(
						new AbstractReturningWork<IntegralDataTypeHolder>() {
							@Override
							public IntegralDataTypeHolder execute(Connection connection) throws SQLException {
								final SqlStatementLogger statementLogger = session
										.getFactory()
										.getServiceRegistry()
										.getService( JdbcServices.class )
										.getSqlStatementLogger();
								IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( numberType );
								int rows;
								do {
									statementLogger.logStatement( selectQuery, FormatStyle.BASIC.getFormatter() );
									PreparedStatement selectStatement = connection.prepareStatement( selectQuery );
									try {
										ResultSet selectRS = selectStatement.executeQuery();
										if ( !selectRS.next() ) {
											String err = "could not read a hi value - you need to populate the table: " + tableName;
											LOG.error( err );
											throw new IdentifierGenerationException( err );
										}
										value.initialize( selectRS, 1 );
										selectRS.close();
									}
									catch ( SQLException sqle ) {
										LOG.error( "could not read a hi value", sqle );
										throw sqle;
									}
									finally {
										selectStatement.close();
									}

									statementLogger.logStatement( updateQuery, FormatStyle.BASIC.getFormatter() );
									PreparedStatement updatePS = connection.prepareStatement( updateQuery );
									try {
										final int increment = applyIncrementSizeToSourceValues ? incrementSize : 1;
										final IntegralDataTypeHolder updateValue = value.copy().add( increment );
										updateValue.bind( updatePS, 1 );
										value.bind( updatePS, 2 );
										rows = updatePS.executeUpdate();
									}
									catch ( SQLException e ) {
									    LOG.unableToUpdateQueryHiValue(tableName, e);
										throw e;
									}
									finally {
										updatePS.close();
									}
								} while ( rows == 0 );

								accessCounter++;

								return value;
							}
						},
						true
				);
			}
		};
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
				dialect.getCreateTableString() + " " + tableName + " ( " + valueColumnName + " " + dialect.getTypeName( Types.BIGINT ) + " )",
				"insert into " + tableName + " values ( " + initialValue + " )"
		};
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		StringBuilder sqlDropString = new StringBuilder().append( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			sqlDropString.append( "if exists " );
		}
		sqlDropString.append( tableName ).append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
			sqlDropString.append( " if exists" );
		}
		return new String[] { sqlDropString.toString() };
	}

	@Override
	public boolean isPhysicalSequence() {
		return false;
	}
}
