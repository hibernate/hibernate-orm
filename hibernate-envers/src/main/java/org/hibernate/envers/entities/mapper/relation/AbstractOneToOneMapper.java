package org.hibernate.envers.entities.mapper.relation;

import org.hibernate.NonUniqueResultException;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.reader.AuditReaderImplementor;

import javax.persistence.NoResultException;
import java.io.Serializable;
import java.util.Map;

/**
 * Template class for property mappers that manage one-to-one relation.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractOneToOneMapper extends AbstractToOneMapper {
    private final String entityName;
    private final String referencedEntityName;

    protected AbstractOneToOneMapper(String entityName, String referencedEntityName, PropertyData propertyData) {
        super(propertyData);
        this.entityName = entityName;
        this.referencedEntityName = referencedEntityName;
    }

    @Override
    public void nullSafeMapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                           AuditReaderImplementor versionsReader, Number revision) {
        EntityInfo referencedEntity = getEntityInfo(verCfg, referencedEntityName);

        Object value = null;
        try {
            value = queryForReferencedEntity(versionsReader, referencedEntity, (Serializable) primaryKey, revision);
        } catch (NoResultException e) {
            value = null;
        } catch (NonUniqueResultException e) {
            throw new AuditException("Many versions results for one-to-one relationship " + entityName +
                                     "." + getPropertyData().getBeanName() + ".", e);
        }

        setPropertyValue(obj, value);
    }

    /**
     * @param versionsReader Audit reader.
     * @param referencedEntity Referenced entity descriptor.
     * @param primaryKey Referenced entity identifier.
     * @param revision Revision number.
     * @return Referenced object or proxy of one-to-one relation.
     */
    protected abstract Object queryForReferencedEntity(AuditReaderImplementor versionsReader, EntityInfo referencedEntity,
                                                       Serializable primaryKey, Number revision);
}
