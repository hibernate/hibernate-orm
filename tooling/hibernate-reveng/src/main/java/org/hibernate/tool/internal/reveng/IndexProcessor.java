package org.hibernate.tool.internal.reveng;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.JDBCException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;

public class IndexProcessor {

	private static final Logger log = Logger.getLogger(IndexProcessor.class);

	public static void processIndices(
			MetaDataDialect metaDataDialect, 
			String  defaultSchema, 
			String defaultCatalog, 
			Table table) {
		
		Map<String, Index> indexes = new HashMap<String, Index>(); // indexname (String) -> Index
		Map<String, UniqueKey> uniquekeys = new HashMap<String, UniqueKey>(); // name (String) -> UniqueKey
		Map<Column, List<UniqueKey>> uniqueColumns = new HashMap<Column, List<UniqueKey>>(); // Column -> List<Index>
		
		Iterator<Map<String, Object>> indexIterator = null;
		try {
			Map<String,Object> indexRs = null;	
			indexIterator = metaDataDialect.getIndexInfo(getCatalogForDBLookup(table.getCatalog(), defaultCatalog), getSchemaForDBLookup(table.getSchema(), defaultSchema), table.getName());
			
			while (indexIterator.hasNext() ) {
				indexRs = indexIterator.next();
				String indexName = (String) indexRs.get("INDEX_NAME");
				String columnName = (String) indexRs.get("COLUMN_NAME");
				boolean unique = !((Boolean)indexRs.get("NON_UNIQUE")).booleanValue();
				
				if (columnName != null || indexName != null) { // both can be non-null with statistical indexs which we don't have any use for.
					
					if(unique) {
						UniqueKey key = uniquekeys.get(indexName);
						if (key==null) {
							key = new UniqueKey();
							key.setName(indexName);
							key.setTable(table);
							table.addUniqueKey(key);							
							uniquekeys.put(indexName, key);
						}
				
						if(indexes.containsKey(indexName) ) {
							throw new JdbcBinderException("UniqueKey exists also as Index! ");
						}
						Column column = getColumn(metaDataDialect, table, columnName);
						key.addColumn(column);
						
						if (unique && key.getColumnSpan()==1) {
							// make list of columns that has the chance of being unique
							List<UniqueKey> l = uniqueColumns.get(column);
							if (l == null) {
								l = new ArrayList<UniqueKey>();
								uniqueColumns.put(column, l);
							}
							l.add(key);
						}
					} 
					else {
						Index index = indexes.get(indexName);
						if(index==null) {
							index = new Index();
							index.setName(indexName);
							index.setTable(table);
							table.addIndex(index);
							indexes.put(indexName, index);					
						}
						
						if(uniquekeys.containsKey(indexName) ) {
							throw new JdbcBinderException("Index exists also as Unique! ");
						}
						Column column = getColumn(metaDataDialect, table, columnName);
						index.addColumn(column);
					}
					
				} 
				else {
					if(DatabaseMetaData.tableIndexStatistic != ((Short)indexRs.get("TYPE")).shortValue() ) {
						log.warn("Index was not statistical, but no column name was found in " + indexName);
					}
						
				}								
			}
		} 
		catch (JDBCException t) {
			log.warn("Exception while trying to get indexinfo on " + TableNameQualifier.qualify(table.getCatalog(), table.getSchema(), table.getName() ) +  "=" + t.getMessage() );
			// Bug #604761 Oracle getIndexInfo() needs major grants And other dbs sucks too ;)
			// http://sourceforge.net/tracker/index.php?func=detail&aid=604761&group_id=36044&atid=415990				
		} 
		finally {
			if (indexIterator != null) {
				try {
					metaDataDialect.close(indexIterator);
				} catch(JDBCException se) {
					log.warn("Exception while trying to close resultset for index meta data",se);
				}
			}
		}
		
		// mark columns that are unique TODO: multiple columns are not unique on their own.
		Iterator<Entry<Column, List<UniqueKey>>> uniqueColumnIterator = uniqueColumns.entrySet().iterator();
		while (uniqueColumnIterator.hasNext() ) {
			Entry<Column, List<UniqueKey>> entry = uniqueColumnIterator.next();
			Column col = entry.getKey();
			Iterator<UniqueKey> keys = entry.getValue().iterator();
			 while (keys.hasNext() ) {
				UniqueKey key = keys.next();
			
				if(key.getColumnSpan()==1) {
					col.setUnique(true);
				}
			}
		}
		
		Iterator<Entry<String, UniqueKey>> iterator = uniquekeys.entrySet().iterator();
		while(iterator.hasNext()) {
			// if keyset has no overlaps with primary key (table.getPrimaryKey())
			// if only key matches then mark as setNaturalId(true);
			iterator.next();
		}
	}
	
	private static String getCatalogForDBLookup(String catalog, String defaultCatalog) {
		return catalog==null?defaultCatalog:catalog;			
	}

	private static String getSchemaForDBLookup(String schema, String defaultSchema) {
		return schema==null?defaultSchema:schema;
	}

	private static Column getColumn(MetaDataDialect metaDataDialect, Table table, String columnName) {
		Column column = new Column();
		column.setName(quote(columnName, metaDataDialect));
		Column existing = table.getColumn(column);
		if(existing!=null) {
			column = existing;
		}
		return column;
	}	
	
	private static String quote(String columnName, MetaDataDialect metaDataDialect) {
		   if(columnName==null) return columnName;
		   if(metaDataDialect.needQuote(columnName)) {
			   if(columnName.length()>1 && columnName.charAt(0)=='`' && columnName.charAt(columnName.length()-1)=='`') {
				   return columnName; // avoid double quoting
			   }
			   return "`" + columnName + "`";
		   } else {
			   return columnName;
		   }		
	}

}
