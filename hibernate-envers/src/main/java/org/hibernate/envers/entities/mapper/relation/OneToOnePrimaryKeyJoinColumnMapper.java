package org.hibernate.envers.entities.mapper.relation;

import org.hibernate.NonUniqueResultException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.Setter;

import javax.persistence.NoResultException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OneToOnePrimaryKeyJoinColumnMapper implements PropertyMapper {
    private final String entityName;
    private final String referencedEntityName;
    private final PropertyData propertyData;

    public OneToOnePrimaryKeyJoinColumnMapper(String entityName, String referencedEntityName, PropertyData propertyData) {
        this.entityName = entityName;
        this.referencedEntityName = referencedEntityName;
        this.propertyData = propertyData;
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        return false;
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey, AuditReaderImplementor versionsReader, Number revision) {
        if (obj == null) {
            return;
        }

    	EntityConfiguration entCfg = verCfg.getEntCfg().get(referencedEntityName);
        boolean isRelationAudited = true;
    	if (entCfg == null) {
    		// a relation marked as RelationTargetAuditMode.NOT_AUDITED
    		entCfg = verCfg.getEntCfg().getNotVersionEntityConfiguration(referencedEntityName);
            isRelationAudited = false;
    	}

        Class<?> entityClass = ReflectionTools.loadClass(entCfg.getEntityClassName());
        Object value = null;
        try {
            if (isRelationAudited) {
                value = versionsReader.createQuery().forEntitiesAtRevision(entityClass, referencedEntityName, revision)
                                                    .add(AuditEntity.id().eq(primaryKey)).getSingleResult();
            } else {
                value = createNotAuditedEntityReference(versionsReader, entityClass, referencedEntityName,
                                                        (Serializable) primaryKey);
            }
        } catch (NoResultException e) {
            value = null;
        } catch (NonUniqueResultException e) {
            throw new AuditException("Many versions results for one-to-one relationship: (" + entityName +
                                     ", " + propertyData.getBeanName() + ")");
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyData);
        setter.set(obj, value, null);
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                     PersistentCollection newColl,
                                                                     Serializable oldColl,
                                                                     Serializable id) {
        return null;
    }

    /**
     * Create Hibernate proxy or retrieve the complete object of referenced, not audited entity. According to
     * {@link Audited#targetAuditMode()}} documentation, reference shall point to current (non-historical) version
     * of an entity.
     */
    private Object createNotAuditedEntityReference(AuditReaderImplementor versionsReader, Class<?> entityClass,
                                                   String entityName, Serializable primaryKey) {
        EntityPersister entityPersister = versionsReader.getSessionImplementor().getFactory().getEntityPersister(entityName);
        if (entityPersister.hasProxy()) {
            // If possible create a proxy. Returning complete object may affect performance.
            return versionsReader.getSession().load(entityClass, primaryKey);
        } else {
            // If proxy is not allowed (e.g. @Proxy(lazy=false)) construct the original object.
            return versionsReader.getSession().get(entityClass, primaryKey);
        }
    }
}