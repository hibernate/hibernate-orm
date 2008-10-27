/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation;

import org.jboss.envers.entities.mapper.PropertyMapper;
import org.jboss.envers.entities.mapper.PersistentCollectionChangeData;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.tools.reflection.ReflectionTools;
import org.jboss.envers.query.VersionsRestrictions;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.hibernate.property.Setter;
import org.hibernate.NonUniqueResultException;
import org.hibernate.collection.PersistentCollection;

import javax.persistence.NoResultException;
import java.util.Map;
import java.util.List;
import java.io.Serializable;

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
