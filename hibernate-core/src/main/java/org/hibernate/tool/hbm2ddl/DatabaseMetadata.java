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
package org.hibernate.tool.hbm2ddl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Table;
import org.jboss.logging.Logger;

/**
 * JDBC database metadata
 * @author Christoph Sturm, Teodor Danciu
 * @author Brett Meyer
 */
public class DatabaseMetadata {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, DatabaseMetaData.class.getName());

	private final Map tables = new HashMap();
	private final Set sequences = new HashSet();
	private final boolean extras;

	private DatabaseMetaData meta;
	private SqlExceptionHelper sqlExceptionHelper;

	private final String[] types;
	/**
	 * @deprecated Use {@link #DatabaseMetadata(Connection, Dialect, Configuration)} instead
	 */
	@Deprecated
	public DatabaseMetadata(Connection connection, Dialect dialect) throws SQLException {
		this(connection, dialect, null, true);
	}

	/**
	 * @deprecated Use {@link #DatabaseMetadata(Connection, Dialect, Configuration, boolean)} instead
	 */
	@Deprecated
	public DatabaseMetadata(Connection connection, Dialect dialect, boolean extras) throws SQLException {
		this(connection, dialect, null, extras);
	}
	
	public DatabaseMetadata(Connection connection, Dialect dialect, Configuration config) throws SQLException {
		this(connection, dialect, config, true);
	}

	public DatabaseMetadata(Connection connection, Dialect dialect, Configuration config, boolean extras)
			throws SQLException {
		// TODO: Duplicates JdbcEnvironmentImpl#buildSqlExceptionHelper
		final StandardSQLExceptionConverter sqlExceptionConverter = new StandardSQLExceptionConverter();
		sqlExceptionConverter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
		sqlExceptionConverter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
		// todo : vary this based on extractedMetaDataSupport.getSqlStateType()
		sqlExceptionConverter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		sqlExceptionHelper = new SqlExceptionHelper( sqlExceptionConverter );
		
		meta = connection.getMetaData();
		this.extras = extras;
		initSequences( connection, dialect );
		if ( config != null
				&& ConfigurationHelper.getBoolean( AvailableSettings.ENABLE_SYNONYMS, config.getProperties(), false ) ) {
			types = new String[] { "TABLE", "VIEW", "SYNONYM" };
		}
		else {
			types = new String[] { "TABLE", "VIEW" };
		}
	}

	public TableMetadata getTableMetadata(String name, String schema, String catalog, boolean isQuoted) throws HibernateException {

		Object identifier = identifier(catalog, schema, name);
		TableMetadata table = (TableMetadata) tables.get(identifier);
		if (table!=null) {
			return table;
		}
		else {

			try {
				ResultSet rs = null;
				try {
					if ( (isQuoted && meta.storesMixedCaseQuotedIdentifiers())) {
						rs = meta.getTables(catalog, schema, name, types);
					} else if ( (isQuoted && meta.storesUpperCaseQuotedIdentifiers())
						|| (!isQuoted && meta.storesUpperCaseIdentifiers() )) {
						rs = meta.getTables(
								StringHelper.toUpperCase(catalog),
								StringHelper.toUpperCase(schema),
								StringHelper.toUpperCase(name),
								types
							);
					}
					else if ( (isQuoted && meta.storesLowerCaseQuotedIdentifiers())
							|| (!isQuoted && meta.storesLowerCaseIdentifiers() )) {
						rs = meta.getTables( 
								StringHelper.toLowerCase( catalog ),
								StringHelper.toLowerCase(schema), 
								StringHelper.toLowerCase(name), 
								types 
							);
					}
					else {
						rs = meta.getTables(catalog, schema, name, types);
					}

					while ( rs.next() ) {
						String tableName = rs.getString("TABLE_NAME");
						if ( name.equalsIgnoreCase(tableName) ) {
							table = new TableMetadata(rs, meta, extras);
							tables.put(identifier, table);
							return table;
						}
					}

					LOG.tableNotFound( name );
					return null;

				}
				finally {
					if ( rs != null ) {
						rs.close();
					}
				}
			}
			catch (SQLException sqlException) {
				throw sqlExceptionHelper.convert( sqlException, "could not get table metadata: " + name );
			}
		}

	}

	private Object identifier(String catalog, String schema, String name) {
		return Table.qualify(catalog,schema,name);
	}

	private void initSequences(Connection connection, Dialect dialect) throws SQLException {
		if ( dialect.supportsSequences() ) {
			String sql = dialect.getQuerySequencesString();
			if (sql!=null) {

				Statement statement = null;
				ResultSet rs = null;
				try {
					statement = connection.createStatement();
					rs = statement.executeQuery(sql);

					while ( rs.next() ) {
						sequences.add( StringHelper.toLowerCase(rs.getString(1)).trim() );
					}
				}
				finally {
					if ( rs != null ) {
						rs.close();
					}
					if ( statement != null ) {
						statement.close();
					}
				}
			}
		}
	}

	public boolean isSequence(Object key) {
		if (key instanceof String){
			String[] strings = StringHelper.split(".", (String) key);
			return sequences.contains( StringHelper.toLowerCase(strings[strings.length-1]));
		}
		return false;
	}

	public boolean isTable(Object key) throws HibernateException {
		if(key instanceof String) {
			Table tbl = new Table((String)key);
			if ( getTableMetadata( tbl.getName(), tbl.getSchema(), tbl.getCatalog(), tbl.isQuoted() ) != null ) {
				return true;
			} else {
				String[] strings = StringHelper.split(".", (String) key);
				if(strings.length==3) {
					tbl = new Table(strings[2]);
					tbl.setCatalog(strings[0]);
					tbl.setSchema(strings[1]);
					return getTableMetadata( tbl.getName(), tbl.getSchema(), tbl.getCatalog(), tbl.isQuoted() ) != null;
				} else if (strings.length==2) {
					tbl = new Table(strings[1]);
					tbl.setSchema(strings[0]);
					return getTableMetadata( tbl.getName(), tbl.getSchema(), tbl.getCatalog(), tbl.isQuoted() ) != null;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "DatabaseMetadata" + tables.keySet().toString() + sequences.toString();
	}
}
