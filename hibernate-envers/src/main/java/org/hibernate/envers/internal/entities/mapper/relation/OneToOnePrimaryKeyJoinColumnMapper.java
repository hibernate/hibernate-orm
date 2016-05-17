/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;

/**
 * Property mapper for {@link javax.persistence.OneToOne} with {@link javax.persistence.PrimaryKeyJoinColumn} relation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OneToOnePrimaryKeyJoinColumnMapper extends AbstractOneToOneMapper {
	public OneToOnePrimaryKeyJoinColumnMapper(
			String entityName,
			String referencedEntityName,
			PropertyData propertyData,
			ServiceRegistry serviceRegistry) {
		super( entityName, referencedEntityName, propertyData, serviceRegistry );
	}

	@Override
	protected Object queryForReferencedEntity(
			AuditReaderImplementor versionsReader, EntityInfo referencedEntity,
			Serializable primaryKey, Number revision) {
		if ( referencedEntity.isAudited() ) {
			// Audited relation.
			return versionsReader.createQuery().forEntitiesAtRevision(
					referencedEntity.getEntityClass(),
					referencedEntity.getEntityName(), revision
			)
					.add( AuditEntity.id().eq( primaryKey ) )
					.getSingleResult();
		}
		else {
			// Not audited relation.
			return createNotAuditedEntityReference(
					versionsReader, referencedEntity.getEntityClass(),
					referencedEntity.getEntityName(), primaryKey
			);
		}
	}

	/**
	 * Create Hibernate proxy or retrieve the complete object of referenced, not audited entity. According to
	 * {@link org.hibernate.envers.Audited#targetAuditMode()}} documentation, reference shall point to current
	 * (non-historical) version of an entity.
	 */
	private Object createNotAuditedEntityReference(
			AuditReaderImplementor versionsReader, Class<?> entityClass,
			String entityName, Serializable primaryKey) {
		final EntityPersister entityPersister = versionsReader.getSessionImplementor().getFactory().getMetamodel()
				.entityPersister( entityName );
		if ( entityPersister.hasProxy() ) {
			// If possible create a proxy. Returning complete object may affect performance.
			return versionsReader.getSession().load( entityClass, primaryKey );
		}
		else {
			// If proxy is not allowed (e.g. @Proxy(lazy=false)) construct the original object.
			return versionsReader.getSession().get( entityClass, primaryKey );
		}
	}
}
