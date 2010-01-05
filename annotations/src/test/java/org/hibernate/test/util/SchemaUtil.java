package org.hibernate.test.util;

import java.util.Iterator;

import org.hibernate.mapping.Table;
import org.hibernate.mapping.Column;
import org.hibernate.cfg.Configuration;

/**
 * Check that the Hibernate metamodel contains some database objects
 *
 * @author Emmanuel Bernard
 */
public abstract class SchemaUtil {
	public static boolean isColumnPresent(String tableName, String columnName, Configuration cfg) {
		final Iterator<Table> tables = ( Iterator<Table> ) cfg.getTableMappings();
		while (tables.hasNext()) {
			Table table = tables.next();
			if (tableName.equals( table.getName() ) ) {
				Iterator<Column> columns = (Iterator<Column>) table.getColumnIterator();
				while ( columns.hasNext() ) {
					Column column = columns.next();
					if ( columnName.equals( column.getName() ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isTablePresent(String tableName, Configuration cfg) {
		final Iterator<Table> tables = ( Iterator<Table> ) cfg.getTableMappings();
		while (tables.hasNext()) {
			Table table = tables.next();
			if (tableName.equals( table.getName() ) ) {
				return true;
			}
		}
		return false;
	}
}
