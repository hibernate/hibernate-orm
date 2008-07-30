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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.mapping.Table;
import org.hibernate.dialect.Dialect;
import org.hibernate.util.StringHelper;

/**
 * JDBC database metadata
 * @author Christoph Sturm, Teodor Danciu
 */
public class DatabaseMetadata {
	
	private static final Logger log = LoggerFactory.getLogger(DatabaseMetadata.class);
	
	private final Map tables = new HashMap();
	private final Set sequences = new HashSet();
	private final boolean extras;

	private DatabaseMetaData meta;
	private SQLExceptionConverter sqlExceptionConverter;

	public DatabaseMetadata(Connection connection, Dialect dialect) throws SQLException {
		this(connection, dialect, true);
	}

	public DatabaseMetadata(Connection connection, Dialect dialect, boolean extras) throws SQLException {
		sqlExceptionConverter = dialect.buildSQLExceptionConverter();
		meta = connection.getMetaData();
		this.extras = extras;
		initSequences(connection, dialect);
	}

	private static final String[] TYPES = {"TABLE", "VIEW"};

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
						rs = meta.getTables(catalog, schema, name, TYPES);
					} else if ( (isQuoted && meta.storesUpperCaseQuotedIdentifiers()) 
						|| (!isQuoted && meta.storesUpperCaseIdentifiers() )) {
						rs = meta.getTables( 
								StringHelper.toUpperCase(catalog), 
								StringHelper.toUpperCase(schema), 
								StringHelper.toUpperCase(name), 
								TYPES 
							);
					}
					else if ( (isQuoted && meta.storesLowerCaseQuotedIdentifiers())
							|| (!isQuoted && meta.storesLowerCaseIdentifiers() )) {
						rs = meta.getTables( 
								StringHelper.toLowerCase(catalog), 
								StringHelper.toLowerCase(schema), 
								StringHelper.toLowerCase(name), 
								TYPES 
							);
					}
					else {
						rs = meta.getTables(catalog, schema, name, TYPES);
					}
					
					while ( rs.next() ) {
						String tableName = rs.getString("TABLE_NAME");
						if ( name.equalsIgnoreCase(tableName) ) {
							table = new TableMetadata(rs, meta, extras);
							tables.put(identifier, table);
							return table;
						}
					}
					
					log.info("table not found: " + name);
					return null;

				}
				finally {
					if (rs!=null) rs.close();
				}
			}
			catch (SQLException sqle) {
				throw JDBCExceptionHelper.convert(
                        sqlExceptionConverter,
				        sqle,
				        "could not get table metadata: " + name
					);
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
						sequences.add( rs.getString(1).toLowerCase().trim() );
					}
				}
				finally {
					if (rs!=null) rs.close();
					if (statement!=null) statement.close();
				}
				
			}
		}
	}

	public boolean isSequence(Object key) {
		if (key instanceof String){
			String[] strings = StringHelper.split(".", (String) key);
			return sequences.contains( strings[strings.length-1].toLowerCase());
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
 	
	public String toString() {
		return "DatabaseMetadata" + tables.keySet().toString() + sequences.toString();
	}
}





