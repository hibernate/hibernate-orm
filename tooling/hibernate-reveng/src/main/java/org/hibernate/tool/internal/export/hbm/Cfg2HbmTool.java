/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.export.hbm;

import java.util.*;
import java.util.function.Consumer;

import org.hibernate.FetchMode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.internal.export.common.EntityNameFromValueVisitor;
import org.hibernate.tool.internal.reveng.util.EnhancedValue;
import org.hibernate.tool.internal.util.SkipBackRefPropertyIterator;
import org.hibernate.tool.internal.util.ValueUtil;

/**
 * @author David Channon and Max
 */
public class Cfg2HbmTool {

    private static final class HasEntityPersisterVisitor implements PersistentClassVisitor {
        private final String name;

        private HasEntityPersisterVisitor(String name) {
            this.name = name;
        }

        public Object accept(Subclass subclass) {
            return bool(!SingleTableEntityPersister.class.getName().equals(name));
        }

        private Object bool(boolean b) {
            return b;
        }

        public Object accept(JoinedSubclass subclass) {
            return bool(!JoinedSubclassEntityPersister.class.getName().equals(name));
        }

        public Object accept(SingleTableSubclass subclass) {
            return bool(!SingleTableEntityPersister.class.getName().equals(name));
        }

        public Object accept(UnionSubclass subclass) {
            return bool(!UnionSubclassEntityPersister.class.getName().equals(name));
        }

        public Object accept(RootClass class1) {
            return bool(!SingleTableEntityPersister.class.getName().equals(name));
        }
    }

    /**
     * Remove any internal keys from the set, eg, any Keys that are prefixed by
     * 'target_' and return the filtered collection.
     */
    public static Properties getFilteredIdentifierGeneratorProperties(Properties properties, Properties environmentProperties) {
        if (properties != null){
            Properties fProp = new Properties();
            for (Object o : properties.keySet()) {
                String key = (String) o;
                if ("schema".equals(key)) {
                    String schema = properties.getProperty(key);
                    if (!isDefaultSchema(schema, environmentProperties)) {
                        fProp.put(key, schema);
                    }
                }
                else if ("catalog".equals(key)) {
                    String catalog = properties.getProperty(key);
                    if (!isDefaultCatalog(catalog, environmentProperties)) {
                        fProp.put(key, catalog);
                    }
                }
                else if (!key.startsWith("target_")) {
                    fProp.put(key, properties.get(key));
                }
            }
            return fProp;
        }
        return null;
    }

    static private boolean isDefaultSchema(String schema, Properties properties) {
        String defaultSchema = properties.getProperty(Environment.DEFAULT_SCHEMA);
        return Objects.equals( defaultSchema, schema );
    }

    static private boolean isDefaultCatalog(String catalog, Properties properties) {
        String defaultCatalog = properties.getProperty(Environment.DEFAULT_CATALOG);
        return Objects.equals( defaultCatalog, catalog );
    }

    public String getTag(PersistentClass pc) {
        return (String) pc.accept(HBMTagForPersistentClassVisitor.INSTANCE);
    }

    public String getTag(Property property) {
        PersistentClass persistentClass = property.getPersistentClass();
        if(persistentClass!=null) {
            if(persistentClass.getVersion()==property) {
                String typeName = ((SimpleValue)property.getValue()).getTypeName();
                if("timestamp".equals(typeName) || "dbtimestamp".equals(typeName)) {
                    return "timestamp";
                }
                else {
                    return "version";
                }
            }
        }
        String toolTag = (String) property.getValue().accept(HBMTagForValueVisitor.INSTANCE);
        if ("component".equals(toolTag) && "embedded".equals(property.getPropertyAccessorName())){
            toolTag = "properties";
        }
        return toolTag;
    }

    public String getCollectionElementTag(Property property){
        Value value = property.getValue();
        if (isOneToMany(value)) return "one-to-many";
        if (isManyToMany(value)) return "many-to-many";
        if (isManyToAny(value)) return "many-to-any";
        if (((Collection)value).getElement() instanceof Component){
            return "composite";
        }
        return "element";
    }


    public boolean isUnsavedValue(Property property) {
        SimpleValue sv = (SimpleValue) property.getValue();
        return (sv.getNullValue() != null) && !"undefined".equals(sv.getNullValue());
    }

    public String getUnsavedValue(Property property) {
        return ( (SimpleValue) property.getValue() ).getNullValue();
    }

    public boolean isIdentifierGeneratorProperties(Property property) {
        Properties val = this.getIdentifierGeneratorProperties(property);
        return val != null;
    }

    public Properties getIdentifierGeneratorProperties(Property property) {
        Properties result = null;
        SimpleValue simpleValue = (SimpleValue)property.getValue();
        if (simpleValue instanceof EnhancedValue) {
            Properties idGenParams = ((EnhancedValue)simpleValue).getIdentifierGeneratorProperties();
            if (idGenParams != null) {
                result = new Properties();
                result.putAll(idGenParams);
            }
        }
        else {
            Map<String, Object> properties = new ValueUtil(simpleValue).getIdentifierGeneratorParameters();
            if (properties != null) {
                result = new Properties();
                result.putAll(properties);
            }
        }
        return result;
    }

    public Set<?> getFilteredIdentifierGeneratorKeySet(Property property, Properties props) {
        return getFilteredIdentifierGeneratorProperties(this.getIdentifierGeneratorProperties(property), props).keySet();
    }

    public boolean isOneToMany(Property property) {
        return isOneToMany(property.getValue());
    }

    public boolean isOneToMany(Value value) {
        if(value instanceof Collection) {
            return ( (Collection)value ).isOneToMany();
        }
        else
            return value instanceof OneToMany;
    }

    public boolean isManyToMany(Property property) {
        return isManyToMany(property.getValue());
    }

    public boolean isManyToMany(Value value) {
        return	(value instanceof Collection &&
                ((Collection)value).getElement() instanceof ManyToOne);
    }


    public boolean isCollection(Property property) {
        return property.getValue() instanceof Collection;
    }

    public boolean isOneToManyCollection(Property property) {
        return isCollection(property) && ((Collection)property.getValue()).isOneToMany();
    }

    public boolean isSimpleValue(Property property) {
        return (property.getValue() instanceof SimpleValue);
    }

    public boolean isManyToOne(Property property) {
        return isManyToOne(property.getValue());
    }

    public boolean isManyToAny(Property property) {
        return isManyToAny(property.getValue());
    }

    public boolean isManyToAny(Value value) {
        return (value instanceof Collection &&
                ((Collection)value).getElement() instanceof Any);
    }

    public boolean isManyToOne(Value value) {
        return (value instanceof ManyToOne);
    }

    public boolean isOneToOne(Property property) {
        return (property.getValue() instanceof OneToOne);
    }

    public boolean isTemporalValue(Property property) {
        if(property.getValue() instanceof SimpleValue) {
            String typeName = ((SimpleValue)property.getValue()).getTypeName();
            if("date".equals(typeName) || "java.sql.Date".equals(typeName)) {
                return true;
            }
            else if ("timestamp".equals(typeName) || "java.sql.Timestamp".equals(typeName)) {
                return true;
            }
            else return "time".equals(typeName) || "java.sql.Time".equals(typeName);
        }
        return false;
    }

    public boolean isNamedQueries(Metadata md) {
        final ArrayList<NamedHqlQueryDefinition<?>> list = new ArrayList<>();
        Consumer<NamedHqlQueryDefinition<?>> consumer = list::add;
        md.visitNamedHqlQueryDefinitions(consumer);
        return !list.isEmpty();
    }

    public boolean isNamedSQLQueries(Metadata md) {
        final ArrayList<NamedNativeQueryDefinition<?>> list = new ArrayList<>();
        Consumer<NamedNativeQueryDefinition<?>> consumer = list::add;
        md.visitNamedNativeQueryDefinitions(consumer);
        return !list.isEmpty();
    }


    public String getCollectionLazy(Collection value){
        return value.isExtraLazy() ? "extra" : Boolean.toString(value.isLazy());
    }

    public boolean isFilterDefinitions(Metadata md) {
        Map<String, FilterDefinition> filterdefs = md.getFilterDefinitions();
        return filterdefs != null && !filterdefs.isEmpty();
    }

    public boolean isClassLevelOptimisticLockMode(PersistentClass pc) {
        return pc.getOptimisticLockStyle() != OptimisticLockStyle.VERSION;
    }

    public String getClassLevelOptimisticLockMode(PersistentClass pc) {
        OptimisticLockStyle oMode = pc.getOptimisticLockStyle();
        if ( oMode == OptimisticLockStyle.DIRTY ) {
            return "dirty";
        }
        else if ( oMode == OptimisticLockStyle.ALL ) {
            return "all";
        }
        else if ( oMode == OptimisticLockStyle.NONE ) {
            return "none";
        }
        else {
            return "version";
        }
    }

    public boolean hasFetchMode(Property property) {
        String fetch = getFetchMode(property);
        return fetch != null && !"default".equals(fetch);
    }
    public String getFetchMode(Property property) {
        FetchMode fetchMode = property.getValue().getFetchMode();
        return (fetchMode== null) ? null : fetchMode.toString().toLowerCase();
    }


    public Formula getFormulaForProperty(Property prop) {
        for (Selectable selectable : prop.getValue().getSelectables()) {
            if (selectable instanceof Formula)
                return (Formula) selectable;
        }
        return null;
    }

    public String columnAttributes(Column col) {
        return columnAttributes(col, false);
    }

    public String columnAttributes(Column column, boolean isPrimaryKeyColumn) {
        StringBuilder sb = new StringBuilder();
        if (column.getPrecision() != null) {
            sb.append("precision=\"").append(column.getPrecision() ).append("\" ");
        }
        if (column.getScale() != null) {
            sb.append("scale=\"").append(column.getScale() ).append("\" ");
        }
        else if (column.getLength() != null){
            sb.append("length=\"").append(column.getLength() ).append("\" ");
        }
        if (!isPrimaryKeyColumn) {
            if (!column.isNullable() ) {
                sb.append("not-null=\"true\" ");
            }
            if (column.isUnique() ) {
                sb.append("unique=\"true\" ");
            }
        }
        if (column.getSqlType() != null) {
            sb.append("sql-type=\""); sb.append(column.getSqlType() ); sb.append("\" ");
        }
        return sb.toString();
    }

    public String getClassName(PersistentClass pc) {
        if (pc.hasPojoRepresentation() ) {
            return pc.getClassName();
        }
        else {
            // todo: return null?
            throw new RuntimeException(pc + " does not have a pojo rep.");
        }
    }

    public String getClassName(OneToMany om) {
        return om.getAssociatedClass().getClassName();
    }

    public String getProxyInterfaceName(PersistentClass pc) {
        if (pc.hasPojoRepresentation() ) {
            return pc.getProxyInterfaceName();
        }
        else {
            throw new RuntimeException(pc + " does not have a pojo rep.");
        }
    }

    public boolean isImportData(Metadata md) {
        return !(md.getImports().isEmpty());
    }

    public boolean needsDiscriminatorElement(PersistentClass clazz) {
        return clazz instanceof RootClass
                && (clazz.getDiscriminator() != null);
    }

    public boolean needsDiscriminator(PersistentClass clazz) {

        return clazz instanceof Subclass
                && !(clazz instanceof UnionSubclass) && !(clazz instanceof JoinedSubclass);
    }


    public boolean needsTable(PersistentClass clazz) {

        return (Boolean) clazz.accept(new PersistentClassVisitor(){

            public Object accept(Subclass subclass) {
                return Boolean.FALSE;
            }

            public Object accept(JoinedSubclass subclass) {
                return Boolean.TRUE;
            }

            public Object accept(SingleTableSubclass subclass) {
                return Boolean.FALSE;
            }

            public Object accept(UnionSubclass subclass) {
                return Boolean.TRUE;
            }

            public Object accept(RootClass class1) {
                return Boolean.TRUE;
            }
        });
    }

    public boolean isSubclass(PersistentClass clazz) {
        return clazz instanceof org.hibernate.mapping.Subclass;
    }

    public boolean isJoinedSubclass(PersistentClass clazz) {
        return clazz instanceof JoinedSubclass;
    }

    public boolean hasCustomEntityPersister(PersistentClass clazz) {
        ServiceRegistry sr = clazz.getServiceRegistry();
        PersisterClassResolver pcr = sr.getService(PersisterClassResolver.class);
        if (pcr == null) return false;
        Class<?> entityPersisterClass = pcr.getEntityPersisterClass(clazz);
        if(entityPersisterClass==null) return false;
        final String name = entityPersisterClass.getName();
        return (Boolean) clazz.accept(new HasEntityPersisterVisitor(name));
    }

    public String getHibernateTypeName(Property p) {
        return (String) p.getValue().accept(new EntityNameFromValueVisitor());
    }


    public String getSafeHibernateTypeName(Property p) {
        return (String) p.getValue().accept(new EntityNameFromValueVisitor(false));
    }

    public Iterator<?> getProperties(Component v) {
        return new SkipBackRefPropertyIterator(v.getProperties().iterator());
    }

    public Iterator<?> getProperties(PersistentClass pc) {
        return new SkipBackRefPropertyIterator(pc.getProperties().iterator());
    }
}
