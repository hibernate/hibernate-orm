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
package org.hibernate.envers.entities.mapper.id;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SingleIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
    private PropertyData propertyData;

    public SingleIdMapper() {
    }

    public SingleIdMapper(PropertyData propertyData) {
        this.propertyData = propertyData;
    }

    public void add(PropertyData propertyData) {
        if (this.propertyData != null) {
            throw new AuditException("Only one property can be added!");
        }

        this.propertyData = propertyData;
    }

    public void mapToEntityFromMap(Object obj, Map data) {
        if (data == null || obj == null) {
            return;
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyData);
        setter.set(obj, data.get(propertyData.getName()), null);
    }

    public Object mapToIdFromMap(Map data) {
        if (data == null) {
            return null;
        }

        return data.get(propertyData.getName());
    }

    public Object mapToIdFromEntity(Object data) {
        if (data == null) {
            return null;
        }

        if(data instanceof HibernateProxy) {
        	HibernateProxy hibernateProxy = (HibernateProxy) data;
        	return hibernateProxy.getHibernateLazyInitializer().getIdentifier();
        } else {
        	Getter getter = ReflectionTools.getGetter(data.getClass(), propertyData);
            return getter.get(data);
        }
    }

    public void mapToMapFromId(Map<String, Object> data, Object obj) {
        if (data != null) {
            data.put(propertyData.getName(), obj);
        }
    }

    public void mapToMapFromEntity(Map<String, Object> data, Object obj) {
        if (obj == null) {
            data.put(propertyData.getName(), null);
        } else {
            if(obj instanceof HibernateProxy) {
            	HibernateProxy hibernateProxy = (HibernateProxy)obj;
            	data.put(propertyData.getName(), hibernateProxy.getHibernateLazyInitializer().getIdentifier());
            } else {
            	Getter getter = ReflectionTools.getGetter(obj.getClass(), propertyData);
            	data.put(propertyData.getName(), getter.get(obj));
            }
        }
    }

    public void mapToEntityFromEntity(Object objTo, Object objFrom) {
        if (objTo == null || objFrom == null) {
            return;
        }

        Getter getter = ReflectionTools.getGetter(objFrom.getClass(), propertyData);
        Setter setter = ReflectionTools.getSetter(objTo.getClass(), propertyData);
        setter.set(objTo, getter.get(objFrom), null);
    }

    public IdMapper prefixMappedProperties(String prefix) {
        return new SingleIdMapper(new PropertyData(prefix + propertyData.getName(), propertyData));
    }

    public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
        List<QueryParameterData> ret = new ArrayList<QueryParameterData>();

        ret.add(new QueryParameterData(propertyData.getName(), obj));

        return ret;
    }
}
