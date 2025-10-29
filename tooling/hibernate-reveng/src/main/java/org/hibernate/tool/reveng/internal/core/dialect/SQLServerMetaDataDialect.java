/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.core.dialect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author ddukker
 *
 */
public class SQLServerMetaDataDialect extends JDBCMetaDataDialect {

	public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
		String sql = null;
			try {			
				catalog = caseForSearch( catalog );
				schema = caseForSearch( schema );
				table = caseForSearch( table );
				
				log.debug("geSuggestedPrimaryKeyStrategyName(" + catalog + "." + schema + "." + table + ")");

				sql = "SELECT a.TABLE_CATALOG, a.TABLE_SCHEMA, a.TABLE_NAME as table_name, c.DATA_TYPE as data_type, b.CONSTRAINT_TYPE,  OBJECTPROPERTY(OBJECT_ID(a.TABLE_NAME),'TableHasIdentity') as hasIdentity " +
					  "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE a " +
					  "INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS b on a.CONSTRAINT_NAME = b.CONSTRAINT_NAME " +
					  "INNER JOIN INFORMATION_SCHEMA.COLUMNS c on a.TABLE_CATALOG = c.TABLE_CATALOG AND a.TABLE_SCHEMA = c.TABLE_SCHEMA AND a.TABLE_NAME = c.TABLE_NAME AND a.COLUMN_NAME = c.COLUMN_NAME " +
					  "WHERE a.TABLE_NAME=? AND a.TABLE_SCHEMA=? AND a.TABLE_CATALOG=? AND b.CONSTRAINT_TYPE = 'Primary key'";

				PreparedStatement statement = getConnection().prepareStatement( sql );
				statement.setString(1, table);
				statement.setString( 2, schema );
				statement.setString( 3, catalog );
				
				final String sc = schema;
				final String cat = catalog;
				return new ResultSetIterator(statement.executeQuery()) {
					
					final Map<String, Object> element = new HashMap<String, Object>();
					protected Map<String, Object> convertRow(ResultSet tableRs) throws SQLException {
						element.clear();
						element.put("TABLE_NAME", tableRs.getString("table_name"));
						element.put("TABLE_SCHEM", sc);
						element.put("TABLE_CAT", cat);
						
						String string = tableRs.getString("data_type");
						
						boolean bool = tableRs.getBoolean("hasIdentity");
						if(string!=null) {
							if(string.equalsIgnoreCase("uniqueidentifier")){
								element.put("HIBERNATE_STRATEGY", "guid");
							}else if(bool){
								element.put("HIBERNATE_STRATEGY", "identity");
							}else{
								element.put("HIBERNATE_STRATEGY", null);
							} 
						}else {
							element.put("HIBERNATE_STRATEGY", null);
						}
						return element;					
					}
					protected Throwable handleSQLException(SQLException e) {
						// schemaRs and catalogRs are only used for error reporting if
						// we get an exception
						throw new RuntimeException(
								"Could not get list of suggested identity strategies from database. Probably a JDBC driver problem. ", e);					
					}
				};
			} catch (SQLException e) {
				throw new RuntimeException(
						"Could not get list of suggested identity strategies from database. Probably a JDBC driver problem. ", e);		         
			} 		
		}

	}
	
