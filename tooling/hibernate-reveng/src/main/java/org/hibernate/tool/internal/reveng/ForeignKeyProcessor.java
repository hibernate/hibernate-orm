package org.hibernate.tool.internal.reveng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.JDBCException;
import org.hibernate.MappingException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.ProgressListener;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;

public class ForeignKeyProcessor {

	private static final Logger log = Logger.getLogger(ForeignKeyProcessor.class);

	public static ForeignKeysInfo processForeignKeys(
			MetaDataDialect metaDataDialect,
			ReverseEngineeringStrategy revengStrategy, 
			String  defaultSchema, 
			String defaultCatalog, 
			DatabaseCollector dbs, 
			Table referencedTable, 
			ProgressListener progress) throws JdbcBinderException {
		// foreign key name to list of columns
		Map<String, List<Column>> dependentColumns = new HashMap<String, List<Column>>();
		// foreign key name to Table
		Map<String, Table> dependentTables = new HashMap<String, Table>();
		Map<String, List<Column>> referencedColumns = new HashMap<String, List<Column>>();
		
		short bogusFkName = 0;
		
		// first get all the relationships dictated by the database schema
		
		Iterator<Map<String, Object>> exportedKeyIterator = null;
		
        log.debug("Calling getExportedKeys on " + referencedTable);
        progress.startSubTask("Finding exported foreignkeys on " + referencedTable.getName());
        try {
        	Map<String, Object> exportedKeyRs = null;
        	exportedKeyIterator = metaDataDialect.getExportedKeys(getCatalogForDBLookup(referencedTable.getCatalog(), defaultCatalog), getSchemaForDBLookup(referencedTable.getSchema(), defaultSchema), referencedTable.getName() );
        try {
			while (exportedKeyIterator.hasNext() ) {
				exportedKeyRs = exportedKeyIterator.next();
				String fkCatalog = getCatalogForModel((String) exportedKeyRs.get("FKTABLE_CAT"), defaultCatalog);
				String fkSchema = getSchemaForModel((String) exportedKeyRs.get("FKTABLE_SCHEM"), defaultSchema);
				String fkTableName = (String) exportedKeyRs.get("FKTABLE_NAME");
				String fkColumnName = (String) exportedKeyRs.get("FKCOLUMN_NAME");
				String pkColumnName = (String) exportedKeyRs.get("PKCOLUMN_NAME");
				String fkName = (String) exportedKeyRs.get("FK_NAME");
				short keySeq = ((Short)exportedKeyRs.get("KEY_SEQ")).shortValue();
								
				Table fkTable = dbs.getTable((String) exportedKeyRs.get("FKTABLE_SCHEM"), (String) exportedKeyRs.get("FKTABLE_CAT"), fkTableName);
				
				if (fkTable == null) {
					fkTable = dbs.getTable(
							getSchemaForModel(fkSchema, defaultSchema), 
							getCatalogForModel(fkCatalog, defaultCatalog), 
							fkTableName);
				}
				
				if(fkTable==null) {
					//	filter out stuff we don't have tables for!
					log.debug("Foreign key " + fkName + " references unknown or filtered table " + TableNameQualifier.qualify(fkCatalog, fkSchema, fkTableName) );
					continue;
				} else {
					log.debug("Foreign key " + fkName);
				}
				
				// TODO: if there is a relation to a column which is not a pk
				//       then handle it as a property-ref
				
				if (keySeq == 0) {
					bogusFkName++;
				}
				
				if (fkName == null) {
					// somehow reuse hibernates name generator ?
					fkName = Short.toString(bogusFkName);
				}
				//Table fkTable = mappings.addTable(fkSchema, fkCatalog, fkTableName, null, false);
				
				
				List<Column> depColumns =  dependentColumns.get(fkName);
				if (depColumns == null) {
					depColumns = new ArrayList<Column>();
					dependentColumns.put(fkName,depColumns);
					dependentTables.put(fkName, fkTable);
				} 
				else {
					Object previousTable = dependentTables.get(fkName);
					if(fkTable != previousTable) {
						throw new JdbcBinderException("Foreign key name (" + fkName + ") mapped to different tables! previous: " + previousTable + " current:" + fkTable);
					}
				}
				
				Column column = new Column(fkColumnName);
				Column existingColumn = fkTable.getColumn(column);
				column = existingColumn==null ? column : existingColumn;
				
				depColumns.add(column);
				
				List<Column> primColumns = referencedColumns.get(fkName);
				if (primColumns == null) {
					primColumns = new ArrayList<Column>();
					referencedColumns.put(fkName,primColumns);					
				} 
				
				Column refColumn = new Column(pkColumnName);
				existingColumn = referencedTable.getColumn(refColumn);
				refColumn = existingColumn==null?refColumn:existingColumn;
				
				primColumns.add(refColumn);
				
			}
		} 
        finally {
        	try {
        		if(exportedKeyIterator!=null) {
        			metaDataDialect.close(exportedKeyIterator);
        		}
        	} catch(JDBCException se) {
        		log.warn("Exception while closing result set for foreign key meta data",se);
        	}
        }
        } catch(JDBCException se) {
        	//throw sec.convert(se, "Exception while reading foreign keys for " + referencedTable, null);
        	log.warn("Exception while reading foreign keys for " + referencedTable + " [" + se.toString() + "]", se);
        	// sybase (and possibly others has issues with exportedkeys) see HBX-411
        	// we continue after this to allow user provided keys to be added.
        }
        
        List<ForeignKey> userForeignKeys = revengStrategy.getForeignKeys(
        		RevEngUtils.createTableIdentifier(referencedTable, defaultCatalog, defaultSchema));
        if(userForeignKeys!=null) {
        	Iterator<ForeignKey> iterator = userForeignKeys.iterator();
        	while ( iterator.hasNext() ) {
        		ForeignKey element = iterator.next();
        		
        		if(!equalTable(referencedTable, element.getReferencedTable(), defaultSchema, defaultCatalog)) {
        			log.debug("Referenced table " + element.getReferencedTable().getName() + " is not " +  referencedTable + ". Ignoring userdefined foreign key " + element );
        			continue; // skip non related foreign keys
        		}
        		
        		String userfkName = element.getName();        
        		Table userfkTable = element.getTable();
        		
        		List<?> userColumns = element.getColumns();
        		List<?> userrefColumns = element.getReferencedColumns();
        		
        		Table deptable = dependentTables.get(userfkName);
        		if(deptable!=null) { // foreign key already defined!?
        			throw new MappingException("Foreign key " + userfkName + " already defined in the database!");
        		}
        		
        		deptable = dbs.getTable(
        				getSchemaForDBLookup(userfkTable.getSchema(), defaultSchema), 
        				getCatalogForDBLookup(userfkTable.getCatalog(), defaultCatalog),
        				userfkTable.getName());
        		if(deptable==null) {
					//	filter out stuff we don't have tables for!
					log.debug("User defined foreign key " + userfkName + " references unknown or filtered table " + TableIdentifier.create(userfkTable) );
					continue;        			
        		}
        		
        		dependentTables.put(userfkName, deptable);
        		
        		List<Column> depColumns = new ArrayList<Column>(userColumns.size() );
        		Iterator<?> colIterator = userColumns.iterator();
        		while(colIterator.hasNext() ) {
        			Column jdbcColumn = (Column) colIterator.next();
        			Column column = new Column(jdbcColumn.getName() );
    				Column existingColumn = deptable.getColumn(column);
    				column = existingColumn==null ? column : existingColumn;
    				depColumns.add(column);
        		}
        		
        		List<Column> refColumns = new ArrayList<Column>(userrefColumns.size() );
        		colIterator = userrefColumns.iterator();
        		while(colIterator.hasNext() ) {
        			Column jdbcColumn = (Column) colIterator.next();
        			Column column = new Column(jdbcColumn.getName() );
    				Column existingColumn = referencedTable.getColumn(column);
    				column = existingColumn==null ? column : existingColumn;
    				refColumns.add(column);
        		}
        		
        		referencedColumns.put(userfkName, refColumns );
        		dependentColumns.put(userfkName, depColumns );
        	}
        }
        
        
        return new ForeignKeysInfo(referencedTable, dependentTables, dependentColumns, referencedColumns);
        
       }

	private static String getCatalogForDBLookup(String catalog, String defaultCatalog) {
		return catalog==null?defaultCatalog:catalog;			
	}

	private static String getSchemaForDBLookup(String schema, String defaultSchema) {
		return schema==null?defaultSchema:schema;
	}

	/** If catalog is equal to defaultCatalog then we return null so it will be null in the generated code. */
	private static String getCatalogForModel(String catalog, String defaultCatalog) {
		if(catalog==null) return null;
		if(catalog.equals(defaultCatalog)) return null;
		return catalog;
	}

	/** If catalog is equal to defaultSchema then we return null so it will be null in the generated code. */
	private static String getSchemaForModel(String schema, String defaultSchema) {
		if(schema==null) return null;
		if(schema.equals(defaultSchema)) return null;
		return schema;
	}
	
    private static boolean equalTable(
    		Table table1, 
    		Table table2, 
    		String defaultSchema, 
    		String defaultCatalog) {
		return  table1.getName().equals(table2.getName()) 
				&& ( equal(
						getSchemaForModel(table1.getSchema(), defaultSchema),
						getSchemaForModel(table2.getSchema(), defaultSchema))
				&& ( equal(
						getCatalogForModel(table1.getCatalog(), defaultCatalog), 
						getCatalogForModel(table2.getCatalog(), defaultCatalog))));
	}

	private static boolean equal(String str, String str2) {
		if(str==str2) return true;
		if(str!=null && str.equals(str2) ) return true;
		return false;
	}

}
