package org.hibernate.tool.internal.reveng.dialect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.tool.util.ReflectionUtil;


/**
 * MetaData dialect that work around tweaks in the H2 database. 
 * 
 * @author Max Rydahl Andersen
 *
 */
public class H2MetaDataDialect extends JDBCMetaDataDialect {
	
	private static final String SPKSQ_H2_1_X =
			"SELECT " + 
			"  idx.TABLE_CATALOG TABLE_CAT, " + 
			"  idx.TABLE_SCHEMA TABLE_SCHEM, " + 
			"  idx.TABLE_NAME, " + 
			"  idx.COLUMN_NAME, " + 
			"  cols.COLUMN_DEFAULT COLUMN_DEFAULT " + 
			"FROM " +
			"  INFORMATION_SCHEMA.INDEXES idx, " + 
			"  INFORMATION_SCHEMA.COLUMNS cols " +
			"WHERE " +
			"  idx.TABLE_CATALOG = cols.TABLE_CATALOG AND " +
			"  idx.TABLE_SCHEMA = cols.TABLE_SCHEMA AND " +
			"  idx.TABLE_NAME = cols.TABLE_NAME AND " +
            "  idx.PRIMARY_KEY = TRUE AND " +
            "  COLUMN_DEFAULT like '%NEXT VALUE FOR%' ";	
	
	private static final String SPKSQ_H2_2_X =
			"SELECT " +
			"  idx.TABLE_CATALOG TABLE_CAT, " +
			"  idx.TABLE_SCHEMA TABLE_SCHEM, " +
			"  idx.TABLE_NAME, " +
			"  cols.COLUMN_NAME, " +
			"  cols.COLUMN_DEFAULT " +
			"FROM " + 
			"  INFORMATION_SCHEMA.INDEXES idx, " + 
			"  INFORMATION_SCHEMA.COLUMNS cols " +
			"WHERE                                     " +
			"   idx.TABLE_CATALOG = cols.TABLE_CATALOG AND " + 
			"   idx.TABLE_SCHEMA = cols.TABLE_SCHEMA   AND " +
			"   idx.TABLE_NAME = cols.TABLE_NAME AND " + 
			"   idx.INDEX_TYPE_NAME = 'PRIMARY KEY' AND " +
			"   cols.COLUMN_DEFAULT LIKE '%NEXT VALUE FOR%'";

	private static boolean understandsCatalogName = true;
	
	private String suggested_primary_key_strategy_query = null;

	public H2MetaDataDialect() {
		super();
		try {
			Class<?> constants = ReflectionUtil.classForName( "org.h2.engine.Constants" );
			Integer build = (Integer)constants.getDeclaredField( "BUILD_ID" ).get( null );
			if ( build.intValue() < 55 ) {
				understandsCatalogName = false;
			}
			suggested_primary_key_strategy_query = build.intValue() > 200 ? SPKSQ_H2_2_X : SPKSQ_H2_1_X;
		}
        catch( Throwable e ) {
            // ignore (probably H2 not in the classpath)
        }
	}

	protected void putTableType(Map<String, Object> element, ResultSet tableRs) throws SQLException {
		String tableType = tableRs.getString("TABLE_TYPE");
		if ("BASE TABLE".equals(tableType)) {
			tableType = "TABLE";
		}
		element.put("TABLE_TYPE", tableType);
	}

	protected void putTablePart(Map<String, Object> element, ResultSet tableRs) throws SQLException {		
		super.putTablePart( element, tableRs );
		if ( !understandsCatalogName ) {
			element.put( "TABLE_CAT", null );
		}
	}
	
	protected void putExportedKeysPart(Map<String, Object> element, ResultSet rs) throws SQLException {		
		super.putExportedKeysPart( element, rs );
		if ( !understandsCatalogName ) {
			element.put( "PKTABLE_CAT", null );
		}
	}
	
	public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
			try {			
				catalog = caseForSearch( catalog );
				schema = caseForSearch( schema );
				table = caseForSearch( table );
				
				log.debug("geSuggestedPrimaryKeyStrategyName(" + catalog + "." + schema + "." + table + ")");
				
				String sql =  suggested_primary_key_strategy_query;				
				if(catalog!=null) {
					sql += "AND idx.TABLE_CATALOG like '" + catalog + "' ";
				}
				if(schema!=null) { 					
					sql += "AND idx.TABLE_SCHEMA like '" + schema + "' ";
				}
				if(table!=null) {
					sql += "AND idx.TABLE_NAME like '" + table + "' ";
				}
									
				PreparedStatement statement = getConnection().prepareStatement( sql );
				
				return new ResultSetIterator(statement.executeQuery()) {
					
					Map<String, Object> element = new HashMap<String, Object>();
					protected Map<String, Object> convertRow(ResultSet tableRs) throws SQLException {
						element.clear();
						putTablePart( element, tableRs );
						String string = tableRs.getString("COLUMN_DEFAULT");
						element.put("HIBERNATE_STRATEGY", StringHelper.isEmpty( string )?null:"identity");						
						return element;					
					}
					protected Throwable handleSQLException(SQLException e) {
						// schemaRs and catalogRs are only used for error reporting if
						// we get an exception
						throw new RuntimeException(
								"Could not get list of suggested identity strategies from database. Probably a JDBC driver problem. ", e );					
					}
				};
			} catch (SQLException e) {
				throw new RuntimeException("Could not get list of suggested identity strategies from database. Probably a JDBC driver problem.", e);		         
			} 		
	}
}
