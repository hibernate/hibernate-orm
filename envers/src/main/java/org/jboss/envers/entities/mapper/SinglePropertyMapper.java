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
package org.jboss.envers.entities.mapper;

import org.jboss.envers.ModificationStore;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.tools.reflection.ReflectionTools;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.tools.Tools;
import org.jboss.envers.exception.VersionsException;
import org.hibernate.property.Setter;
import org.hibernate.collection.PersistentCollection;

import java.util.Map;
import java.util.List;
import java.io.Serializable;

/**
 * TODO: diff
 * @author Adam Warski (adam at warski dot org)
 */
public class SinglePropertyMapper implements PropertyMapper, SimpleMapperBuilder {
    private String propertyName;

    public SinglePropertyMapper() { }

    public void add(String propertyName, ModificationStore modStore) {
        if (this.propertyName != null) {
            throw new VersionsException("Only one property can be added!");
        }

        this.propertyName = propertyName;
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        data.put(propertyName, newObj);

        return !Tools.objectsEqual(newObj, oldObj);
    }

    public void mapToEntityFromMap(VersionsConfiguration verCfg, Object obj, Map data, Object primaryKey, VersionsReaderImplementor versionsReader, Number revision) {
        if (data == null || obj == null) {
            return;
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyName);
        setter.set(obj, data.get(propertyName), null);
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        return null;
    }

}
