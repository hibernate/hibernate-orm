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

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.Iterator;
import javax.persistence.MapKey;
import javax.persistence.Version;
import javax.persistence.JoinColumn;

import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.*;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.configuration.GlobalConfiguration;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * A helper class to read versioning meta-data from annotations on a persistent class.
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 */
public final class AnnotationsMetadataReader {
    private final GlobalConfiguration globalCfg;
    private final ReflectionManager reflectionManager;
    private final PersistentClass pc;

    /**
     * This object is filled with information read from annotations and returned by the <code>getVersioningData</code>
     * method.
     */
    private final PersistentClassAuditingData auditData;

    /**
     * Not null if the whole class is annotated with @Audited; the variable then holds the value of this
     * annotation's "modStore" parameter.
     */
    private ModificationStore defaultStore;

    private Set<String> propertyPersistentProperties;
    private Set<String> fieldPersistentProperties;

    public AnnotationsMetadataReader(GlobalConfiguration globalCfg, ReflectionManager reflectionManager,
                                     PersistentClass pc) {
        this.globalCfg = globalCfg;
        this.reflectionManager = reflectionManager;
        this.pc = pc;

        auditData = new PersistentClassAuditingData();

        propertyPersistentProperties = Tools.newHashSet();
        fieldPersistentProperties = Tools.newHashSet();
    }

    /**
     * Checks if a property is audited and if yes, sets its modification store on the supplied property data.
     * @param property Property to check.
     * @param propertyData Property data, on which to set this property's modification store.
     * @param persistentProperties Persistent properties with the access type of the given property.
     * @return True, iff this property is audited.
     */
    private boolean ifPropertyAuditedAddStore(XProperty property, PersistentPropertyAuditingData propertyData,
                                              Set<String> persistentProperties) {
        // If this is not a persistent property, with the same access type as currently checked,
        // it's not audited as well.
        if (!persistentProperties.contains(property.getName())) {
            return false;
        }

        // check if a property is declared as not audited to exclude it
        // useful if a class is audited but some properties should be excluded
        NotAudited unVer = property.getAnnotation(NotAudited.class);
        if (unVer != null) {
            return false;
        } else {
            // if the optimistic locking field has to be unversioned and the current property
            // is the optimistic locking field, don't audit it
            if (globalCfg.isUnversionedOptimisticLockingField()) {
                Version jpaVer = property.getAnnotation(Version.class);
                if (jpaVer != null) {
                    return false;
                }
            }
        }

        // Checking if this property is explicitly audited or if all properties are.
        Audited ver = property.getAnnotation(Audited.class);
        if (ver != null) {
            propertyData.setStore(ver.modStore());
            return true;
        } else {
            if (defaultStore != null) {
                propertyData.setStore(defaultStore);
                return true;
            } else {
                return false;
            }
        }
    }

    private void addPropertyMapKey(XProperty property, PersistentPropertyAuditingData propertyData) {
        MapKey mapKey = property.getAnnotation(MapKey.class);
        if (mapKey != null) {
            propertyData.setMapKey(mapKey.name());
        }
    }

    private void addPropertyJoinTables(XProperty property, PersistentPropertyAuditingData propertyData) {
        AuditJoinTable joinTable = property.getAnnotation(AuditJoinTable.class);
        if (joinTable != null) {
            propertyData.setJoinTable(joinTable);
        } else {
            propertyData.setJoinTable(getDefaultAuditJoinTable());
        }
    }

    private void addFromProperties(Iterable<XProperty> properties, String accessType, Set<String> persistenProperties) {
        for (XProperty property : properties) {
            PersistentPropertyAuditingData propertyData = new PersistentPropertyAuditingData();

            if (ifPropertyAuditedAddStore(property, propertyData, persistenProperties)) {
                // Now we know that the property is audited
                auditData.getProperties().put(property.getName(), propertyData);

                propertyData.setName(property.getName());
                propertyData.setAccessType(accessType);

                addPropertyJoinTables(property, propertyData);
                addPropertyMapKey(property, propertyData);
            }
        }
    }

    private void addPropertiesFromClass(XClass clazz)  {
        XClass superclazz = clazz.getSuperclass();
        if (!"java.lang.Object".equals(superclazz.getName())) {
            addPropertiesFromClass(superclazz);
        }

        addFromProperties(clazz.getDeclaredProperties("field"), "field", fieldPersistentProperties);
        addFromProperties(clazz.getDeclaredProperties("property"), "property", propertyPersistentProperties);
    }

    private void readDefaultAudited(XClass clazz) {
        Audited defaultAudited = clazz.getAnnotation(Audited.class);

        if (defaultAudited != null) {
            defaultStore = defaultAudited.modStore();
        }
    }

    private void readPersistentProperties() {
        Iterator propertyIter = pc.getPropertyIterator();
        while (propertyIter.hasNext()) {
            Property property = (Property) propertyIter.next();
            if ("field".equals(property.getPropertyAccessorName())) {
                fieldPersistentProperties.add(property.getName());
            } else {
                propertyPersistentProperties.add(property.getName());
            }
        }
    }

    private void addVersionsTable(XClass clazz) {
        AuditTable auditTable = clazz.getAnnotation(AuditTable.class);
        if (auditTable != null) {
            auditData.setAuditTable(auditTable);
        } else {
            auditData.setAuditTable(getDefaultAuditTable());
        }
    }

    private void addVersionsSecondaryTables(XClass clazz) {
        // Getting information on secondary tables
        SecondaryAuditTable secondaryVersionsTable1 = clazz.getAnnotation(SecondaryAuditTable.class);
        if (secondaryVersionsTable1 != null) {
            auditData.getSecondaryTableDictionary().put(secondaryVersionsTable1.secondaryTableName(),
                    secondaryVersionsTable1.secondaryVersionsTableName());
        }

        SecondaryAuditTables secondaryVersionsTables = clazz.getAnnotation(SecondaryAuditTables.class);
        if (secondaryVersionsTables != null) {
            for (SecondaryAuditTable secondaryVersionsTable2 : secondaryVersionsTables.value()) {
                auditData.getSecondaryTableDictionary().put(secondaryVersionsTable2.secondaryTableName(),
                        secondaryVersionsTable2.secondaryVersionsTableName());
            }
        }
    }

    public PersistentClassAuditingData getAuditData() {
        if (pc.getClassName() == null) {
            return auditData;
        }

        readPersistentProperties();

        try {
            XClass clazz = reflectionManager.classForName(pc.getClassName(), this.getClass());

            readDefaultAudited(clazz);
            addPropertiesFromClass(clazz);
            addVersionsTable(clazz);
            addVersionsSecondaryTables(clazz);
        } catch (ClassNotFoundException e) {
            throw new MappingException(e);
        }

        return auditData;
    }

    private AuditTable defaultAuditTable;
    private AuditTable getDefaultAuditTable() {
        if (defaultAuditTable == null) {
            defaultAuditTable =  new AuditTable() {
                public String value() { return ""; }
                public String schema() { return ""; }
                public String catalog() { return ""; }
                public Class<? extends Annotation> annotationType() { return this.getClass(); }
            };
        }

        return defaultAuditTable;
    }

    private AuditJoinTable defaultAuditJoinTable;
    private AuditJoinTable getDefaultAuditJoinTable() {
        if (defaultAuditJoinTable == null) {
            defaultAuditJoinTable = new AuditJoinTable() {
                public String name() { return ""; }
                public String schema() { return ""; }
                public String catalog() { return ""; }
                public JoinColumn[] inverseJoinColumns() { return new JoinColumn[0]; }
                public Class<? extends Annotation> annotationType() { return this.getClass(); }
            };
        }

        return defaultAuditJoinTable;
    }
}
