/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2019-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevengMetadataCollector;
import org.hibernate.tool.internal.reveng.binder.ForeignKeyUtils.ForeignKeyForColumns;
import org.hibernate.tool.internal.reveng.util.EnhancedBasicValue;
import org.hibernate.tool.internal.reveng.util.EnhancedComponent;
import org.hibernate.tool.internal.reveng.util.EnhancedValue;
import org.hibernate.tool.internal.reveng.util.RevengUtils;

class PrimaryKeyBinder extends AbstractBinder {

    private static final Logger LOGGER = Logger.getLogger(PrimaryKeyBinder.class.getName());

    static PrimaryKeyBinder create(BinderContext binderContext) {
        return new PrimaryKeyBinder(binderContext);
    }

    private final BasicPropertyBinder basicPropertyBinder;
    private final BasicValueBinder simpleValueBinder;
    private final ManyToOneBinder manyToOneBinder;
    private final PropertyBinder propertyBinder;


    private PrimaryKeyBinder(BinderContext binderContext) {
        super(binderContext);
        this.basicPropertyBinder = BasicPropertyBinder.create(binderContext);
        this.simpleValueBinder = BasicValueBinder.create(binderContext);
        this.manyToOneBinder = ManyToOneBinder.create(binderContext);
        this.propertyBinder = PropertyBinder.create(binderContext);
    }

    PrimaryKeyInfo bind(
            Table table,
            RootClass rc,
            Set<Column> processed,
            RevengMetadataCollector revengMetadataCollector) {
        List<Column> keyColumns = getKeyColumns(table);
        final TableIdentifier tableIdentifier = TableIdentifier.create(table);
        PrimaryKeyInfo pki = createPrimaryKeyInfo(tableIdentifier, keyColumns);
        EnhancedValue id = createKeyValue(rc, keyColumns, pki.suggestedStrategy, table, revengMetadataCollector, processed);
        id.setIdentifierGeneratorProperties(pki.suggestedProperties);
        Property property = propertyBinder.bind(
                table,
                BinderUtils.makeUnique(rc,getIdPropertyName(tableIdentifier, keyColumns)),
                id,
                RevengUtils.createAssociationInfo(null, null, true, true));
        rc.setIdentifierProperty(property);
        rc.setDeclaredIdentifierProperty(property);
        rc.setIdentifier(id);
        return pki;
    }

    void updatePrimaryKey(RootClass rc, PrimaryKeyInfo pki) {
        EnhancedValue idValue = (EnhancedValue) rc.getIdentifierProperty().getValue();
        Properties defaultStrategyProperties = new Properties();
        Property constrainedOneToOne = getConstrainedOneToOne(rc);
        if(constrainedOneToOne!=null) {
            if(pki.suggestedStrategy==null) {
                idValue.setIdentifierGeneratorStrategy("foreign");
            }
            if(pki.suggestedProperties==null) {
                defaultStrategyProperties.setProperty("property", constrainedOneToOne.getName());
                idValue.setIdentifierGeneratorProperties(defaultStrategyProperties);
            }
        }
    }

    private EnhancedValue createKeyValue(
            PersistentClass rc,
            List<Column> keyColumns,
            String suggestedStrategyName,
            Table table,
            RevengMetadataCollector revengMetadataCollector,
            Set<Column> processed) {
        if (keyColumns.size()>1) {
            LOGGER.log(Level.INFO, "id strategy for " + rc.getEntityName() + " since it has a multiple column primary key");
            return handleCompositeKey(rc, processed, keyColumns);
        }
        else {
            String tableIdentifierStrategyName =
                    getTableIdentifierStrategyName(suggestedStrategyName, revengMetadataCollector, table);
            return handleColumnKey(table, tableIdentifierStrategyName, processed, keyColumns);
        }
    }

    private PrimaryKeyInfo createPrimaryKeyInfo(
            TableIdentifier tableIdentifier,
            List<Column> keyColumns) {
        PrimaryKeyInfo result = new PrimaryKeyInfo();
        result.suggestedProperties = getRevengStrategy().getTableIdentifierProperties(tableIdentifier);
        if (keyColumns.size() == 1) {
            result.suggestedStrategy = RevengUtils.getTableIdentifierStrategyNameInRevengStrategy(
                    getRevengStrategy(),
                    tableIdentifier,
                    getDefaultCatalog(),
                    getDefaultSchema());
        }
        return result;
    }

    private String getTableIdentifierStrategyName(
            String suggestedStrategy,
            RevengMetadataCollector revengMetadataCollector,
            Table table) {
        if(suggestedStrategy==null) {
            suggestedStrategy = revengMetadataCollector.getSuggestedIdentifierStrategy(
                    table.getCatalog(),
                    table.getSchema(),
                    table.getName() );
            return suggestedStrategy == null ? "assigned" : suggestedStrategy;
        }
        else {
            return suggestedStrategy;
        }
    }

    private String getIdPropertyName(
            TableIdentifier tableIdentifier,

            List<Column> keyColumns) {
        String result = getRevengStrategy().tableToIdentifierPropertyName(tableIdentifier);
        if (result ==  null) {
            if (keyColumns.size() > 1) {
                result = "id";
            }
            else {
                result = getRevengStrategy().columnToPropertyName(
                        tableIdentifier,
                        keyColumns.get(0).getName());
            }
        }
        return result;
    }

    private boolean isGeneratedId(
            List<Column> keyColumns,
            String tableIdentifierStrategyName) {
        boolean result = false;
        if (keyColumns.size() == 1) {
            result = !"assigned".equals(tableIdentifierStrategyName);
        }
        return result;
    }

    private List<Column> getKeyColumns(Table table) {
        List<Column> result;
        if (table.getPrimaryKey()!=null) {
            result = table.getPrimaryKey().getColumns();
        }
        else {
            LOGGER.log(Level.INFO, "No primary key found for " + table + ", using all properties as the identifier.");
            result = new ArrayList<>( table.getColumns() );
        }
        return result;
    }

    private EnhancedBasicValue handleColumnKey(
            Table table,
            String tableIdentifierStrategyName,
            Set<Column> processed,
            List<Column> keyColumns) {
        Column pkc = keyColumns.get(0);
        BinderUtils.checkColumnForMultipleBinding(pkc);
        processed.add(pkc);
        EnhancedBasicValue result = simpleValueBinder.bind(
                table,
                pkc,
                isGeneratedId(keyColumns, tableIdentifierStrategyName));
        result.setIdentifierGeneratorStrategy(tableIdentifierStrategyName);
        return result;
    }

    private EnhancedComponent handleCompositeKey(
            PersistentClass rc,
            Set<Column> processedColumns,
            List<Column> keyColumns) {
        EnhancedComponent result = new EnhancedComponent(getMetadataBuildingContext(), rc);
        result.setMetaAttributes(Collections.emptyMap());
        result.setEmbedded(false);
        result.setComponentClassName(getCompositeIdName(rc));
        addKeyColumns(result, rc.getTable(), getKeyColumns(rc.getTable(), keyColumns), processedColumns);
        result.setNullValue("undefined");
        result.setIdentifierGeneratorStrategy("assigned");
        return result;
    }

    private static void markAsUseInEquals(Property property) {
        Map<String, MetaAttribute> m = new HashMap<>();
        MetaAttribute ma = new MetaAttribute("use-in-equals");
        ma.addValue("true");
        m.put(ma.getName(),ma);
        property.setMetaAttributes(m);
    }

    private Property getConstrainedOneToOne(RootClass rc) {
        for (Property property : rc.getProperties()) {
            if( property.getValue() instanceof OneToOne oto ) {
                if(oto.isConstrained()) {
                    return property;
                }
            }
        }
        return null;
    }

    private Property bindManyToOneProperty(
            Component pkc,
            ForeignKeyForColumns fkfc,
            Table table, Set<Column>
                    processedColumns) {
        ForeignKey foreignKey = fkfc.key;
        Property property = manyToOneBinder.bind(
                BinderUtils.makeUnique(pkc, getForeignKeyToEntityName(foreignKey)),
                true,
                table,
                foreignKey,
                processedColumns);
        processedColumns.addAll(fkfc.columns);
        return property;
    }

    private Property bindBasicProperty(
            Component pkc,
            Column column,
            Table table,
            Set<Column> processedColumns) {
        Property result;
        if ( processedColumns.contains(column) ) {
            throw new RuntimeException("Binding column twice for primary key should not happen: " + column);
        }
        else {
            BinderUtils.checkColumnForMultipleBinding(column);
            result = basicPropertyBinder.bind(
                    BinderUtils.makeUnique(pkc, getColumnToPropertyName(table, column)),
                    table,
                    column);
            processedColumns.add(column);
        }
        return result;
    }

    private String getForeignKeyToEntityName(ForeignKey foreignKey) {
        return getRevengStrategy().foreignKeyToEntityName(
                foreignKey.getName(),
                TableIdentifier.create(foreignKey.getTable()),
                foreignKey.getColumns(),
                TableIdentifier.create(foreignKey.getReferencedTable()),
                foreignKey.getReferencedColumns(),
                true
        );
    }

    private String getColumnToPropertyName(Table table, Column column) {
        return getRevengStrategy().columnToPropertyName(
                TableIdentifier.create(table),
                column.getName());
    }

    private void addKeyColumns(
            Component pkc,
            Table table,
            List<?> list,
            Set<Column> processedColumns) {
        for ( Object element : list ) {
            Property property;
            if ( element instanceof Column ) {
                property = bindBasicProperty( pkc, (Column) element, table, processedColumns );
            }
            else if ( element instanceof ForeignKeyForColumns ) {
                property = bindManyToOneProperty( pkc, (ForeignKeyForColumns) element, table, processedColumns );
            }
            else {
                throw new RuntimeException( "unknown thing" );
            }
            markAsUseInEquals( property );
            pkc.addProperty( property );
        }
    }

    private List<?> getKeyColumns(Table table, List<Column> keyColumns) {
        if (preferBasicCompositeIds() ) {
            return new ArrayList<Object>(keyColumns);
        }
        else {
            return ForeignKeyUtils.findForeignKeys(table.getForeignKeyCollection(), keyColumns);
        }
    }

    private String getCompositeIdName(PersistentClass pc) {
        String compositeIdName = getRevengStrategy().tableToCompositeIdName(
                TableIdentifier.create(pc.getTable()));
        if(compositeIdName==null) {
            compositeIdName = getRevengStrategy().classNameToCompositeIdName(pc.getClassName());
        }
        return compositeIdName;
    }

}
