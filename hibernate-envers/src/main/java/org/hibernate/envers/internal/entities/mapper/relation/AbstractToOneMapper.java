/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * Base class for property mappers that manage to-one relation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class AbstractToOneMapper implements PropertyMapper {
	private final ServiceRegistry serviceRegistry;
	private final PropertyData propertyData;

	protected AbstractToOneMapper(ServiceRegistry serviceRegistry, PropertyData propertyData) {
		this.serviceRegistry = serviceRegistry;
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
		final Setter setter = ReflectionTools.getSetter( targetObject.getClass(), propertyData, serviceRegistry );
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
	protected static class EntityInfo {
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

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		return propertyData != null && propertyData.isUsingModifiedFlag();
	}
}
