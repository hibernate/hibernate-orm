package org.hibernate.id.enhanced;

import java.sql.Types;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.dialect.Dialect;
import org.hibernate.LockMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TransactionHelper;
import org.hibernate.id.IdentifierGenerationException;

/**
 * Describes a table used to mimic sequence behavior
 *
 * @author Steve Ebersole
 */
public class TableStructure extends TransactionHelper implements DatabaseStructure {
	private static final Log log = LogFactory.getLog( TableStructure.class );
	private static final Log SQL_LOG = LogFactory.getLog( "org.hibernate.SQL" );

	private final String tableName;
	private final String valueColumnName;
	private final int initialValue;
	private final int incrementSize;
	private final String selectQuery;
	private final String updateQuery;

	private boolean applyIncrementSizeToSourceValues;
	private int accessCounter;

	public TableStructure(Dialect dialect, String tableName, String valueColumnName, int initialValue, int incrementSize) {
		this.tableName = tableName;
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
		this.valueColumnName = valueColumnName;

		selectQuery = "select " + valueColumnName + " id_val" +
				" from " + dialect.appendLockHint( LockMode.UPGRADE, tableName ) +
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
			public long getNextValue() {
				return ( ( Number ) doWorkInNewTransaction( session ) ).longValue();
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

	protected Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException {
		long result;
		int rows;
		do {
			SQL_LOG.debug( selectQuery );
			PreparedStatement selectPS = conn.prepareStatement( selectQuery );
			try {
				ResultSet selectRS = selectPS.executeQuery();
				if ( !selectRS.next() ) {
					String err = "could not read a hi value - you need to populate the table: " + tableName;
					log.error( err );
					throw new IdentifierGenerationException( err );
				}
				result = selectRS.getLong( 1 );
				selectRS.close();
			}
			catch ( SQLException sqle ) {
				log.error( "could not read a hi value", sqle );
				throw sqle;
			}
			finally {
				selectPS.close();
			}

			SQL_LOG.debug( updateQuery );
			PreparedStatement updatePS = conn.prepareStatement( updateQuery );
			try {
				int increment = applyIncrementSizeToSourceValues ? incrementSize : 1;
				updatePS.setLong( 1, result + increment );
				updatePS.setLong( 2, result );
				rows = updatePS.executeUpdate();
			}
			catch ( SQLException sqle ) {
				log.error( "could not update hi value in: " + tableName, sqle );
				throw sqle;
			}
			finally {
				updatePS.close();
			}
		} while ( rows == 0 );

		accessCounter++;

		return new Long( result );
	}

}
