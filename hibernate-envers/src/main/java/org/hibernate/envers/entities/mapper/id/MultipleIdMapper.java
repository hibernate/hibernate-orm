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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.entities.PropertyData;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MultipleIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {
    public MultipleIdMapper(String compositeIdClass) {
        super(compositeIdClass);
    }

    public void mapToMapFromId(Map<String, Object> data, Object obj) {
        for (IdMapper idMapper : ids.values()) {
            idMapper.mapToMapFromEntity(data, obj);
        }
    }

    public void mapToMapFromEntity(Map<String, Object> data, Object obj) {
        mapToMapFromId(data, obj);
    }

    public void mapToEntityFromMap(Object obj, Map data) {
        for (IdMapper idMapper : ids.values()) {
            idMapper.mapToEntityFromMap(obj, data);
        }
    }

    public IdMapper prefixMappedProperties(String prefix) {
        MultipleIdMapper ret = new MultipleIdMapper(compositeIdClass);

        for (PropertyData propertyData : ids.keySet()) {
            String propertyName = propertyData.getName();
            ret.ids.put(propertyData, new SingleIdMapper(new PropertyData(prefix + propertyName, propertyData)));
        }

        return ret;
    }

    public Object mapToIdFromEntity(Object data) {
        if (data == null) {
            return null;
        }

        Object ret;
        try {
            ret = Thread.currentThread().getContextClassLoader().loadClass(compositeIdClass).newInstance();
        } catch (Exception e) {
            throw new AuditException(e);
        }

        for (SingleIdMapper mapper : ids.values()) {
            mapper.mapToEntityFromEntity(ret, data);
        }

        return ret;
    }

    public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        mapToMapFromId(data, obj);

        List<QueryParameterData> ret = new ArrayList<QueryParameterData>();

        for (Map.Entry<String, Object> propertyData : data.entrySet()) {
            ret.add(new QueryParameterData(propertyData.getKey(), propertyData.getValue()));
        }

        return ret;
    }
}
