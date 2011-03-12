/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TransactionHelper;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.jdbc.util.FormatStyle;
import org.hibernate.jdbc.util.SQLStatementLogger;

/**
 * Describes a table used to mimic sequence behavior
 *
 * @author Steve Ebersole
 */
public class TableStructure extends TransactionHelper implements DatabaseStructure {
	private static final Logger log = LoggerFactory.getLogger( TableStructure.class );
	private static final SQLStatementLogger SQL_STATEMENT_LOGGER = new SQLStatementLogger( false, false );

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

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return tableName;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getInitialValue() {
		return initialValue;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getIncrementSize() {
		return incrementSize;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTimesAccessed() {
		return accessCounter;
	}

	/**
	 * {@inheritDoc}
	 */
	public void prepare(Optimizer optimizer) {
		applyIncrementSizeToSourceValues = optimizer.applyIncrementSizeToSourceValues();
	}

	/**
	 * {@inheritDoc}
	 */
	public AccessCallback buildCallback(final SessionImplementor session) {
		return new AccessCallback() {
			public IntegralDataTypeHolder getNextValue() {
				return ( IntegralDataTypeHolder ) doWorkInNewTransaction( session );
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
				dialect.getCreateTableString() + " " + tableName + " ( " + valueColumnName + " " + dialect.getTypeName( Types.BIGINT ) + " )",
				"insert into " + tableName + " values ( " + initialValue + " )"
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		StringBuffer sqlDropString = new StringBuffer().append( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			sqlDropString.append( "if exists " );
		}
		sqlDropString.append( tableName ).append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
			sqlDropString.append( " if exists" );
		}
		return new String[] { sqlDropString.toString() };
	}

	/**
	 * {@inheritDoc}
	 */
	protected Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException {
		IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( numberType );
		int rows;
		do {
			SQL_STATEMENT_LOGGER.logStatement( selectQuery, FormatStyle.BASIC );
			PreparedStatement selectPS = conn.prepareStatement( selectQuery );
			try {
				ResultSet selectRS = selectPS.executeQuery();
				if ( !selectRS.next() ) {
					String err = "could not read a hi value - you need to populate the table: " + tableName;
					log.error( err );
					throw new IdentifierGenerationException( err );
				}
				value.initialize( selectRS, 1 );
				selectRS.close();
			}
			catch ( SQLException sqle ) {
				log.error( "could not read a hi value", sqle );
				throw sqle;
			}
			finally {
				selectPS.close();
			}

			SQL_STATEMENT_LOGGER.logStatement( updateQuery, FormatStyle.BASIC );
			PreparedStatement updatePS = conn.prepareStatement( updateQuery );
			try {
				final int increment = applyIncrementSizeToSourceValues ? incrementSize : 1;
				final IntegralDataTypeHolder updateValue = value.copy().add( increment );
				updateValue.bind( updatePS, 1 );
				value.bind( updatePS, 2 );
				rows = updatePS.executeUpdate();
			}
			catch ( SQLException sqle ) {
				log.error( "could not updateQuery hi value in: " + tableName, sqle );
				throw sqle;
			}
			finally {
				updatePS.close();
			}
		} while ( rows == 0 );

		accessCounter++;

		return value;
	}

}
