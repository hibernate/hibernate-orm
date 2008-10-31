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
import javax.persistence.MapKey;
import javax.persistence.Version;

import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.*;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.tools.reflection.YClass;
import org.hibernate.envers.tools.reflection.YProperty;
import org.hibernate.envers.tools.reflection.YReflectionManager;

import org.hibernate.MappingException;
import org.hibernate.mapping.PersistentClass;

/**
 * A helper class to read versioning meta-data from annotations on a persistent class.
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 */
public final class AnnotationsMetadataReader {
    private final GlobalConfiguration globalCfg;
    private final YReflectionManager reflectionManager;
    private final PersistentClass pc;

    /**
     * This object is filled with information read from annotations and returned by the <code>getVersioningData</code>
     * method.
     */
    private final PersistentClassVersioningData versioningData;

    public AnnotationsMetadataReader(GlobalConfiguration globalCfg, YReflectionManager reflectionManager,
                                     PersistentClass pc) {
        this.globalCfg = globalCfg;
        this.reflectionManager = reflectionManager;
        this.pc = pc;

        versioningData = new PersistentClassVersioningData();
    }

    private void addPropertyVersioned(YProperty property) {
        Audited ver = property.getAnnotation(Audited.class);
        if (ver != null) {
            versioningData.propertyStoreInfo.propertyStores.put(property.getName(), ver.modStore());
        }
    }

    private void addPropertyMapKey(YProperty property) {
        MapKey mapKey = property.getAnnotation(MapKey.class);
        if (mapKey != null) {
            versioningData.mapKeys.put(property.getName(), mapKey.name());
        }
    }

    private void addPropertyUnversioned(YProperty property) {
        // check if a property is declared as unversioned to exclude it
        // useful if a class is versioned but some properties should be excluded
        NotAudited unVer = property.getAnnotation(NotAudited.class);
        if (unVer != null) {
            versioningData.unversionedProperties.add(property.getName());
        } else {
            // if the optimistic locking field has to be unversioned and the current property
            // is the optimistic locking field, add it to the unversioned properties list
            if (globalCfg.isUnversionedOptimisticLockingField()) {
                Version jpaVer = property.getAnnotation(Version.class);
                if (jpaVer != null) {
                    versioningData.unversionedProperties.add(property.getName());
                }
            }
        }
    }

    private void addPropertyJoinTables(YProperty property) {
        AuditJoinTable joinTable = property.getAnnotation(AuditJoinTable.class);
        if (joinTable != null) {
            versioningData.versionsJoinTables.put(property.getName(), joinTable);
        }
    }

    private void addFromProperties(Iterable<YProperty> properties) {
        for (YProperty property : properties) {
            addPropertyVersioned(property);
            addPropertyUnversioned(property);
            addPropertyJoinTables(property);
            addPropertyMapKey(property);
        }
    }

    private void addPropertiesFromClass(YClass clazz)  {
        YClass superclazz = clazz.getSuperclass();
        if (!"java.lang.Object".equals(superclazz.getName())) {
            addPropertiesFromClass(superclazz);
        }

        addFromProperties(clazz.getDeclaredProperties("field"));
        addFromProperties(clazz.getDeclaredProperties("property"));
    }

    private void addDefaultVersioned(YClass clazz) {
        Audited defaultVersioned = clazz.getAnnotation(Audited.class);

        if (defaultVersioned != null) {
            versioningData.propertyStoreInfo.defaultStore = defaultVersioned.modStore();
        }
    }

    private void addVersionsTable(YClass clazz) {
        AuditTable versionsTable = clazz.getAnnotation(AuditTable.class);
        if (versionsTable != null) {
            versioningData.versionsTable = versionsTable;
        } else {
            versioningData.versionsTable = getDefaultVersionsTable();
        }
    }

    private void addVersionsSecondaryTables(YClass clazz) {
        // Getting information on secondary tables
        SecondaryAuditTable secondaryVersionsTable1 = clazz.getAnnotation(SecondaryAuditTable.class);
        if (secondaryVersionsTable1 != null) {
            versioningData.secondaryTableDictionary.put(secondaryVersionsTable1.secondaryTableName(),
                    secondaryVersionsTable1.secondaryVersionsTableName());
        }

        SecondaryAuditTables secondaryVersionsTables = clazz.getAnnotation(SecondaryAuditTables.class);
        if (secondaryVersionsTables != null) {
            for (SecondaryAuditTable secondaryVersionsTable2 : secondaryVersionsTables.value()) {
                versioningData.secondaryTableDictionary.put(secondaryVersionsTable2.secondaryTableName(),
                        secondaryVersionsTable2.secondaryVersionsTableName());
            }
        }
    }

    public PersistentClassVersioningData getVersioningData() {
        if (pc.getClassName() == null) {
            return versioningData;
        }

        try {
            YClass clazz = reflectionManager.classForName(pc.getClassName(), this.getClass());

            addDefaultVersioned(clazz);
            addPropertiesFromClass(clazz);
            addVersionsTable(clazz);
            addVersionsSecondaryTables(clazz);
        } catch (ClassNotFoundException e) {
            throw new MappingException(e);
        }

        return versioningData;
    }

    private AuditTable getDefaultVersionsTable() {
        return new AuditTable() {
            public String value() { return ""; }
            public String schema() { return ""; }
            public String catalog() { return ""; }
            public Class<? extends Annotation> annotationType() { return this.getClass(); }
        };
    }
}
