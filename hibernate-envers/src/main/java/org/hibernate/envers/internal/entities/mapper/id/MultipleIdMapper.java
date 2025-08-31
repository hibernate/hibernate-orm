/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.mapping.Component;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.service.ServiceRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of an identifier mapper for {@link jakarta.persistence.IdClass} or multiple
 * {@link jakarta.persistence.Id} identifier mappings.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class MultipleIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {

	private final boolean embedded;

	public MultipleIdMapper(Component component) {
		super( component.getComponentClass(), component.getServiceRegistry() );
		this.embedded = component.isEmbedded();
	}

	private MultipleIdMapper(boolean embedded, Class<?> compositeIdClass, ServiceRegistry serviceRegistry) {
		super( compositeIdClass, serviceRegistry );
		this.embedded = embedded;
	}

	@Override
	public void add(PropertyData propertyData) {
		ids.put( propertyData, resolveIdMapper( propertyData ) );
	}

	@Override
	public void mapToMapFromId(SharedSessionContractImplementor session, Map<String, Object> data, Object obj) {
		if ( compositeIdClass.isInstance( obj ) ) {
			if ( embedded ) {
				final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( obj );
				if ( lazyInitializer != null ) {
					obj = lazyInitializer.getInternalIdentifier();
				}
			}
			for ( Map.Entry<PropertyData, AbstractIdMapper> entry : ids.entrySet() ) {
				final PropertyData propertyData = entry.getKey();
				final AbstractIdMapper idMapper = entry.getValue();

				if ( propertyData.getVirtualReturnClass() == null ) {
					idMapper.mapToMapFromEntity( data, obj );
				}
				else {
					idMapper.mapToMapFromId( session, data, obj );
				}
			}
		}
		else {
			mapToMapFromId( data, obj );
		}
	}

	@Override
	public void mapToMapFromId(Map<String, Object> data, Object obj) {
		for ( IdMapper idMapper : ids.values() ) {
			idMapper.mapToMapFromEntity( data, obj );
		}
	}

	@Override
	public void mapToMapFromEntity(Map<String, Object> data, Object obj) {
		if ( embedded ) {
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( obj );
			if ( lazyInitializer != null ) {
				obj = lazyInitializer.getInternalIdentifier();
			}
		}
		for ( IdMapper idMapper : ids.values() ) {
			idMapper.mapToMapFromEntity( data, obj );
		}
	}

	@Override
	public boolean mapToEntityFromMap(Object obj, Map data) {
		boolean ret = true;
		for ( IdMapper idMapper : ids.values() ) {
			ret &= idMapper.mapToEntityFromMap( obj, data );
		}

		return ret;
	}

	@Override
	public IdMapper prefixMappedProperties(String prefix) {
		final MultipleIdMapper ret = new MultipleIdMapper( embedded, compositeIdClass, getServiceRegistry() );

		for ( PropertyData propertyData : ids.keySet() ) {
			final String propertyName = propertyData.getName();
			ret.ids.put( propertyData, resolveIdMapper( new PropertyData( prefix + propertyName, propertyData ) ) );
		}

		return ret;
	}

	@Override
	public Object mapToIdFromEntity(Object data) {
		if ( data == null ) {
			return null;
		}

		final Object compositeId = instantiateCompositeId();
		for ( AbstractIdMapper mapper : ids.values() ) {
			mapper.mapToEntityFromEntity( compositeId, data );
		}

		return compositeId;
	}

	@Override
	public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
		final Map<String, Object> data = new LinkedHashMap<>();
		mapToMapFromId( data, obj );

		final List<QueryParameterData> ret = new ArrayList<>();

		for ( Map.Entry<String, Object> propertyData : data.entrySet() ) {
			ret.add( new QueryParameterData( propertyData.getKey(), propertyData.getValue() ) );
		}

		return ret;
	}

	private SingleIdMapper resolveIdMapper(PropertyData propertyData) {
		if ( propertyData.getVirtualReturnClass() != null ) {
			return new VirtualEntitySingleIdMapper( getServiceRegistry(), propertyData );
		}
		return new SingleIdMapper( getServiceRegistry(), propertyData );
	}
}
