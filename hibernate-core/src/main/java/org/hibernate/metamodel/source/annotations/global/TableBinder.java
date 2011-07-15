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

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.ObjectName;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Binds table related information. This binder is called after the entities are bound.
 *
 * @author Hardy Ferentschik
 */
public class TableBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TableBinder.class.getName()
	);

	private TableBinder() {
	}

	/**
	 * Binds {@link org.hibernate.annotations.Tables} and {@link org.hibernate.annotations.Table} annotations to the supplied
	 * metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static void bind(AnnotationBindingContext bindingContext) {
		List<AnnotationInstance> annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.TABLE );
		for ( AnnotationInstance tableAnnotation : annotations ) {
			bind( bindingContext.getMetadataImplementor(), tableAnnotation );
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.TABLES );
		for ( AnnotationInstance tables : annotations ) {
			for ( AnnotationInstance table : JandexHelper.getValue( tables, "value", AnnotationInstance[].class ) ) {
				bind( bindingContext.getMetadataImplementor(), table );
			}
		}
	}

	private static void bind(MetadataImplementor metadata, AnnotationInstance tableAnnotation) {
		String tableName = JandexHelper.getValue( tableAnnotation, "appliesTo", String.class );
		ObjectName objectName = new ObjectName( tableName );
		Schema schema = metadata.getDatabase().getSchema( objectName.getSchema(), objectName.getCatalog() );
		Table table = schema.locateTable( objectName.getName() );
		if ( table != null ) {
			bindHibernateTableAnnotation( table, tableAnnotation );
		}
	}

	private static void bindHibernateTableAnnotation(Table table, AnnotationInstance tableAnnotation) {
		for ( AnnotationInstance indexAnnotation : JandexHelper.getValue(
				tableAnnotation,
				"indexes",
				AnnotationInstance[].class
		) ) {
			bindIndexAnnotation( table, indexAnnotation );
		}
		String comment = JandexHelper.getValue( tableAnnotation, "comment", String.class );
		if ( StringHelper.isNotEmpty( comment ) ) {
			table.addComment( comment.trim() );
		}
	}

	private static void bindIndexAnnotation(Table table, AnnotationInstance indexAnnotation) {
		String indexName = JandexHelper.getValue( indexAnnotation, "appliesTo", String.class );
		String[] columnNames = JandexHelper.getValue( indexAnnotation, "columnNames", String[].class );
		if ( columnNames == null ) {
			LOG.noColumnsSpecifiedForIndex( indexName, table.toLoggableString() );
			return;
		}
		org.hibernate.metamodel.relational.Index index = table.getOrCreateIndex( indexName );
		for ( String columnName : columnNames ) {
			Column column = findColumn( table, columnName );
			if ( column == null ) {
				throw new AnnotationException( "@Index references a unknown column: " + columnName );
			}
			index.addColumn( column );
		}
	}

	private static Column findColumn(Table table, String columnName) {
		Column column = null;
		for ( SimpleValue value : table.values() ) {
			if ( value instanceof Column && ( (Column) value ).getColumnName().getName().equals( columnName ) ) {
				column = (Column) value;
				break;
			}
		}
		return column;
	}
}
