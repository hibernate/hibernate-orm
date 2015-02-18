package org.hibernate.envers.query.internal.impl;

import org.hibernate.envers.tools.Pair;

import java.util.HashMap;
import java.util.Map;

public class InitializationContext {
    private Map<String, Map<Pair<Object, Object>, Object>> initializedInstancesByType =
            new HashMap<String, Map<Pair<Object, Object>, Object>>();

    public void storeInitializedInstance(String type, Object entity, Pair<Object, Object> entIdRev) {
        Map<Pair<Object, Object>, Object> instancesForType = initializedInstancesByType.get(type);
        if (instancesForType == null) {
            instancesForType = new HashMap<Pair<Object, Object>, Object>();
            initializedInstancesByType.put(type, instancesForType);
        }
        instancesForType.put(entIdRev, entity);
    }

    public Object getInstance(String type, Pair<Object, Object> entIdRev) {
        Map<Pair<Object, Object>, Object> instancesForType = initializedInstancesByType.get(type);
        if (instancesForType == null) {
            return null;
        }
        return instancesForType.get(entIdRev);
    }

}
