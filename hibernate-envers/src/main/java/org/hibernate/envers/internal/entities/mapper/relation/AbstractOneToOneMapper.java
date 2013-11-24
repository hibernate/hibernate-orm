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
import java.util.Map;
import javax.persistence.NoResultException;

import org.hibernate.NonUniqueResultException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * Template class for property mappers that manage one-to-one relation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractOneToOneMapper extends AbstractToOneMapper {
	private final String entityName;
	private final String referencedEntityName;

	protected AbstractOneToOneMapper(String entityName, String referencedEntityName, PropertyData propertyData) {
		super( propertyData );
		this.entityName = entityName;
		this.referencedEntityName = referencedEntityName;
	}

	@Override
	public void nullSafeMapToEntityFromMap(
			AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
			AuditReaderImplementor versionsReader, Number revision) {
		final EntityInfo referencedEntity = getEntityInfo( verCfg, referencedEntityName );

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

		setPropertyValue( obj, value );
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
