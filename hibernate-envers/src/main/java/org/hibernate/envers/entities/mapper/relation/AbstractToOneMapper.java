package org.hibernate.envers.entities.mapper.relation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Base class for property mappers that represent to-one relation.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractToOneMapper implements PropertyMapper {
    private final PropertyData propertyData;

    protected AbstractToOneMapper(PropertyData propertyData) {
        this.propertyData = propertyData;
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        return false;
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                   AuditReaderImplementor versionsReader, Number revision) {
        if (obj != null) {
            nullSafeMapToEntityFromMap(verCfg, obj, data, primaryKey, versionsReader, revision);
        }
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                     PersistentCollection newColl,
                                                                     Serializable oldColl, Serializable id) {
        return null;
    }

    /**
     * @param verCfg Audit configuration.
     * @param entityName Entity name.
     * @return Entity class, name and information whether it is audited or not.
     */
    protected RelationDescriptor getRelationDescriptor(AuditConfiguration verCfg, String entityName) {
        EntityConfiguration entCfg = verCfg.getEntCfg().get(entityName);
        boolean isRelationAudited = true;
        if (entCfg == null) {
            // a relation marked as RelationTargetAuditMode.NOT_AUDITED
            entCfg = verCfg.getEntCfg().getNotVersionEntityConfiguration(entityName);
            isRelationAudited = false;
        }
        Class entityClass = ReflectionTools.loadClass(entCfg.getEntityClassName());
        return new RelationDescriptor(entityClass, entityName, isRelationAudited);
    }

    protected void setPropertyValue(Object targetObject, Object value) {
        Setter setter = ReflectionTools.getSetter(targetObject.getClass(), propertyData);
        setter.set(targetObject, value, null);
    }

    /**
     * @return Bean property that represents the relation.
     */
    protected PropertyData getPropertyData() {
        return propertyData;
    }

    /**
     * Parameter {@code obj} is never {@code null}.
     * @see PropertyMapper#mapToEntityFromMap(AuditConfiguration, Object, Map, Object, AuditReaderImplementor, Number)
     */
    public abstract void nullSafeMapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                                    AuditReaderImplementor versionsReader, Number revision);

    /**
     * Simple descriptor of managed relation.
     */
    protected class RelationDescriptor {
        private final Class referencedEntityClass;
        private final String referencedEntityName;
        private final boolean audited;

        public RelationDescriptor(Class referencedEntityClass, String referencedEntityName, boolean audited) {
            this.referencedEntityClass = referencedEntityClass;
            this.referencedEntityName = referencedEntityName;
            this.audited = audited;
        }

        public Class getReferencedEntityClass() { return referencedEntityClass; }
        public String getReferencedEntityName() { return referencedEntityName; }
        public boolean isAudited() { return audited; }
    }
}
