/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.service.ServiceRegistry;

/**
 * Property mapper for not owning side of {@link jakarta.persistence.OneToOne} relation.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OneToOneNotOwningMapper extends AbstractOneToOneMapper {
	private final String owningReferencePropertyName;

	public OneToOneNotOwningMapper(
			String notOwningEntityName,
			String owningEntityName,
			String owningReferencePropertyName,
			PropertyData propertyData,
			ServiceRegistry serviceRegistry) {
		super( notOwningEntityName, owningEntityName, propertyData, serviceRegistry );
		this.owningReferencePropertyName = owningReferencePropertyName;
	}

	@Override
	protected Object queryForReferencedEntity(
			AuditReaderImplementor versionsReader,
			EntityInfo referencedEntity,
			Serializable primaryKey,
			Number revision) {
		return versionsReader.createQuery().forEntitiesAtRevision(
				referencedEntity.getEntityClass(),
				referencedEntity.getEntityName(), revision
		)
				.add( AuditEntity.relatedId( owningReferencePropertyName ).eq( primaryKey ) )
				.getSingleResult();
	}
}
