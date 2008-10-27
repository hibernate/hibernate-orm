/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation.component;

import org.jboss.envers.entities.EntityInstantiator;
import org.jboss.envers.tools.query.Parameters;
import org.jboss.envers.tools.reflection.ReflectionTools;

import java.util.Map;

/**
 * A component mapper for the @MapKey mapping with the name parameter specified: the value of the map's key
 * is a property of the entity. This doesn't have an effect on the data stored in the versions tables,
 * so <code>mapToMapFromObject</code> is empty.
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleMapKeyPropertyComponentMapper implements MiddleComponentMapper {
    private final String propertyName;

    public MiddleMapKeyPropertyComponentMapper(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object mapToObjectFromFullMap(EntityInstantiator entityInstantiator, Map<String, Object> data,
                                         Object dataObject, Number revision) {
        // dataObject is not null, as this mapper can only be used in an index.
        return ReflectionTools.getGetter(dataObject.getClass(), propertyName).get(dataObject);
    }

    public void mapToMapFromObject(Map<String, Object> data, Object obj) {
        // Doing nothing.
    }

    public void addMiddleEqualToQuery(Parameters parameters, String prefix1, String prefix2) {
        // Doing nothing.
    }
}