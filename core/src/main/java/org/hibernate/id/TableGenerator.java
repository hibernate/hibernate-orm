//$Id: TableGenerator.java 11303 2007-03-19 22:06:14Z steve.ebersole@jboss.com $
package org.hibernate.id;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TransactionHelper;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;

/**
 * An <tt>IdentifierGenerator</tt> that uses a database
 * table to store the last generated value. It is not
 * intended that applications use this strategy directly.
 * However, it may be used to build other (efficient)
 * strategies. The returned type is <tt>Integer</tt>.<br>
 * <br>
 * The hi value MUST be fetched in a seperate transaction
 * to the <tt>Session</tt> transaction so the generator must
 * be able to obtain a new connection and commit it. Hence
 * this implementation may not be used when Hibernate is
 * fetching connections  when the user is supplying
 * connections.<br>
 * <br>
 * The returned value is of type <tt>integer</tt>.<br>
 * <br>
 * Mapping parameters supported: table, column
 *
 * @see TableHiLoGenerator
 * @author Gavin King
 */
public class TableGenerator extends TransactionHelper
	implements PersistentIdentifierGenerator, Configurable {
	/* COLUMN and TABLE should be renamed but it would break the public API */
	/** The column parameter */
	public static final String COLUMN = "column";
	
	/** Default column name */
	public static final String DEFAULT_COLUMN_NAME = "next_hi";
	
	/** The table parameter */
	public static final String TABLE = "table";
	
	/** Default table name */	
	public static final String DEFAULT_TABLE_NAME = "hibernate_unique_key";

	private static final Log log = LogFactory.getLog(TableGenerator.class);

	private String tableName;
	private String columnName;
	private String query;
	private String update;

	public void configure(Type type, Properties params, Dialect dialect) {

		tableName = PropertiesHelper.getString(TABLE, params, DEFAULT_TABLE_NAME);
		columnName = PropertiesHelper.getString(COLUMN, params, DEFAULT_COLUMN_NAME);
		String schemaName = params.getProperty(SCHEMA);
		String catalogName = params.getProperty(CATALOG);

		if ( tableName.indexOf( '.' )<0 ) {
			tableName = Table.qualify( catalogName, schemaName, tableName );
		}

		query = "select " + 
			columnName + 
			" from " + 
			dialect.appendLockHint(LockMode.UPGRADE, tableName) +
			dialect.getForUpdateString();

		update = "update " + 
			tableName + 
			" set " + 
			columnName + 
			" = ? where " + 
			columnName + 
			" = ?";
	}

	public synchronized Serializable generate(SessionImplementor session, Object object)
		throws HibernateException {
		int result = ( (Integer) doWorkInNewTransaction(session) ).intValue();
		return new Integer(result);
	}


	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
			dialect.getCreateTableString() + " " + tableName + " ( " + columnName + " " + dialect.getTypeName(Types.INTEGER) + " )",
			"insert into " + tableName + " values ( 0 )"
		};
	}

	public String[] sqlDropStrings(Dialect dialect) {
		StringBuffer sqlDropString = new StringBuffer( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			sqlDropString.append( "if exists " );
		}
		sqlDropString.append( tableName ).append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
			sqlDropString.append( " if exists" );
		}
		return new String[] { sqlDropString.toString() };
	}

	public Object generatorKey() {
		return tableName;
	}

	public Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException {
		int result;
		int rows;
		do {
			// The loop ensures atomicity of the
			// select + update even for no transaction
			// or read committed isolation level

			sql = query;
			SQL.debug(query);
			PreparedStatement qps = conn.prepareStatement(query);
			try {
				ResultSet rs = qps.executeQuery();
				if ( !rs.next() ) {
					String err = "could not read a hi value - you need to populate the table: " + tableName;
					log.error(err);
					throw new IdentifierGenerationException(err);
				}
				result = rs.getInt(1);
				rs.close();
			}
			catch (SQLException sqle) {
				log.error("could not read a hi value", sqle);
				throw sqle;
			}
			finally {
				qps.close();
			}

			sql = update;
			SQL.debug(update);
			PreparedStatement ups = conn.prepareStatement(update);
			try {
				ups.setInt( 1, result + 1 );
				ups.setInt( 2, result );
				rows = ups.executeUpdate();
			}
			catch (SQLException sqle) {
				log.error("could not update hi value in: " + tableName, sqle);
				throw sqle;
			}
			finally {
				ups.close();
			}
		}
		while (rows==0);
		return new Integer(result);
	}
}
