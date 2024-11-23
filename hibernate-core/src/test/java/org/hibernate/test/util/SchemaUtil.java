/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.EntityManagerFactory;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.AbstractEntityPersister;

/**
 * Check that the Hibernate metamodel contains some database objects
 *
 * @author Emmanuel Bernard
 */
public abstract class SchemaUtil {
	public static Set<String> getColumnNames(String tableName, Metadata metadata) {
		Set<String> result = new HashSet<>();
		for ( Table table : metadata.collectTableMappings() ) {
			if (tableName.equals( table.getName() ) ) {
				Iterator<Column> columns = table.getColumnIterator();
				while ( columns.hasNext() ) {
					Column column = columns.next();
					result.add( column.getName() );
				}
			}
		}
		return result;
	}

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

	public static Set<String> getColumnNames(EntityManagerFactory entityManagerFactory, Class<?> entityType) {
		Set<String> result = new HashSet<>();
		AbstractEntityPersister persister = (AbstractEntityPersister) entityManagerFactory
				.unwrap( SessionFactoryImplementor.class )
				.getMetamodel().entityPersister( entityType );
		if ( persister == null ) {
			return result;
		}
		for ( String propertyName : persister.getPropertyNames() ) {
			Collections.addAll( result, persister.getPropertyColumnNames( propertyName ) );
		}
		return result;
	}
}
