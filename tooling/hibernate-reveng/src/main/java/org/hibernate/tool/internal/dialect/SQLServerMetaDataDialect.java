package org.hibernate.tool.internal.dialect;

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
						"INNER JOIN INFORMATION_SCHEMA.Columns c on a.TABLE_CATALOG = c.TABLE_CATALOG AND a.TABLE_SCHEMA = c.TABLE_SCHEMA AND a.TABLE_NAME = c.TABLE_NAME AND a.COLUMN_NAME = c.COLUMN_NAME " +
						"WHERE a.TABLE_NAME='"+table+"' AND a.TABLE_SCHEMA='"+schema+"' AND a.TABLE_CATALOG='"+catalog+"' AND b.CONSTRAINT_TYPE = 'Primary key'";
				
				PreparedStatement statement = getConnection().prepareStatement( sql );
				
				final String sc = schema;
				final String cat = catalog;
				return new ResultSetIterator(statement.executeQuery(), getSQLExceptionConverter()) {
					
					Map<String, Object> element = new HashMap<String, Object>();
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
						throw getSQLExceptionConverter().convert( e,
								"Could not get list of suggested identity strategies from database. Probably a JDBC driver problem. ", null);					
					}
				};
			} catch (SQLException e) {
				throw getSQLExceptionConverter().convert(e, "Could not get list of suggested identity strategies from database. Probably a JDBC driver problem. ", sql);		         
			} 		
		}

	}
	
