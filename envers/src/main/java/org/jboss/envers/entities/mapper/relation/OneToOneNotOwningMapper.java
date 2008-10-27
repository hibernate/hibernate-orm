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
package org.jboss.envers.entities.mapper.relation;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;

import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.entities.mapper.PersistentCollectionChangeData;
import org.jboss.envers.entities.mapper.PropertyMapper;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.query.VersionsRestrictions;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.tools.reflection.ReflectionTools;

import org.hibernate.NonUniqueResultException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class OneToOneNotOwningMapper implements PropertyMapper {
    private String owningReferencePropertyName;
    private String owningEntityName;
    private String propertyName;

    public OneToOneNotOwningMapper(String owningReferencePropertyName, String owningEntityName, String propertyName) {
        this.owningReferencePropertyName = owningReferencePropertyName;
        this.owningEntityName = owningEntityName;
        this.propertyName = propertyName;
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        return false;
    }

    public void mapToEntityFromMap(VersionsConfiguration verCfg, Object obj, Map data, Object primaryKey, VersionsReaderImplementor versionsReader, Number revision) {
        if (obj == null) {
            return;
        }

        Class<?> entityClass = ReflectionTools.loadClass(owningEntityName);

        Object value;

        try {
            value = versionsReader.createQuery().forEntitiesAtRevision(entityClass, revision)
                    .add(VersionsRestrictions.relatedIdEq(owningReferencePropertyName, primaryKey)).getSingleResult();
        } catch (NoResultException e) {
            value = null;
        } catch (NonUniqueResultException e) {
            throw new VersionsException("Many versions results for one-to-one relationship: (" + owningEntityName +
                    ", " + owningReferencePropertyName + ")");
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyName);
        setter.set(obj, value, null);
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        return null;
    }
}
