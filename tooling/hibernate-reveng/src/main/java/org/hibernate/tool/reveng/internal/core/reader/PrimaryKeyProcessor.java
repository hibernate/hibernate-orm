/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.reader;

import org.hibernate.JDBCException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.sql.Alias;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.internal.core.RevengMetadataCollector;
import org.hibernate.tool.reveng.internal.core.util.RevengUtils;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PrimaryKeyProcessor {

	private static final Logger log = Logger.getLogger(PrimaryKeyProcessor.class);

	public static void processPrimaryKey(
			RevengDialect metaDataDialect,
			RevengStrategy revengStrategy,
			String defaultSchema,
			String defaultCatalog,
			RevengMetadataCollector revengMetadataCollector,
			Table table) {

		List<Object[]> columns = new ArrayList<>();
		PrimaryKey key = null;
		Iterator<Map<String, Object>> primaryKeyIterator = null;
		try {
			Map<String, Object> primaryKeyRs;
			primaryKeyIterator = metaDataDialect.getPrimaryKeys(getCatalogForDBLookup(table.getCatalog(), defaultCatalog), getSchemaForDBLookup(table.getSchema(), defaultSchema), table.getName() );

			while (primaryKeyIterator.hasNext() ) {
				primaryKeyRs = primaryKeyIterator.next();

				String columnName = (String) primaryKeyRs.get("COLUMN_NAME");
				short seq = (Short) primaryKeyRs.get("KEY_SEQ");
				String name = (String) primaryKeyRs.get("PK_NAME");

				if(key==null) {
					key = new PrimaryKey(table);
					key.setName(name);
					if(table.getPrimaryKey()!=null) {
						throw new RuntimeException(table + " already has a primary key!"); //TODO: ignore ?
					}
					table.setPrimaryKey(key);
				}
				else {
					if( !Objects.equals( name, key.getName() ) && name != null ) {
						throw new RuntimeException("Duplicate names found for primarykey. Existing name: " + key.getName() + " JDBC name: " + name + " on table " + table);
					}
				}

				columns.add(new Object[] {seq, columnName});
			}
		}
		finally {
			if (primaryKeyIterator!=null) {
				try {
					metaDataDialect.close(primaryKeyIterator);
				}
				catch(JDBCException se) {
					log.warn("Exception when closing resultset for reading primary key information",se);
				}
			}
		}

		columns.sort((o1, o2) -> {
			Short left = (Short) o1[0];
			Short right = (Short) o2[0];
			return left.compareTo(right);
		});

		List<String> t = new ArrayList<>( columns.size() );
		for (Object[] element : columns) {
			t.add((String) element[1]);
		}

		if(key==null) {
			log.warn("The JDBC driver didn't report any primary key columns in " + table.getName() + ". Asking rev.eng. strategy" );
			List<String> userPrimaryKey = RevengUtils.getPrimaryKeyInfoInRevengStrategy(revengStrategy, table, defaultCatalog, defaultSchema);	      	if(userPrimaryKey!=null && !userPrimaryKey.isEmpty()) {
				key = new PrimaryKey(table);
				key.setName(new Alias(15, "PK").toAliasString( table.getName()));
				if(table.getPrimaryKey()!=null) {
					throw new RuntimeException(table + " already has a primary key!"); //TODO: ignore ?
				}
				table.setPrimaryKey(key);
				t = new ArrayList<>( userPrimaryKey );
			}
			else {
				log.warn("Rev.eng. strategy did not report any primary key columns for " + table.getName());
			}
		}

		Iterator<Map<String, Object>> suggestedPrimaryKeyStrategyName = metaDataDialect.getSuggestedPrimaryKeyStrategyName( getCatalogForDBLookup(table.getCatalog(), defaultCatalog), getSchemaForDBLookup(table.getSchema(), defaultSchema), table.getName() );
		try {
			if(suggestedPrimaryKeyStrategyName.hasNext()) {
				Map<String, Object> m = suggestedPrimaryKeyStrategyName.next();
				String suggestion = (String) m.get( "HIBERNATE_STRATEGY" );
				if(suggestion!=null) {
					revengMetadataCollector.addSuggestedIdentifierStrategy(
							transformForModelLookup(table.getCatalog(), defaultCatalog),
							transformForModelLookup(table.getSchema(), defaultSchema),
							table.getName(),
							suggestion );
				}
			}
		}
		finally {
			if(suggestedPrimaryKeyStrategyName!=null) {
				try {
					metaDataDialect.close(suggestedPrimaryKeyStrategyName);
				}
				catch(JDBCException se) {
					log.warn("Exception while closing iterator for suggested primary key strategy name",se);
				}
			}
		}

		if(key!=null) {
			for (String name : t) {
				// should get column from table if it already exists!
				Column col = getColumn(metaDataDialect, table, name);
				key.addColumn(col);
			}
			log.debug("primary key for " + table + " -> "  + key);
		}

	}

	private static String getCatalogForDBLookup(String catalog, String defaultCatalog) {
		return catalog==null?defaultCatalog:catalog;
	}

	private static String transformForModelLookup(String id, String defaultId) {
		return id == null || id.equals(defaultId) ? null : id;
	}

	private static String getSchemaForDBLookup(String schema, String defaultSchema) {
		return schema==null?defaultSchema:schema;
	}

	private static Column getColumn(RevengDialect metaDataDialect, Table table, String columnName) {
		Column column = new Column();
		column.setName(quote(metaDataDialect, columnName));
		Column existing = table.getColumn(column);
		if(existing!=null) {
			column = existing;
		}
		return column;
	}

	private static String quote(RevengDialect metaDataDialect, String columnName) {
		if(columnName==null) return null;
		if(metaDataDialect.needQuote(columnName)) {
			if(columnName.length()>1 && columnName.charAt(0)=='`' && columnName.charAt(columnName.length()-1)=='`') {
				return columnName; // avoid double quoting
			}
			return "`" + columnName + "`";
		}
		else {
			return columnName;
		}
	}

}
