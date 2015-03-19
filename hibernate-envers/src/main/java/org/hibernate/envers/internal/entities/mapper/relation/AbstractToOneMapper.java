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
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.Setter;

/**
 * Base class for property mappers that manage to-one relation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractToOneMapper implements PropertyMapper {
	private final PropertyData propertyData;

	protected AbstractToOneMapper(PropertyData propertyData) {
		this.propertyData = propertyData;
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		return false;
	}

	@Override
	public void mapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		if ( obj != null ) {
			nullSafeMapToEntityFromMap( enversService, obj, data, primaryKey, versionsReader, revision );
		}
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl,
			Serializable id) {
		return null;
	}

	/**
	 * @param enversService The EnversService
	 * @param entityName Entity name.
	 *
	 * @return Entity class, name and information whether it is audited or not.
	 */
	protected EntityInfo getEntityInfo(EnversService enversService, String entityName) {
		EntityConfiguration entCfg = enversService.getEntitiesConfigurations().get( entityName );
		boolean isRelationAudited = true;
		if ( entCfg == null ) {
			// a relation marked as RelationTargetAuditMode.NOT_AUDITED
			entCfg = enversService.getEntitiesConfigurations().getNotVersionEntityConfiguration( entityName );
			isRelationAudited = false;
		}
		final Class entityClass = ReflectionTools.loadClass( entCfg.getEntityClassName(), enversService.getClassLoaderService() );
		return new EntityInfo( entityClass, entityName, isRelationAudited );
	}

	protected void setPropertyValue(Object targetObject, Object value) {
		final Setter setter = ReflectionTools.getSetter( targetObject.getClass(), propertyData );
		setter.set( targetObject, value, null );
	}

	/**
	 * @return Bean property that represents the relation.
	 */
	protected PropertyData getPropertyData() {
		return propertyData;
	}

	/**
	 * Parameter {@code obj} is never {@code null}.
	 */
	public abstract void nullSafeMapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision);

	/**
	 * Simple descriptor of an entity.
	 */
	protected class EntityInfo {
		private final Class entityClass;
		private final String entityName;
		private final boolean audited;

		public EntityInfo(Class entityClass, String entityName, boolean audited) {
			this.entityClass = entityClass;
			this.entityName = entityName;
			this.audited = audited;
		}

		public Class getEntityClass() {
			return entityClass;
		}

		public String getEntityName() {
			return entityName;
		}

		public boolean isAudited() {
			return audited;
		}
	}
}
