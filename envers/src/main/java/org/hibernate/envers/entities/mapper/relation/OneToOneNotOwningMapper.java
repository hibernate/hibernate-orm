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
package org.hibernate.envers.entities.mapper.relation;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.NonUniqueResultException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class OneToOneNotOwningMapper implements PropertyMapper {
    private String owningReferencePropertyName;
    private String owningEntityName;
    private PropertyData propertyData;

    public OneToOneNotOwningMapper(String owningReferencePropertyName, String owningEntityName,
                                   PropertyData propertyData) {
        this.owningReferencePropertyName = owningReferencePropertyName;
        this.owningEntityName = owningEntityName;
        this.propertyData = propertyData;
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        return false;
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey, AuditReaderImplementor versionsReader, Number revision) {
        if (obj == null) {
            return;
        }

        Class<?> entityClass = ReflectionTools.loadClass(owningEntityName);

        Object value;

        try {
            value = versionsReader.createQuery().forEntitiesAtRevision(entityClass, revision)
                    .add(AuditEntity.relatedId(owningReferencePropertyName).eq(primaryKey)).getSingleResult();
        } catch (NoResultException e) {
            value = null;
        } catch (NonUniqueResultException e) {
            throw new AuditException("Many versions results for one-to-one relationship: (" + owningEntityName +
                    ", " + owningReferencePropertyName + ")");
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyData);
        setter.set(obj, value, null);
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        return null;
    }
}
