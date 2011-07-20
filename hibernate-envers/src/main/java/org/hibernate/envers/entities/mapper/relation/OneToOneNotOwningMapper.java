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
package org.hibernate.envers.entities.mapper.relation;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import org.hibernate.Criteria;
import org.hibernate.NonUniqueResultException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern�n Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OneToOneNotOwningMapper implements PropertyMapper {
    private String owningReferencePropertyName;
    private String owningEntityName;
    private PropertyData propertyData;

    public OneToOneNotOwningMapper(String owningReferencePropertyName, String owningEntityName,
                                   PropertyData propertyData) {
        this.owningReferencePropertyName = owningReferencePropertyName;
        this.owningEntityName = owningEntityName;
        this.propertyData = propertyData;
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        return false;
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey, AuditReaderImplementor versionsReader, Number revision) {
        if (obj == null) {
            return;
        }

    	EntityConfiguration entCfg = verCfg.getEntCfg().get(owningEntityName);
        boolean targetRelationAudited = true;
    	if(entCfg == null) {
    		// a relation marked as RelationTargetAuditMode.NOT_AUDITED 
    		entCfg = verCfg.getEntCfg().getNotVersionEntityConfiguration(owningEntityName);
            targetRelationAudited = false;
    	}

        Class<?> entityClass = ReflectionTools.loadClass(entCfg.getEntityClassName());

        Object value;

        try {
            if (targetRelationAudited) {
                value = versionsReader.createQuery().forEntitiesAtRevision(entityClass, owningEntityName, revision)
                                      .add(AuditEntity.relatedId(owningReferencePropertyName).eq(primaryKey)).getSingleResult();
            } else {
                value = createNotAuditedRelationOwningReference(verCfg, versionsReader, revision, entityClass, primaryKey);
            }
        } catch (NoResultException e) {
            value = null;
        } catch (NonUniqueResultException e) {
            throw new AuditException("Many versions results for one-to-one relationship: (" + owningEntityName +
                    ", " + owningReferencePropertyName + ")");
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyData);
        setter.set(obj, value, null);
    }

    /**
     * Create Hibernate proxy or retrieve the complete object of referenced entity which is not audited but owns the relation.
     * According to {@link Audited#targetAuditMode()}} documentation, reference shall point to current (non-historical)
     * version of an entity.
     */
    private Object createNotAuditedRelationOwningReference(AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
                                                           Number revision, Class<?> entityClass, Object primaryKey) {
        EntityPersister owningEntityPersister = versionsReader.getSessionImplementor().getFactory().getEntityPersister(owningEntityName);
        String owningReferencePropertyPath = owningReferencePropertyName + "." + owningEntityPersister.getIdentifierPropertyName();
        Criteria criteria = versionsReader.getSession().createCriteria(entityClass);
        if (owningEntityPersister.hasProxy()) {
            // Retrieve ID of related entity to create proxy object. Returning complete object may affect performance.
            Serializable owningId = (Serializable) criteria.setProjection(Projections.id())
                                                           .add(Restrictions.eq(owningReferencePropertyPath, primaryKey))
                                                           .uniqueResult();
            return versionsReader.getSessionImplementor().getFactory().getEntityPersister(owningEntityName)
                                 .createProxy(owningId, new ToOneDelegateSessionImplementor(versionsReader, entityClass, owningId, revision, verCfg));
        } else {
            // If proxy is not allowed (e.g. @Proxy(lazy=false)) construct the original object.
            return criteria.add(Restrictions.eq(owningReferencePropertyPath, primaryKey)).uniqueResult();
        }
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        return null;
    }
}
