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
package org.jboss.envers.entities.mapper.id;

import org.jboss.envers.exception.VersionsException;

import java.util.*;

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

        for (String propertyName : ids.keySet()) {
            ret.ids.put(propertyName, new SingleIdMapper(propertyName, prefix + propertyName));
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
            throw new VersionsException(e);
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
