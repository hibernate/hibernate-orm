/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.service.ServiceRegistry;


/**
 * An implementation of an identifier mapper for a single basic attribute property.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class SingleIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
	private PropertyData propertyData;

	public SingleIdMapper(ServiceRegistry serviceRegistry) {
		super( serviceRegistry );
	}

	public SingleIdMapper(ServiceRegistry serviceRegistry, PropertyData propertyData) {
		this( serviceRegistry );
		this.propertyData = propertyData;
	}

	@Override
	public void add(PropertyData propertyData) {
		if ( this.propertyData != null ) {
			throw new AuditException( "Only one property can be added!" );
		}

		this.propertyData = propertyData;
	}

	@Override
	public void add(PropertyData propertyData, AbstractIdMapper idMapper) {
		throw new AuditException( "This method is not allowed for a single identifier mapper" );
	}

	@Override
	public boolean mapToEntityFromMap(final Object obj, Map data) {
		if ( data == null || obj == null ) {
			return false;
		}

		final Object value = data.get( propertyData.getName() );
		if ( value == null ) {
			return false;
		}

		setValueOnObject( propertyData, obj, value );
		return true;
	}

	@Override
	public Object mapToIdFromMap(Map data) {
		if ( data == null ) {
			return null;
		}

		return data.get( propertyData.getName() );
	}

	@Override
	public Object mapToIdFromEntity(final Object data) {
		if ( data == null ) {
			return null;
		}

		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( data );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}
		else {
			return getValueFromObject( propertyData, data );
		}
	}

	@Override
	public void mapToMapFromId(Map<String, Object> data, Object obj) {
		if ( data != null ) {
			data.put( propertyData.getName(), obj );
		}
	}

	@Override
	public void mapToMapFromEntity(Map<String, Object> data, final Object obj) {
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
				data.put( propertyData.getName(), value );
			}
		}
	}

	@Override
	public void mapToEntityFromEntity(final Object objTo, final Object objFrom) {
		if ( objTo == null || objFrom == null ) {
			return;
		}
		getAndSetValue(propertyData, objFrom, objTo );
	}

	@Override
	public IdMapper prefixMappedProperties(String prefix) {
		return new SingleIdMapper( getServiceRegistry(), new PropertyData( prefix + propertyData.getName(), propertyData ) );
	}

	@Override
	public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
		final List<QueryParameterData> ret = new ArrayList<>();

		ret.add( new QueryParameterData( propertyData.getName(), obj ) );

		return ret;
	}
}
