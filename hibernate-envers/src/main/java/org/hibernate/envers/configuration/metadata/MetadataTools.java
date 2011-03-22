/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.configuration.metadata;
import java.util.*;
import javax.persistence.JoinColumn;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
public class MetadataTools {
    public static Element addNativelyGeneratedId(Element parent, String name, String type) {
        Element id_mapping = parent.addElement("id");
        id_mapping.addAttribute("name", name).addAttribute("type", type);

        Element generator_mapping = id_mapping.addElement("generator");
        generator_mapping.addAttribute("class", "native");
        /*generator_mapping.addAttribute("class", "sequence");
        generator_mapping.addElement("param").addAttribute("name", "sequence").setText("custom");*/

        return id_mapping;
    }

    public static Element addProperty(Element parent, String name, String type, boolean insertable, boolean updateable, boolean key) {
        Element prop_mapping;
        if (key) {
            prop_mapping = parent.addElement("key-property");
        } else {
            prop_mapping = parent.addElement("property");
        }

        prop_mapping.addAttribute("name", name);
        prop_mapping.addAttribute("insert", Boolean.toString(insertable));
        prop_mapping.addAttribute("update", Boolean.toString(updateable));

        if (type != null) {
            prop_mapping.addAttribute("type", type);
        }

        return prop_mapping;
    }

    public static Element addProperty(Element parent, String name, String type, boolean insertable, boolean key) {
        return addProperty(parent, name, type, insertable, false, key);
    }

    private static void addOrModifyAttribute(Element parent, String name, String value) {
        Attribute attribute = parent.attribute(name);
        if (attribute == null) {
            parent.addAttribute(name, value);
        } else {
            attribute.setValue(value);
        }
    }

    public static Element addOrModifyColumn(Element parent, String name) {
        Element column_mapping = parent.element("column");

        if (column_mapping == null) {
            return addColumn(parent, name, null, 0, 0, null, null, null);
        }

        if (!StringTools.isEmpty(name)) {
            addOrModifyAttribute(column_mapping, "name", name);
        }

        return column_mapping;
    }

    public static Element addColumn(Element parent, String name, Integer length, Integer scale, Integer precision,
									String sqlType, String customRead, String customWrite) {
        Element column_mapping = parent.addElement("column");

        column_mapping.addAttribute("name", name);
        if (length != null) {
            column_mapping.addAttribute("length", length.toString());
        }
		if (scale != 0) {
			column_mapping.addAttribute("scale", Integer.toString(scale));
		}
		if (precision != 0) {
			column_mapping.addAttribute("precision", Integer.toString(precision));
		}
		if (!StringTools.isEmpty(sqlType)) {
            column_mapping.addAttribute("sql-type", sqlType);
        }

        if (!StringTools.isEmpty(customRead)) {
            column_mapping.addAttribute("read", customRead);
        }
        if (!StringTools.isEmpty(customWrite)) {
            column_mapping.addAttribute("write", customWrite);
        }

        return column_mapping;
    }

    private static Element createEntityCommon(Document document, String type, AuditTableData auditTableData,
                                              String discriminatorValue) {
        Element hibernate_mapping = document.addElement("hibernate-mapping");
        hibernate_mapping.addAttribute("auto-import", "false");

        Element class_mapping = hibernate_mapping.addElement(type);

        if (auditTableData.getAuditEntityName() != null) {
            class_mapping.addAttribute("entity-name", auditTableData.getAuditEntityName());
        }

        if (discriminatorValue != null) {
            class_mapping.addAttribute("discriminator-value", discriminatorValue);
        }

        if (!StringTools.isEmpty(auditTableData.getAuditTableName())) {
            class_mapping.addAttribute("table", auditTableData.getAuditTableName());
        }

        if (!StringTools.isEmpty(auditTableData.getSchema())) {
            class_mapping.addAttribute("schema", auditTableData.getSchema());
        }

        if (!StringTools.isEmpty(auditTableData.getCatalog())) {
            class_mapping.addAttribute("catalog", auditTableData.getCatalog());
        }

        return class_mapping;
    }

    public static Element createEntity(Document document, AuditTableData auditTableData, String discriminatorValue) {
        return createEntityCommon(document, "class", auditTableData, discriminatorValue);
    }

    public static Element createSubclassEntity(Document document, String subclassType, AuditTableData auditTableData,
                                               String extendsEntityName, String discriminatorValue) {
        Element class_mapping = createEntityCommon(document, subclassType, auditTableData, discriminatorValue);

        class_mapping.addAttribute("extends", extendsEntityName);

        return class_mapping;
    }

    public static Element createJoin(Element parent, String tableName,
                                     String schema, String catalog) {
        Element join_mapping = parent.addElement("join");

        join_mapping.addAttribute("table", tableName);

        if (!StringTools.isEmpty(schema)) {
            join_mapping.addAttribute("schema", schema);
        }

        if (!StringTools.isEmpty(catalog)) {
            join_mapping.addAttribute("catalog", catalog);
        }

        return join_mapping;
    }

    public static void addColumns(Element any_mapping, Iterator<Column> columns) {
        while (columns.hasNext()) {
            Column column = columns.next();
            addColumn(any_mapping, column.getName(), column.getLength(), column.getScale(), column.getPrecision(),
					column.getSqlType(), column.getCustomRead(), column.getCustomWrite());
        }
    }

    @SuppressWarnings({"unchecked"})
    private static void changeNamesInColumnElement(Element element, ColumnNameIterator columnNameIterator) {
        Iterator<Element> properties = element.elementIterator();
        while (properties.hasNext()) {
            Element property = properties.next();

            if ("column".equals(property.getName())) {
                Attribute nameAttr = property.attribute("name");
                if (nameAttr != null) {
                    nameAttr.setText(columnNameIterator.next());
                }
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public static void prefixNamesInPropertyElement(Element element, String prefix, ColumnNameIterator columnNameIterator,
                                                    boolean changeToKey, boolean insertable) {
        Iterator<Element> properties = element.elementIterator();
        while (properties.hasNext()) {
            Element property = properties.next();

            if ("property".equals(property.getName())) {
                Attribute nameAttr = property.attribute("name");
                if (nameAttr != null) {
                    nameAttr.setText(prefix + nameAttr.getText());
                }

                changeNamesInColumnElement(property, columnNameIterator);

                if (changeToKey) {
                    property.setName("key-property");
                }

				Attribute insert = property.attribute("insert");
				insert.setText(Boolean.toString(insertable));
            }
        }
    }

    /**
     * Returns column name of the given element or <code>null</code>.
     * @param element Element descriptor.
     * @return Column name if the corresponding element is a <code>property</code> or <code>column</code> tag.
     */
    public static String getElementColumnName(Element element) {
        String columnName = null;
        if ("property".equals(element.getName())) {
            // Column name equals property name if 'column' attribute is not specified.
            columnName = element.attribute("column") != null ? element.attribute("column").getValue()
                                                             : element.attribute("name").getValue();
        } else if ("column".equals(element.getName())) {
            columnName = element.attribute("name").getValue();
        }
        return columnName;
    }

    /**
     * Adds database index information to <code>column</code> or <code>property</code> elements.
     * If <code>element</code> (second argument) tag has a different name or corresponding index is not defined,
     * no action is taken.
     * @param pc Persistent class mapping.
     * @param element Element descriptor to which index attribute shall be added.
     * @param indexNamePrefix Index name prefix.
     * @param indexNameSuffix Index name suffix.
     */
    public static void addIndexToColumn(PersistentClass pc, Element element, String indexNamePrefix,
                                        String indexNameSuffix) {
        String columnName = getElementColumnName(element);
        if (columnName != null) {
            Set<String> indexNames = getIndexNameForColumn(pc, columnName);
            if (indexNames != null) {
                element.addAttribute("index", collectionToString(indexNames, ", ", indexNamePrefix, indexNameSuffix));
            }
        }
    }

    /**
     * Generates a string representation of collections' items.
     * @param collection Collection of items.
     * @param separator Desired separator.
     * @param itemPrefix String that shall be concatenated before each item.
     * @param itemSuffix String that shall be concatenated after each item.
     * @return String representation of collections' items.
     */
    private static String collectionToString(Collection collection, String separator, String itemPrefix,
                                             String itemSuffix) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        StringBuilder ret = new StringBuilder();
        for (Object item : collection) {
            ret.append(itemPrefix).append(item).append(itemSuffix).append(separator);
        }
        return ret.substring(0, ret.length() - separator.length());
    }

    /**
     * Returns set of database index names associated with a given column name.
     * @param pc Persistent class mapping.
     * @param columnName Name of the column.
     *        It should be part of the table mapped by persistent class passed as first argument.
     * @return Database index names or <code>null</code>.
     */
    @SuppressWarnings({"unchecked"})
    public static Set<String> getIndexNameForColumn(PersistentClass pc, String columnName) {
        Iterator<Column> columnIterator = pc.getTable().getColumnIterator();
        while (columnIterator.hasNext()) {
            Column column = columnIterator.next();
            if (columnName.equals(column.getName())) {
                return getIndexNameForColumn(pc, column);
            }
        }
        return null;
    }

    /**
     * @see #getIndexNameForColumn(PersistentClass, String)
     */
    @SuppressWarnings({"unchecked"})
    public static Set<String> getIndexNameForColumn(PersistentClass pc, Column column) {
        Set<String> ret = new LinkedHashSet<String>();
        Iterator<Index> indexIterator = pc.getTable().getIndexIterator();
        while (indexIterator.hasNext()) {
            Index index = indexIterator.next();
            if (index.containsColumn(column)) {
                ret.add(index.getName());
            }
        }
        return ret.isEmpty() ? null : ret;
    }

    /**
     * An iterator over column names.
     */
    public static abstract class ColumnNameIterator implements Iterator<String> { }

    public static ColumnNameIterator getColumnNameIterator(final Iterator<Column> columnIterator) {
        return new ColumnNameIterator() {
            public boolean hasNext() { return columnIterator.hasNext(); }
            public String next() { return columnIterator.next().getName(); }
            public void remove() { columnIterator.remove(); }
        };
    }

    public static ColumnNameIterator getColumnNameIterator(final JoinColumn[] joinColumns) {
        return new ColumnNameIterator() {
            int counter = 0;
            public boolean hasNext() { return counter < joinColumns.length; }
            public String next() { return joinColumns[counter++].name(); }
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }
}
