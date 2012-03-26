package org.hibernate.envers.entities.mapper.relation;

import java.io.Serializable;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.envers.Audited;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Property mapper for {@link OneToOne} with {@link PrimaryKeyJoinColumn} relation.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OneToOnePrimaryKeyJoinColumnMapper extends AbstractOneToOneMapper {
    public OneToOnePrimaryKeyJoinColumnMapper(String entityName, String referencedEntityName, PropertyData propertyData) {
        super(entityName, referencedEntityName, propertyData);
    }

    @Override
    protected Object queryForReferencedEntity(AuditReaderImplementor versionsReader, EntityInfo referencedEntity,
                                              Serializable primaryKey, Number revision) {
        if (referencedEntity.isAudited()) {
            // Audited relation.
            return versionsReader.createQuery().forEntitiesAtRevision(referencedEntity.getEntityClass(),
                                                                      referencedEntity.getEntityName(), revision)
                                               .add(AuditEntity.id().eq(primaryKey))
                                               .getSingleResult();
        } else {
            // Not audited relation.
            return createNotAuditedEntityReference(versionsReader, referencedEntity.getEntityClass(),
                                                   referencedEntity.getEntityName(), primaryKey);
        }
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