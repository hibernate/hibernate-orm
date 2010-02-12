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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.jdbc.util.FormatStyle;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TransactionHelper;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;

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
public class MultipleHiLoPerTableGenerator 
	extends TransactionHelper
	implements PersistentIdentifierGenerator, Configurable {
	
	private static final Logger log = LoggerFactory.getLogger(MultipleHiLoPerTableGenerator.class);
	
	public static final String ID_TABLE = "table";
	public static final String PK_COLUMN_NAME = "primary_key_column";
	public static final String PK_VALUE_NAME = "primary_key_value";
	public static final String VALUE_COLUMN_NAME = "value_column";
	public static final String PK_LENGTH_NAME = "primary_key_length";

	private static final int DEFAULT_PK_LENGTH = 255;
	public static final String DEFAULT_TABLE = "hibernate_sequences";
	private static final String DEFAULT_PK_COLUMN = "sequence_name";
	private static final String DEFAULT_VALUE_COLUMN = "sequence_next_hi_value";
	
	private String tableName;
	private String pkColumnName;
	private String valueColumnName;
	private String query;
	private String insert;
	private String update;

	//hilo params
	public static final String MAX_LO = "max_lo";

	private int maxLo;
	private int lo;
	private IntegralDataTypeHolder value;

	private Class returnClass;
	private int keySize;


	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
			new StringBuffer( dialect.getCreateTableString() )
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
					.append( " ) " )
					.toString()
		};
	}

	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
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
		IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( returnClass );
		int rows;
		do {
			// The loop ensures atomicity of the
			// select + update even for no transaction
			// or read committed isolation level

			//sql = query;
			SQL_STATEMENT_LOGGER.logStatement( sql, FormatStyle.BASIC );
			PreparedStatement qps = conn.prepareStatement(query);
			PreparedStatement ips = null;
			try {
				ResultSet rs = qps.executeQuery();
				boolean isInitialized = rs.next();
				if ( !isInitialized ) {
					value.initialize( 0 );
					ips = conn.prepareStatement( insert );
					value.bind( ips, 1 );
					ips.execute();
				}
				else {
					value.initialize( rs, 0 );
				}
				rs.close();
			}
			catch (SQLException sqle) {
				log.error("could not read or init a hi value", sqle);
				throw sqle;
			}
			finally {
				if (ips != null) {
					ips.close();
				}
				qps.close();
			}

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

	public synchronized Serializable generate(SessionImplementor session, Object obj)
		throws HibernateException {
		// maxLo < 1 indicates a hilo generator with no hilo :?
		if ( maxLo < 1 ) {
			//keep the behavior consistent even for boundary usages
			IntegralDataTypeHolder value = null;
			while ( value == null || value.lt( 1 ) ) {
				value = (IntegralDataTypeHolder) doWorkInNewTransaction( session );
			}
			return value.makeValue();
		}

		if ( lo > maxLo ) {
			IntegralDataTypeHolder hiVal = (IntegralDataTypeHolder) doWorkInNewTransaction( session );
			lo = ( hiVal.eq( 0 ) ) ? 1 : 0;
			value = hiVal.copy().multiplyBy( maxLo+1 ).add( lo );
			if ( log.isDebugEnabled() ) {
				log.debug("new hi value: " + hiVal);
			}
		}
		return value.makeValueThenIncrement();
	}

	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );

		tableName = normalizer.normalizeIdentifierQuoting( PropertiesHelper.getString( ID_TABLE, params, DEFAULT_TABLE ) );
		if ( tableName.indexOf( '.' ) < 0 ) {
			tableName = dialect.quote( tableName );
			final String schemaName = dialect.quote(
					normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) )
			);
			final String catalogName = dialect.quote(
					normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) )
			);
			tableName = Table.qualify( catalogName, schemaName, tableName );
		}
		else {
			// if already qualified there is not much we can do in a portable manner so we pass it
			// through and assume the user has set up the name correctly.
		}

		pkColumnName = dialect.quote(
				normalizer.normalizeIdentifierQuoting(
						PropertiesHelper.getString( PK_COLUMN_NAME, params, DEFAULT_PK_COLUMN )
				)
		);
		valueColumnName = dialect.quote(
				normalizer.normalizeIdentifierQuoting(
						PropertiesHelper.getString( VALUE_COLUMN_NAME, params, DEFAULT_VALUE_COLUMN )
				)
		);
		keySize = PropertiesHelper.getInt(PK_LENGTH_NAME, params, DEFAULT_PK_LENGTH);
		String keyValue = PropertiesHelper.getString(PK_VALUE_NAME, params, params.getProperty(TABLE) );

		query = "select " +
			valueColumnName +
			" from " +
			dialect.appendLockHint( LockMode.PESSIMISTIC_WRITE, tableName ) +
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
		maxLo = PropertiesHelper.getInt(MAX_LO, params, Short.MAX_VALUE);
		lo = maxLo + 1; // so we "clock over" on the first invocation
		returnClass = type.getReturnedClass();
	}
}
