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
package org.hibernate.envers.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.envers.entities.EntityInstantiator;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.envers.tools.reflection.ReflectionTools;

/**
 * A component mapper for the @MapKey mapping with the name parameter specified: the value of the map's key
 * is a property of the entity. This doesn't have an effect on the data stored in the versions tables,
 * so <code>mapToMapFromObject</code> is empty.
 * @author Adam Warski (adam at warski dot org)
 */
public class MiddleMapKeyPropertyComponentMapper implements MiddleComponentMapper {
    private final String propertyName;
    private final String accessType;

    public MiddleMapKeyPropertyComponentMapper(String propertyName, String accessType) {
        this.propertyName = propertyName;
        this.accessType = accessType;
    }

    public Object mapToObjectFromFullMap(EntityInstantiator entityInstantiator, Map<String, Object> data,
                                         Object dataObject, Number revision) {
        // dataObject is not null, as this mapper can only be used in an index.
        return ReflectionTools.getGetter(dataObject.getClass(), propertyName, accessType).get(dataObject);
    }

    public void mapToMapFromObject(Map<String, Object> data, Object obj) {
        // Doing nothing.
    }

    public void addMiddleEqualToQuery(Parameters parameters, String prefix1, String prefix2) {
        // Doing nothing.
    }
}