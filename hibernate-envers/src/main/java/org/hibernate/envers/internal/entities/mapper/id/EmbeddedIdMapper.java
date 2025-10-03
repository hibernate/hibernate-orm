/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.mapping.Component;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * An identifier mapper implementation for {@link jakarta.persistence.EmbeddedId} mappings.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class EmbeddedIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {
	private PropertyData idPropertyData;

	public EmbeddedIdMapper(PropertyData propertyData, Component component) {
		super( component.getComponentClass(), component.getServiceRegistry() );
		this.idPropertyData = propertyData;
	}

	private EmbeddedIdMapper(PropertyData idPropertyData, Class<?> compositeIdClass, ServiceRegistry serviceRegistry) {
		super( compositeIdClass, serviceRegistry );
		this.idPropertyData = idPropertyData;
	}

	@Override
	public void mapToMapFromId(Map<String, Object> data, Object obj) {
		for ( IdMapper idMapper : ids.values() ) {
			idMapper.mapToMapFromEntity( data, obj );
		}
	}

	@Override
	public void mapToMapFromEntity(Map<String, Object> data, final Object obj) {
		if ( obj == null ) {
			return;
		}
		mapToMapFromId( data, getValueFromObject( idPropertyData, obj ) );
	}

	@Override
	public boolean mapToEntityFromMap(final Object obj, final Map data) {
		if ( data == null || obj == null ) {
			return false;
		}

		final Setter setter = ReflectionTools.getSetter( obj.getClass(), idPropertyData, getServiceRegistry() );
		try {
			if ( compositeIdClass.isRecord() ) {
				final var subObj = mapToRecordFromMap( data );
				if ( subObj == null ) {
					return false;
				}
				else {
					setter.set( obj, subObj );
					return true;
				}
			}

			final Object subObj = instantiateCompositeId();

			boolean ret = true;
			for ( IdMapper idMapper : ids.values() ) {
				ret &= idMapper.mapToEntityFromMap( subObj, data );
			}

			if ( ret ) {
				setter.set( obj, subObj );
			}

			return ret;
		}
		catch (Exception e) {
			throw new AuditException( e );
		}
	}

	@Override
	public IdMapper prefixMappedProperties(String prefix) {
		final EmbeddedIdMapper ret = new EmbeddedIdMapper( idPropertyData, compositeIdClass, getServiceRegistry() );

		for ( PropertyData propertyData : ids.keySet() ) {
			final String propertyName = propertyData.getName();
			ret.ids.put( propertyData, new SingleIdMapper( getServiceRegistry(), new PropertyData( prefix + propertyName, propertyData ) ) );
		}

		return ret;
	}

	@Override
	public Object mapToIdFromEntity(final Object data) {
		if ( data == null ) {
			return null;
		}

		return getValueFromObject( idPropertyData, data );
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
}
