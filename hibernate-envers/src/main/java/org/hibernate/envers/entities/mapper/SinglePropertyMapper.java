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
package org.hibernate.envers.entities.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.HibernateException;

/**
 * TODO: diff
 * @author Adam Warski (adam at warski dot org)
 */
public class SinglePropertyMapper implements PropertyMapper, SimpleMapperBuilder {
    private PropertyData propertyData;

    public SinglePropertyMapper(PropertyData propertyData) {
        this.propertyData = propertyData;
    }

    public SinglePropertyMapper() { }

    public void add(PropertyData propertyData) {
        if (this.propertyData != null) {
            throw new AuditException("Only one property can be added!");
        }

        this.propertyData = propertyData;
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        data.put(propertyData.getName(), newObj);

        return !Tools.objectsEqual(newObj, oldObj);
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                   AuditReaderImplementor versionsReader, Number revision) {
        if (data == null || obj == null) {
            return;
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyData);
		Object value = data.get(propertyData.getName());
		// We only set a null value if the field is not primite. Otherwise, we leave it intact.
		if (value != null || !isPrimitive(setter, propertyData, obj.getClass())) {
        	setter.set(obj, value, null);
		}
    }

	private boolean isPrimitive(Setter setter, PropertyData propertyData, Class<?> cls) {
		if (cls == null) {
			throw new HibernateException("No field found for property: " + propertyData.getName());
		}

		if (setter instanceof DirectPropertyAccessor.DirectSetter) {
			// In a direct setter, getMethod() returns null
			// Trying to look up the field
			try {
				return cls.getDeclaredField(propertyData.getBeanName()).getType().isPrimitive();
			} catch (NoSuchFieldException e) {
				return isPrimitive(setter, propertyData, cls.getSuperclass());
			}
		} else {
			return setter.getMethod().getParameterTypes()[0].isPrimitive();
		}
	}

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                     PersistentCollection newColl,
                                                                     Serializable oldColl,
                                                                     Serializable id) {
        return null;
    }

}
