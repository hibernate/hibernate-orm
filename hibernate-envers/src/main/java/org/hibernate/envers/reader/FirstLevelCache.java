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
package org.hibernate.envers.reader;

import java.util.Map;

import static org.hibernate.envers.tools.Tools.newHashMap;
import org.hibernate.envers.tools.Triple;
import static org.hibernate.envers.tools.Triple.make;

/**
 * First level cache for versioned entities, versions reader-scoped. Each entity is uniquely identified by a
 * revision number and entity id.
 * @author Adam Warski (adam at warski dot org)
 */
public class FirstLevelCache {
    private final Map<Triple<String, Number, Object>, Object> cache;

    public FirstLevelCache() {
        cache = newHashMap();
    }

    public Object get(String entityName, Number revision, Object id) {
        return cache.get(make(entityName, revision, id));
    }

    public void put(String entityName, Number revision, Object id, Object entity) {
        cache.put(make(entityName, revision, id), entity);
    }

    public boolean contains(String entityName, Number revision, Object id) {
        return cache.containsKey(make(entityName, revision, id));
    }
}
