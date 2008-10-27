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
import org.jboss.envers.entities.mapper.id.IdMapper;
import org.jboss.envers.tools.Tools;
import org.jboss.envers.tools.reflection.ReflectionTools;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.hibernate.property.Setter;
import org.hibernate.collection.PersistentCollection;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ToOneIdMapper implements PropertyMapper {
    private final IdMapper delegate;
    private final String propertyName;
    private final String referencedEntityName;

    public ToOneIdMapper(IdMapper delegate, String propertyName, String referencedEntityName) {
        this.delegate = delegate;
        this.propertyName = propertyName;
        this.referencedEntityName = referencedEntityName;
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        HashMap<String, Object> newData = new HashMap<String, Object>();
        data.put(propertyName, newData);

        delegate.mapToMapFromEntity(newData, newObj);

        return !Tools.objectsEqual(newObj, oldObj);
    }

    public void mapToEntityFromMap(VersionsConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                   VersionsReaderImplementor versionsReader, Number revision) {
        if (obj == null) {
            return;
        }

        Object entityId = delegate.mapToIdFromMap((Map) data.get(propertyName));
        Object value;
        if (entityId == null) {
            value = null;
        } else {
            if (versionsReader.getFirstLevelCache().contains(referencedEntityName, revision, entityId)) {
                value = versionsReader.getFirstLevelCache().get(referencedEntityName, revision, entityId);
            } else {
                Class<?> entityClass = ReflectionTools.loadClass(referencedEntityName);

                value = versionsReader.getSessionImplementor().getFactory().getEntityPersister(referencedEntityName).
                        createProxy(null, new ToOneDelegateSessionImplementor(versionsReader, entityClass, entityId, revision));
            }
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
