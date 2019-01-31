package org.hibernate.tool.internal.reveng;

import java.sql.DatabaseMetaData;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.JDBCException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.ProgressListener;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;

public class BasicColumnProcessor {

	private static final Logger log = Logger.getLogger(BasicColumnProcessor.class);

	public static void processBasicColumns(
			MetaDataDialect metaDataDialect, 
			ReverseEngineeringStrategy revengStrategy, 
			String defaultSchema, String defaultCatalog, 
			Table table, 
			ProgressListener progress) {
		
		String qualify = TableNameQualifier.qualify(table.getCatalog(), table.getSchema(), table.getName() );
		Iterator<?> columnIterator = null;
		
		try {
			Map<?, ?> columnRs = null;
			log.debug("Finding columns for " + qualify );
			progress.startSubTask("Finding columns for " + qualify);
			columnIterator = metaDataDialect.getColumns(getCatalogForDBLookup(table.getCatalog(), defaultCatalog), getSchemaForDBLookup(table.getSchema(), defaultSchema), table.getName(), null);
			//dumpHeader(columnRs);
			while (columnIterator.hasNext() ) {
				//dumpRow(columnRs);
				columnRs = (Map<?, ?>) columnIterator.next();
				String tableName = (String) columnRs.get("TABLE_NAME");
				int sqlType = ((Integer)columnRs.get("DATA_TYPE")).intValue();
				//String sqlTypeName = (String) columnRs.get("TYPE_NAME");
				String columnName = (String) columnRs.get("COLUMN_NAME");
				String comment = (String) columnRs.get("REMARKS");
				
				TableIdentifier ti = RevEngUtils.createTableIdentifier(table, defaultCatalog, defaultSchema);
				if(revengStrategy.excludeColumn(ti, columnName)) {
					log.debug("Column " + ti + "." + columnName + " excluded by strategy");
					continue;
				}
				if(!tableName.equals(table.getName())) {
					log.debug("Table name " + tableName + " does not match requested " + table.getName() + ". Ignoring column " + columnName + " since it either is invalid or a duplicate" );
					continue;
				}
				
				//String columnDefaultValue = columnRs.getString("COLUMN_DEF"); TODO: only read if have a way to avoid issues with clobs/lobs and similar
				int dbNullability = ((Integer)columnRs.get("NULLABLE")).intValue();
				boolean isNullable = true;
				switch (dbNullability) {
				case DatabaseMetaData.columnNullable:
				case DatabaseMetaData.columnNullableUnknown:
					isNullable = true;
					break;
				case DatabaseMetaData.columnNoNulls:
					isNullable = false;
					break;
				default:
					isNullable = true;
				}
				
				int size = ((Integer)columnRs.get("COLUMN_SIZE")).intValue();
				int decimalDigits = ((Integer)columnRs.get("DECIMAL_DIGITS")).intValue();
				
				Column column = new Column();
				column.setName(quote(columnName, metaDataDialect));
				Column existing = table.getColumn(column);
				if(existing!=null) {
					// TODO: should we just pick it up and fill it up with whatever we get from the db instead ?
					throw new JdbcBinderException(column + " already exists in " + qualify);
				}
								
				//TODO: column.setSqlType(sqlTypeName); //this does not work 'cos the precision/scale/length are not retured in TYPE_NAME
				//column.setSqlType(sqlTypeName);
				column.setComment(comment);
				column.setSqlTypeCode(Integer.valueOf(sqlType) );
                if(intBounds(size) ) {
                	if(JdbcToHibernateTypeHelper.typeHasLength(sqlType) ) {
                		column.setLength(size);
                	} 
                	if(JdbcToHibernateTypeHelper.typeHasScaleAndPrecision(sqlType) ) {
                		column.setPrecision(size); 
                	}
				} 
                if(intBounds(decimalDigits) ) {
                	if(JdbcToHibernateTypeHelper.typeHasScaleAndPrecision(sqlType) ) {
                		column.setScale(decimalDigits);
                	}
				}
				
				column.setNullable(isNullable);

				// columnDefaultValue is useless for Hibernate
				// isIndexed  (available via Indexes)
				// unique - detected when getting indexes
				// isPk - detected when finding primary keys				
				
				table.addColumn(column);
			}
		}
		finally {
			
			if(columnIterator!=null) {
				try {
					metaDataDialect.close(columnIterator);
				} catch(JDBCException se) {
					log.warn("Exception while closing iterator for column meta data",se);
				}
			}
		}
				
	}

	private static String getCatalogForDBLookup(String catalog, String defaultCatalog) {
		return catalog==null?defaultCatalog:catalog;			
	}

	private static String getSchemaForDBLookup(String schema, String defaultSchema) {
		return schema==null?defaultSchema:schema;
	}

    private static boolean intBounds(int size) {
        return size>=0 && size!=Integer.MAX_VALUE;
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
