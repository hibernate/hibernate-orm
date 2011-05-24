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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.hibernate.AnnotationException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.ObjectName;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;

/**
 * Binds table related information. This binder is called after the entities are bound.
 *
 * @author Hardy Ferentschik
 */
public class TableBinder {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TableBinder.class.getName());

    /**
     * Binds {@link org.hibernate.annotations.Tables} and {@link org.hibernate.annotations.Table} annotations to the supplied
     * metadata.
     *
     * @param metadata the global metadata
     * @param jandex the annotation index repository
     */
    public static void bind( MetadataImpl metadata,
                             Index jandex ) {
        for (AnnotationInstance tableAnnotation : jandex.getAnnotations(HibernateDotNames.TABLE)) {
            bind(metadata, tableAnnotation);
        }
        for (AnnotationInstance tables : jandex.getAnnotations(HibernateDotNames.TABLES)) {
            for (AnnotationInstance table : JandexHelper.getValueAsArray(tables, "value")) {
                bind(metadata, table);
            }
        }
    }

    private static void bind( MetadataImpl metadata,
                              AnnotationInstance tableAnnotation ) {
        String tableName = JandexHelper.getValueAsString(tableAnnotation, "appliesTo");
        ObjectName objectName = new ObjectName(tableName);
        Schema schema = metadata.getDatabase().getSchema(objectName.getSchema(), objectName.getCatalog());
        Table table = schema.getTable(objectName.getName());
        if (table != null) bindHibernateTableAnnotation(table, tableAnnotation);
    }

    private static void bindHibernateTableAnnotation( Table table,
                                                      AnnotationInstance tableAnnotation ) {
        for (AnnotationInstance indexAnnotation : JandexHelper.getValueAsArray(tableAnnotation, "indexes")) {
            bindIndexAnnotation(table, indexAnnotation);
        }
        String comment = JandexHelper.getValueAsString(tableAnnotation, "comment");
        if (StringHelper.isNotEmpty(comment)) table.addComment(comment.trim());
    }

    private static void bindIndexAnnotation( Table table,
                                             AnnotationInstance indexAnnotation ) {
        String indexName = JandexHelper.getValueAsString(indexAnnotation, "appliesTo");
        String[] columnNames = (String[])JandexHelper.getValue(indexAnnotation, "columnNames");
        if (columnNames == null) {
            LOG.noColumnsSpecifiedForIndex(indexName, table.toLoggableString());
            return;
        }
        org.hibernate.metamodel.relational.Index index = table.getOrCreateIndex(indexName);
        for (String columnName : columnNames) {
            Column column = findColumn(table, columnName);
            if (column == null) throw new AnnotationException("@Index references a unknown column: " + columnName);
            index.addColumn(column);
        }
    }

    private static Column findColumn( Table table,
                                      String columnName ) {
        Column column = null;
        for (SimpleValue value : table.values()) {
            if (value instanceof Column && ((Column)value).getName().equals(columnName)) {
                column = (Column)value;
                break;
            }
        }
        return column;
    }

    private TableBinder() {
    }
}
