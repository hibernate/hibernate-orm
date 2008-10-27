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
package org.jboss.envers.entities.mapper;

import java.util.Map;

/**
 * Data describing the change of a single object in a persisten collection (when the object was added, removed or
 * modified in the collection).
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentCollectionChangeData {
    private final String entityName;
    private final Map<String, Object> data;
    private final Object changedElement;

    public PersistentCollectionChangeData(String entityName, Map<String, Object> data, Object changedElement) {
        this.entityName = entityName;
        this.data = data;
        this.changedElement = changedElement;
    }

    /**
     *
     * @return Name of the (middle) entity that holds the collection data.
     */
    public String getEntityName() {
        return entityName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    /**
     * For use by bi-directional associations.
     * @return The affected element, which was changed (added, removed, modified) in the collection.
     */
    public Object getChangedElement() {
        return changedElement;
    }
}
