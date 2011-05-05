/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.global;

import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.ObjectName;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.internal.MetadataImpl;

/**
 * Binds table related information. This binder is called after the entities are bound.
 *
 * @author Hardy Ferentschik
 */
public class TableBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, TableBinder.class.getName()
	);

	private TableBinder() {
	}

	/**
	 * Binds {@link org.hibernate.annotations.Tables} and {@link org.hibernate.annotations.Table}
	 * annotations to the specified meta data instance.
	 *
	 * @param meta the global metadata
	 * @param index the annotation index repository
	 */
	public static void bind(MetadataImpl meta, Index index) {
		// check @o.h.a.Tables
		List<AnnotationInstance> tablesAnnotations = index.getAnnotations( HibernateDotNames.TABLES );
		for ( AnnotationInstance tableAnnotation : tablesAnnotations ) {
			AnnotationInstance tables[] = tableAnnotation.value().asNestedArray();
			bindTablesAnnotation( meta, Arrays.asList( tables ) );
		}

		// check @o.h.a.Table
		List<AnnotationInstance> tableAnnotations = index.getAnnotations( HibernateDotNames.TABLE );
		bindTablesAnnotation( meta, tableAnnotations );
	}

	private static void bindTablesAnnotation(MetadataImpl meta, List<AnnotationInstance> tableAnnotations) {
		for ( AnnotationInstance tableAnnotation : tableAnnotations ) {
			String tableName = tableAnnotation.value( "appliesTo" ).asString();
			ObjectName objectName = new ObjectName( tableName );
			Schema schema = meta.getDatabase().getSchema( objectName.getSchema(), objectName.getCatalog() );
			Table table = schema.getTable( objectName.getName() );
			if ( table == null ) {
				continue;
			}
			bindHibernateTableAnnotation( table, tableAnnotation );
		}
	}

	private static void bindHibernateTableAnnotation(Table table, AnnotationInstance tableAnnotation) {
		if ( tableAnnotation.value( "indexes" ) != null ) {
			AnnotationInstance[] indexAnnotations = tableAnnotation.value( "indexes" ).asNestedArray();
			for ( AnnotationInstance indexAnnotation : indexAnnotations ) {
				bindIndexAnnotation( table, indexAnnotation );
			}
		}

		if ( tableAnnotation.value( "comment" ) != null ) {
			table.addComment( tableAnnotation.value( "comment" ).asString().trim() );
		}



	}

	private static void bindIndexAnnotation(Table table, AnnotationInstance indexAnnotation) {
		String indexName = indexAnnotation.value( "name" ).asString();
		if ( indexAnnotation.value( "columnNames" ) == null ) {
			LOG.noColumnsSpecifiedForIndex( indexName, table.toLoggableString() );
			return;
		}

		org.hibernate.metamodel.relational.Index index = table.getOrCreateIndex( indexName );

		String[] columnNames = indexAnnotation.value( "columnNames" ).asStringArray();
		for ( String columnName : columnNames ) {
			Column column = findColumn( table, columnName );
			if ( column == null ) {
				throw new AnnotationException(
						"@Index references a unknown column: " + columnName
				);
			}
			index.addColumn( column );
		}
	}

	private static Column findColumn(Table table, String columnName) {
		Column column = null;
		for ( SimpleValue value : table.values() ) {
			if ( value instanceof Column && ( (Column) value ).getName().equals( columnName ) ) {
				column = (Column) value;
				break;
			}
		}
		return column;
	}
}


