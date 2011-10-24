/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.util;
import java.util.Iterator;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

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
