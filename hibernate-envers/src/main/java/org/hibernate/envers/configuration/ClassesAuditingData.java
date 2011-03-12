package org.hibernate.envers.configuration;

import org.hibernate.envers.configuration.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.tools.MappingTools;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * A helper class holding auditing meta-data for all persistent classes.
 * @author Adam Warski (adam at warski dot org)
 */
public class ClassesAuditingData {
    private static final Logger log = LoggerFactory.getLogger(ClassesAuditingData.class);

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

    /**
     * After all meta-data is read, updates calculated fields. This includes:
     * <ul>
     * <li>setting {@code forceInsertable} to {@code true} for properties specified by {@code @AuditMappedBy}</li> 
     * </ul>
     */
    public void updateCalculatedFields() {
        for (Map.Entry<PersistentClass, ClassAuditingData> classAuditingDataEntry : persistentClassToAuditingData.entrySet()) {
            PersistentClass pc = classAuditingDataEntry.getKey();
            ClassAuditingData classAuditingData = classAuditingDataEntry.getValue();
            for (String propertyName : classAuditingData.getPropertyNames()) {
                PropertyAuditingData propertyAuditingData = classAuditingData.getPropertyAuditingData(propertyName);
                // If a property had the @AuditMappedBy annotation, setting the referenced fields to be always insertable.
                if (propertyAuditingData.getAuditMappedBy() != null) {
                    String referencedEntityName = MappingTools.getReferencedEntityName(pc.getProperty(propertyName).getValue());

                    ClassAuditingData referencedClassAuditingData = entityNameToAuditingData.get(referencedEntityName);

                    forcePropertyInsertable(referencedClassAuditingData, propertyAuditingData.getAuditMappedBy(),
                            pc.getEntityName(), referencedEntityName);

                    forcePropertyInsertable(referencedClassAuditingData, propertyAuditingData.getPositionMappedBy(),
                            pc.getEntityName(), referencedEntityName);
                }
            }
        }
    }

    private void forcePropertyInsertable(ClassAuditingData classAuditingData, String propertyName,
                                         String entityName, String referencedEntityName) {
        if (propertyName != null) {
            if (classAuditingData.getPropertyAuditingData(propertyName) == null) {
                throw new MappingException("@AuditMappedBy points to a property that doesn't exist: " +
                    referencedEntityName + "." + propertyName);
            }

            log.debug("Non-insertable property " + referencedEntityName + "." + propertyName +
                    " will be made insertable because a matching @AuditMappedBy was found in the " +
                    entityName + " entity.");

            classAuditingData
                    .getPropertyAuditingData(propertyName)
                    .setForceInsertable(true);
        }
    }
}
