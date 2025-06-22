/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.util.Map;
import jakarta.persistence.NoResultException;

import org.hibernate.NonUniqueResultException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.service.ServiceRegistry;

/**
 * Template class for property mappers that manage one-to-one relation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractOneToOneMapper extends AbstractToOneMapper {
	private final String entityName;
	private final String referencedEntityName;

	protected AbstractOneToOneMapper(
			String entityName,
			String referencedEntityName,
			PropertyData propertyData,
			ServiceRegistry serviceRegistry) {
		super( serviceRegistry, propertyData );
		this.entityName = entityName;
		this.referencedEntityName = referencedEntityName;
	}

	@Override
	public void nullSafeMapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		Object value = nullSafeMapToEntityFromMap( enversService, data, primaryKey, versionsReader, revision );
		setPropertyValue( obj, value );
	}

	@Override
	public Object nullSafeMapToEntityFromMap(
			EnversService enversService,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		final EntityInfo referencedEntity = getEntityInfo( enversService, referencedEntityName );

		Object value;
		try {
			value = queryForReferencedEntity( versionsReader, referencedEntity, (Serializable) primaryKey, revision );
		}
		catch (NoResultException e) {
			value = null;
		}
		catch (NonUniqueResultException e) {
			throw new AuditException(
					"Many versions results for one-to-one relationship " + entityName +
							"." + getPropertyData().getBeanName() + ".", e
			);
		}
		return value;
	}

	/**
	 * @param versionsReader Audit reader.
	 * @param referencedEntity Referenced entity descriptor.
	 * @param primaryKey Referenced entity identifier.
	 * @param revision Revision number.
	 *
	 * @return Referenced object or proxy of one-to-one relation.
	 */
	protected abstract Object queryForReferencedEntity(
			AuditReaderImplementor versionsReader, EntityInfo referencedEntity,
			Serializable primaryKey, Number revision);

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		if ( getPropertyData().isUsingModifiedFlag() ) {
			data.put(
					getPropertyData().getModifiedFlagPropertyName(),
					collectionPropertyName.equals( getPropertyData().getName() )
			);
		}
	}
}
