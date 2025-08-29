/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.AbstractPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.service.ServiceRegistry;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Base class for property mappers that manage to-one relation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class AbstractToOneMapper extends AbstractPropertyMapper {
	private final ServiceRegistry serviceRegistry;
	private final PropertyData propertyData;

	protected AbstractToOneMapper(ServiceRegistry serviceRegistry, PropertyData propertyData) {
		this.serviceRegistry = serviceRegistry;
		this.propertyData = propertyData;
	}

	@Override
	public boolean mapToMapFromEntity(
			SharedSessionContractImplementor session,
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
	public Object mapToEntityFromMap(
			EnversService enversService,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		return nullSafeMapToEntityFromMap( enversService, data, primaryKey, versionsReader, revision );
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SharedSessionContractImplementor session,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl,
			Object id) {
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
		if ( isDynamicComponentMap() ) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> map = (Map<String, Object>) targetObject;
			map.put( propertyData.getBeanName(), value );
		}
		else {
			setValueOnObject( propertyData, targetObject, value, serviceRegistry );
		}
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

	public abstract Object nullSafeMapToEntityFromMap(
			EnversService enversService,
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
