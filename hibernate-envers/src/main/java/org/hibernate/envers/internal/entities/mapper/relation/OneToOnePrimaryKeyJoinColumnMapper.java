/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Property mapper for {@link javax.persistence.OneToOne} with {@link javax.persistence.PrimaryKeyJoinColumn} relation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OneToOnePrimaryKeyJoinColumnMapper extends AbstractOneToOneMapper {
	public OneToOnePrimaryKeyJoinColumnMapper(
			String entityName,
			String referencedEntityName,
			PropertyData propertyData) {
		super( entityName, referencedEntityName, propertyData );
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
		final EntityPersister entityPersister = versionsReader.getSessionImplementor().getFactory().getEntityPersister(
				entityName
		);
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
