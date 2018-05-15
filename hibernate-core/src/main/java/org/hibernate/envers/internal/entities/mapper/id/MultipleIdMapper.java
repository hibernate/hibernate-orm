/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class MultipleIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {
	public MultipleIdMapper(Class compositeIdClass, ServiceRegistry serviceRegistry) {
		super( compositeIdClass, serviceRegistry );
	}

	@Override
	public void add(PropertyData propertyData) {
		ids.put( propertyData, resolveIdMapper( propertyData ) );
	}

	@Override
	public void mapToMapFromId(Session session, Map<String, Object> data, Object obj) {
		if ( compositeIdClass.isInstance( obj ) ) {
			for ( Map.Entry<PropertyData, SingleIdMapper> entry : ids.entrySet() ) {
				final PropertyData propertyData = entry.getKey();
				final SingleIdMapper idMapper = entry.getValue();

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
		final MultipleIdMapper ret = new MultipleIdMapper( compositeIdClass, getServiceRegistry() );

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
		for ( SingleIdMapper mapper : ids.values() ) {
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
