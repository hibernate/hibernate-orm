package org.hibernate.envers.configuration;

import org.hibernate.envers.configuration.metadata.reader.ClassAuditingData;
import org.hibernate.mapping.PersistentClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * A helper class holding auditing meta-data for all persistent classes.
 * @author Adam Warski (adam at warski dot org)
 */
public class ClassesAuditingData {
    private final Map<String, ClassAuditingData> entityNameToAuditingData = new HashMap<String, ClassAuditingData>();
    private final Map<PersistentClass, ClassAuditingData> persistentClassToAuditingData = new LinkedHashMap<PersistentClass, ClassAuditingData>();

    /**
     * Stores information about auditing meta-data for the given class.
     * @param pc Persistent class.
     * @param cad Auditing meta-data for the given class.
     */
    public void addClassAuditingData(PersistentClass pc, ClassAuditingData cad) {
        entityNameToAuditingData.put(pc.getEntityName(), cad);
        persistentClassToAuditingData.put(pc, cad);
    }

    /**
     * @return A collection of all auditing meta-data for persistent classes.
     */
    public Collection<Map.Entry<PersistentClass, ClassAuditingData>> getAllClassAuditedData() {
        return persistentClassToAuditingData.entrySet();
    }

    /**
     * @param entityName Name of the entity.
     * @return Auditing meta-data for the given entity.
     */
    public ClassAuditingData getClassAuditingData(String entityName) {
        return entityNameToAuditingData.get(entityName);
    }
}
