/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.mapping.Component;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.CompositeTypeImplementor;


/**
 * An abstract identifier mapper implementation specific for composite identifiers.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class AbstractCompositeIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
	protected final CompositeTypeImplementor compositeType;
	protected Map<PropertyData, AbstractIdMapper> ids;

	protected AbstractCompositeIdMapper(Component component) {
		this( component.getServiceRegistry(), (CompositeTypeImplementor) component.getType() );
	}

	protected AbstractCompositeIdMapper(ServiceRegistry serviceRegistry, CompositeTypeImplementor compositeType) {
		super( serviceRegistry );
		this.compositeType = compositeType;
		ids = Tools.newLinkedHashMap();
	}

	@Override
	public void add(PropertyData propertyData) {
		add( propertyData, new SingleIdMapper( getServiceRegistry(), propertyData ) );
	}

	@Override
	public void add(PropertyData propertyData, AbstractIdMapper idMapper) {
		ids.put( propertyData, idMapper );
	}

	@Override
	public Object mapToIdFromMap(Map data) {
		if ( data == null ) {
			return null;
		}

		if ( !compositeType.isMutable() ) {
			return mapToImmutableIdFromMap( data );
		}

		final Object compositeId = instantiateCompositeId( null );

		if ( compositeType.isMutable() ) {
			for ( AbstractIdMapper mapper : ids.values() ) {
				if ( !mapper.mapToEntityFromMap( compositeId, data ) ) {
					return null;
				}
			}
		}

		return compositeId;
	}

	protected Object mapToImmutableIdFromMap(Map data) {
		final var propertyNames = compositeType.getPropertyNames();
		final var values = new Object[propertyNames.length];
		for ( int i = 0; i < propertyNames.length; i++ ) {
			values[i] = data.get( propertyNames[i] );
		}
		return instantiateCompositeId( values );
	}

	@Override
	public void mapToEntityFromEntity(Object objectTo, Object objectFrom) {
		// no-op; does nothing
	}

	protected Object instantiateCompositeId(Object[] values) {
		try {
			return compositeType.getMappingModelPart()
					.getEmbeddableTypeDescriptor()
					.getRepresentationStrategy()
					.getInstantiator()
					.instantiate( () -> values );
		}
		catch ( Exception e ) {
			throw new AuditException( e );
		}
	}
}
