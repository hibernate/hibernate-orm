package org.jboss.envers.reader;

import org.jboss.envers.tools.Triple;
import static org.jboss.envers.tools.Triple.*;
import static org.jboss.envers.tools.Tools.newHashMap;

import java.util.Map;

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
