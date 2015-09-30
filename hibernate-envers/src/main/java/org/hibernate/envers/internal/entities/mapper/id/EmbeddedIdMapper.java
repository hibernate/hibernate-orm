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

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmbeddedIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {
	private PropertyData idPropertyData;

	public EmbeddedIdMapper(PropertyData idPropertyData, Class compositeIdClass, ServiceRegistry serviceRegistry) {
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
	public void mapToMapFromEntity(Map<String, Object> data, Object obj) {
		if ( obj == null ) {
			return;
		}

		final Getter getter = ReflectionTools.getGetter( obj.getClass(), idPropertyData, getServiceRegistry() );
		mapToMapFromId( data, getter.get( obj ) );
	}

	@Override
	public boolean mapToEntityFromMap(Object obj, Map data) {
		if ( data == null || obj == null ) {
			return false;
		}

		final Getter getter = ReflectionTools.getGetter( obj.getClass(), idPropertyData, getServiceRegistry() );
		final Setter setter = ReflectionTools.getSetter( obj.getClass(), idPropertyData, getServiceRegistry() );

		try {
			final Object subObj = ReflectHelper.getDefaultConstructor( getter.getReturnType() ).newInstance();

			boolean ret = true;
			for ( IdMapper idMapper : ids.values() ) {
				ret &= idMapper.mapToEntityFromMap( subObj, data );
			}

			if ( ret ) {
				setter.set( obj, subObj, null );
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
	public Object mapToIdFromEntity(Object data) {
		if ( data == null ) {
			return null;
		}

		final Getter getter = ReflectionTools.getGetter( data.getClass(), idPropertyData, getServiceRegistry() );
		return getter.get( data );
	}

	@Override
	public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
		final Map<String, Object> data = new LinkedHashMap<String, Object>();
		mapToMapFromId( data, obj );

		final List<QueryParameterData> ret = new ArrayList<QueryParameterData>();

		for ( Map.Entry<String, Object> propertyData : data.entrySet() ) {
			ret.add( new QueryParameterData( propertyData.getKey(), propertyData.getValue() ) );
		}

		return ret;
	}
}
