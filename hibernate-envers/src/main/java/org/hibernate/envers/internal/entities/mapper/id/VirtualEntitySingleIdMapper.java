/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.OrmTools;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;


/**
 * An extension to the {@link SingleIdMapper} implementation that supports the use case of an {@code @IdClass}
 * mapping that contains an entity association where the {@code @IdClass} stores the primary key of the
 * associated entity rather than the entity object itself.
 * <p>
 * Internally this mapper is capable of transforming the primary key values into the associated entity object
 * and vice versa depending upon the operation.
 *
 * @author Chris Cranford
 */
public class VirtualEntitySingleIdMapper extends SingleIdMapper {

	private final PropertyData propertyData;
	private final String entityName;

	private IdMapper entityIdMapper;

	public VirtualEntitySingleIdMapper(ServiceRegistry serviceRegistry, PropertyData propertyData) {
		super( serviceRegistry, propertyData );
		this.propertyData = propertyData;
		this.entityName = resolveEntityName( this.propertyData );
	}

	@Override
	public void mapToMapFromId(SharedSessionContractImplementor session, Map<String, Object> data, Object obj) {
		final Object value = getValueFromObject( propertyData, obj );

		// Either loads the entity from the session's 1LC if it already exists or potentially creates a
		// proxy object to represent the entity by identifier so that we can reference it in the map.
		final Object entity = OrmTools.loadAuditEntity( this.entityName, value, session );
		data.put( propertyData.getName(), entity );
	}

	@Override
	public void mapToEntityFromEntity(Object objTo, Object objFrom) {
		if ( objTo == null || objFrom == null ) {
			return;
		}

		final Getter getter = ReflectionTools.getGetter(
				objFrom.getClass(),
				propertyData,
				getServiceRegistry()
		);

		final Setter setter = ReflectionTools.getSetter(
				objTo.getClass(),
				propertyData,
				getServiceRegistry()
		);

		// Get the value from the containing entity
		final Object value = getter.get( objFrom );
		if ( value == null ) {
			return;
		}

		if ( !value.getClass().equals( propertyData.getVirtualReturnClass() ) ) {
			setter.set( objTo, getAssociatedEntityIdMapper().mapToIdFromEntity( value ) );
		}
		else {
			// This means we're setting the object
			setter.set( objTo, value );
		}
	}

	@Override
	public boolean mapToEntityFromMap(Object obj, Map data) {
		if ( data == null || obj == null ) {
			return false;
		}

		final Object value = data.get( propertyData.getName() );
		if ( value == null ) {
			return false;
		}

		final Setter setter = ReflectionTools.getSetter(
				obj.getClass(),
				propertyData,
				getServiceRegistry()
		);
		final Class<?> paramClass = ReflectionTools.getType(
				obj.getClass(),
				propertyData,
				getServiceRegistry()
		);

		if ( paramClass != null && paramClass.equals( propertyData.getVirtualReturnClass() ) ) {
			setter.set( obj, getAssociatedEntityIdMapper().mapToIdFromEntity( value ) );
		}
		else {
			setter.set( obj, value );
		}

		return true;
	}

	@Override
	public void mapToMapFromEntity(Map<String, Object> data, Object obj) {
		if ( obj == null ) {
			data.put( propertyData.getName(), null );
		}
		else {
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( obj );
			if ( lazyInitializer != null ) {
				data.put( propertyData.getName(), lazyInitializer.getInternalIdentifier() );
			}
			else {
				final Object value = getValueFromObject( propertyData, obj );

				if ( propertyData.getVirtualReturnClass().isInstance( value ) ) {
					// The value is the primary key, need to map it via IdMapper
					getPrefixedAssociatedEntityIdMapper( propertyData ).mapToMapFromId( data, value );
				}
				else {
					data.put( propertyData.getName(), value );
				}
			}
		}
	}

	private IdMapper getAssociatedEntityIdMapper() {
		if ( entityIdMapper == null ) {
			entityIdMapper = resolveEntityIdMapper( getServiceRegistry(), entityName );
		}
		return entityIdMapper;
	}

	private IdMapper getPrefixedAssociatedEntityIdMapper(PropertyData propertyData) {
		return getAssociatedEntityIdMapper().prefixMappedProperties( propertyData.getName() + "." );
	}

	private static String resolveEntityName(PropertyData propertyData) {
		if ( EntityType.class.isInstance( propertyData.getType() ) ) {
			final EntityType entityType = (EntityType) propertyData.getType();
			return entityType.getAssociatedEntityName();
		}
		return null;
	}

	private static IdMapper resolveEntityIdMapper(ServiceRegistry serviceRegistry, String entityName) {
		final EntitiesConfigurations entitiesConfigurations = serviceRegistry.getService( EnversService.class )
				.getEntitiesConfigurations();

		final EntityConfiguration auditedEntityConfiguration = entitiesConfigurations.get( entityName );
		if ( auditedEntityConfiguration != null ) {
			return auditedEntityConfiguration.getIdMapper();
		}
		else {
			return entitiesConfigurations.getNotVersionEntityConfiguration( entityName ).getIdMapper();
		}
	}

}
