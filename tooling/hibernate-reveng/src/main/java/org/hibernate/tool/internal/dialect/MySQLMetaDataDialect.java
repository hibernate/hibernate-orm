package org.hibernate.tool.internal.dialect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MySQLMetaDataDialect extends JDBCMetaDataDialect {

	/**
	 * Based on info from http://dev.mysql.com/doc/refman/5.0/en/show-table-status.html
	 * Should work on pre-mysql 5 too since it uses the "old" SHOW TABLE command instead of SELECT from infotable.
	 */
	public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
		String sql = null;
			try {			
				catalog = caseForSearch( catalog );
				schema = caseForSearch( schema );
				table = caseForSearch( table );
				
				log.debug("geSuggestedPrimaryKeyStrategyName(" + catalog + "." + schema + "." + table + ")");
				
				sql = "show table status " + (catalog==null?"":" from " + catalog + " ") + (table==null?"":" like '" + table + "' ");
				PreparedStatement statement = getConnection().prepareStatement( sql );
				
				final String sc = schema;
				final String cat = catalog;
				return new ResultSetIterator(statement.executeQuery(), getSQLExceptionConverter()) {
					
					Map<String, Object> element = new HashMap<String, Object>();
					protected Map<String, Object> convertRow(ResultSet tableRs) throws SQLException {
						element.clear();
						element.put("TABLE_NAME", tableRs.getString("NAME"));
						element.put("TABLE_SCHEM", sc);
						element.put("TABLE_CAT", cat);
						
						String string = tableRs.getString("AUTO_INCREMENT");
						if(string==null) {
							element.put("HIBERNATE_STRATEGY", null);
						} else {
							element.put("HIBERNATE_STRATEGY", "identity");
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
	
	@Override
	public Iterator<Map<String,Object>> getTables(
			String xcatalog, 
			String xschema, 
			String xtable) {
	     // MySql JDBC Driver doesn't like 'null' values for the table search pattern, use '%' instead
		return super.getTables(xcatalog, xschema, xtable != null ? xtable : "%");
	}

	public Iterator<Map<String, Object>> getColumns(
			String xcatalog, 
			String xschema, 
			String xtable, 
			String xcolumn) {
	     // MySql JDBC Driver doesn't like 'null' values for the table and column search patterns, use '%' instead
		return super.getColumns(
				xcatalog, 
				xschema, 
				xtable != null ? xtable : "%",
				xcolumn != null ? xcolumn : "%");
	}
	
}
	
