/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util;

import java.util.Iterator;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * Check that the Hibernate metamodel contains some database objects
 *
 * @author Emmanuel Bernard
 */
public abstract class SchemaUtil {
	@SuppressWarnings("unchecked")
	public static boolean isColumnPresent(String tableName, String columnName, Metadata metadata) {
		for ( Table table : metadata.collectTableMappings() ) {
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

	public static boolean isTablePresent(String tableName, Metadata metadata) {
		for ( Table table : metadata.collectTableMappings() ) {
			if ( tableName.equals( table.getName() ) ) {
				return true;
			}
		}

		return false;
	}
}
