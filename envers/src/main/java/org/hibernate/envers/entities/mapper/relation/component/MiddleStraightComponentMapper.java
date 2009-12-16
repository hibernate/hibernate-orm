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

import org.hibernate.envers.entities.EntityInstantiator;
import org.hibernate.envers.tools.query.Parameters;

import java.util.Map;

/**
 * A mapper for reading and writing a property straight to/from maps. This mapper cannot be used with middle tables,
 * but only with "fake" bidirectional indexed relations. 
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleStraightComponentMapper implements MiddleComponentMapper {
    private final String propertyName;

    public MiddleStraightComponentMapper(String propertyName) {
        this.propertyName = propertyName;
    }

    @SuppressWarnings({"unchecked"})
    public Object mapToObjectFromFullMap(EntityInstantiator entityInstantiator, Map<String, Object> data,
                                         Object dataObject, Number revision) {
        return data.get(propertyName);
    }

    public void mapToMapFromObject(Map<String, Object> data, Object obj) {
        data.put(propertyName, obj);
    }

    public void addMiddleEqualToQuery(Parameters parameters, String prefix1, String prefix2) {
        throw new UnsupportedOperationException("Cannot use this mapper with a middle table!");
    }
}