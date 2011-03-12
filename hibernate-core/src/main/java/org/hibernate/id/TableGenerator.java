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
package org.hibernate.id;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.jdbc.util.FormatStyle;
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

	private static final Logger log = LoggerFactory.getLogger(TableGenerator.class);

	private Type identifierType;
	private String tableName;
	private String columnName;
	private String query;
	private String update;

	public void configure(Type type, Properties params, Dialect dialect) {
		identifierType = type;

		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );

		tableName = PropertiesHelper.getString( TABLE, params, DEFAULT_TABLE_NAME );
		if ( tableName.indexOf( '.' ) < 0 ) {
			final String schemaName = normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) );
			final String catalogName = normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) );
			tableName = Table.qualify(
					dialect.quote( catalogName ),
					dialect.quote( schemaName ),
					dialect.quote( tableName )
			);
		}
		else {
			// if already qualified there is not much we can do in a portable manner so we pass it
			// through and assume the user has set up the name correctly.
		}

		columnName = dialect.quote(
				normalizer.normalizeIdentifierQuoting(
						PropertiesHelper.getString( COLUMN, params, DEFAULT_COLUMN_NAME )
				)
		);

		query = "select " + 
			columnName + 
			" from " + 
			dialect.appendLockHint(LockMode.PESSIMISTIC_WRITE, tableName) +
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

	protected IntegralDataTypeHolder generateHolder(SessionImplementor session) {
		return (IntegralDataTypeHolder) doWorkInNewTransaction( session );
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

	/**
	 * Get the next value.
	 *
	 * @param conn The sql connection to use.
	 * @param sql n/a
	 *
	 * @return Prior to 3.5 this method returned an {@link Integer}.  Since 3.5 it now
	 * returns a {@link IntegralDataTypeHolder}
	 *
	 * @throws SQLException
	 */
	public Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException {
		IntegralDataTypeHolder value = buildHolder();
		int rows;
		do {
			// The loop ensures atomicity of the
			// select + update even for no transaction
			// or read committed isolation level

			sql = query;
			SQL_STATEMENT_LOGGER.logStatement( sql, FormatStyle.BASIC );
			PreparedStatement qps = conn.prepareStatement(query);
			try {
				ResultSet rs = qps.executeQuery();
				if ( !rs.next() ) {
					String err = "could not read a hi value - you need to populate the table: " + tableName;
					log.error(err);
					throw new IdentifierGenerationException(err);
				}
				value.initialize( rs, 1 );
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
			SQL_STATEMENT_LOGGER.logStatement( sql, FormatStyle.BASIC );
			PreparedStatement ups = conn.prepareStatement(update);
			try {
				value.copy().increment().bind( ups, 1 );
				value.bind( ups, 2 );
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
		return value;
	}

	protected IntegralDataTypeHolder buildHolder() {
		return IdentifierGeneratorHelper.getIntegralDataTypeHolder( identifierType.getReturnedClass() );
	}
}
